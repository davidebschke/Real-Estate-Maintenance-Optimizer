package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.HistoryEntryResponse;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentConflictException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidActualEndException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating, reading, rescheduling and deleting appointments, including recurring series.
 */
@Service
@Transactional
@EnableConfigurationProperties(AppointmentSchedulingProperties.class)
public class AppointmentService {

    static final String SCOPE_SERIES = "series";
    static final int RECURRENCE_HORIZON_OCCURRENCES = 12;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository repository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;
    private final AppointmentSchedulingProperties schedulingProperties;

    public AppointmentService(
            AppointmentRepository repository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            MessageSource messageSource,
            AppointmentSchedulingProperties schedulingProperties) {
        this.repository = repository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.messageSource = messageSource;
        this.schedulingProperties = schedulingProperties;
    }

    /**
     * Returns every appointment of the given account, sorted by start time.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAll(String ownerId) {
        return repository.findAllByPropertyOwnerIdOrderByStartAsc(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the appointment with the given id of the given account.
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getById(String ownerId, String id) {
        return toResponse(loadOrThrow(ownerId, id));
    }

    /**
     * Creates a new appointment for one of the given account's properties, rejecting a schedule that overlaps with
     * or comes closer than the configured buffer to another of the account's own appointments, materializing a bounded horizon of future occurrences when
     * recurring and using up one of the account's appointment creations if it has a limit.
     */
    public AppointmentResponse create(String ownerId, CreateAppointmentRequest request) {
        if (request.recurring() && (request.recurrenceIntervalMonths() == null || request.recurrenceIntervalMonths() <= 0)) {
            throw new InvalidRecurrenceException("recurrenceIntervalRequired");
        }

        LocalDateTime firstStart = request.start().truncatedTo(ChronoUnit.MICROS);
        Property property = propertyRepository.findByIdAndOwnerId(request.propertyId(), ownerId)
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyId()));

        User owner = loadOwner(ownerId);
        requireFreeSchedule(owner, null, firstStart, request.durationMinutes());
        owner.consumeAppointmentCreation();
        String seriesId = request.recurring() ? UUID.randomUUID().toString() : null;
        List<String> materials = normalizeMaterials(request.materials());
        String description = normalizeDescription(request.description());
        int occurrenceCount = request.recurring() ? RECURRENCE_HORIZON_OCCURRENCES : 1;

        Appointment firstOccurrence = null;
        for (int occurrenceIndex = 0; occurrenceIndex < occurrenceCount; occurrenceIndex++) {
            LocalDateTime occurrenceStart = request.recurring()
                    ? firstStart.plusMonths((long) request.recurrenceIntervalMonths() * occurrenceIndex)
                    : firstStart;
            LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(request.durationMinutes());

            Appointment occurrence = new Appointment(
                    UUID.randomUUID().toString(),
                    seriesId,
                    request.title(),
                    property,
                    description,
                    occurrenceStart,
                    occurrenceEnd,
                    request.locked(),
                    request.recurring(),
                    request.recurrenceIntervalMonths(),
                    materials,
                    List.of(new HistoryEntry(currentInstant(), HistoryEventType.CREATED, List.of())),
                    null);

            Appointment savedOccurrence = repository.save(occurrence);
            if (occurrenceIndex == 0) {
                firstOccurrence = savedOccurrence;
            }
        }

        return toResponse(firstOccurrence);
    }

    /**
     * Reschedules an unlocked appointment to a new start and duration, rejecting locked appointments and a changed
     * schedule that overlaps with or comes closer than the configured buffer to another of the account's appointments.
     */
    public AppointmentResponse move(String ownerId, String id, MoveAppointmentRequest request) {
        return reschedule(ownerId, id, request, HistoryEventType.MOVED);
    }

    /**
     * Reschedules an unlocked appointment to the slot of an accepted AI optimization proposal like {@link #move}, recording the move as an optimization and, for a recurring occurrence, remembering the start it was originally planned for.
     */
    public AppointmentResponse applyOptimizedSchedule(String ownerId, String id, MoveAppointmentRequest request) {
        Appointment appointment = loadOrThrow(ownerId, id);
        if (appointment.recurring()) {
            appointment.anchorRecurrenceStart();
        }
        return reschedule(ownerId, id, request, HistoryEventType.OPTIMIZED);
    }

    private AppointmentResponse reschedule(
            String ownerId, String id, MoveAppointmentRequest request, HistoryEventType historyEventType) {
        Appointment appointment = loadOrThrow(ownerId, id);
        if (appointment.locked()) {
            throw new AppointmentLockedException(id);
        }

        LocalDateTime newStart = request.start().truncatedTo(ChronoUnit.MICROS);
        LocalDateTime newEnd = newStart.plusMinutes(request.durationMinutes());
        if (!newStart.equals(appointment.start()) || !newEnd.equals(appointment.end())) {
            requireFreeSchedule(loadOwner(ownerId), id, newStart, request.durationMinutes());
        }
        HistoryEntry moveEntry = new HistoryEntry(
                currentInstant(),
                historyEventType,
                List.of(formatRange(appointment.start(), appointment.end()), formatRange(newStart, newEnd)));

        appointment.reschedule(newStart, newEnd, moveEntry);
        return toResponse(appointment);
    }

    /**
     * Updates an appointment's title, property, schedule, locked/recurring state, description and materials, rejecting a schedule change on a locked appointment; turning "recurring" on for a not-yet-recurring appointment additionally materializes the same horizon of future occurrences a newly created recurring appointment would get.
     */
    public AppointmentResponse update(String ownerId, String id, CreateAppointmentRequest request) {
        if (request.recurring() && (request.recurrenceIntervalMonths() == null || request.recurrenceIntervalMonths() <= 0)) {
            throw new InvalidRecurrenceException("recurrenceIntervalRequired");
        }

        Appointment appointment = loadOrThrow(ownerId, id);
        Property property = propertyRepository.findByIdAndOwnerId(request.propertyId(), ownerId)
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyId()));

        LocalDateTime newStart = request.start().truncatedTo(ChronoUnit.MICROS);
        LocalDateTime newEnd = newStart.plusMinutes(request.durationMinutes());
        boolean scheduleChanged = !newStart.equals(appointment.start()) || !newEnd.equals(appointment.end());
        if (scheduleChanged && appointment.locked()) {
            throw new AppointmentLockedException(id);
        }

        boolean startsNewSeries = request.recurring() && !appointment.recurring();
        String seriesId = request.recurring() ? (startsNewSeries ? UUID.randomUUID().toString() : appointment.seriesId()) : null;
        List<String> materials = normalizeMaterials(request.materials());
        String description = normalizeDescription(request.description());
        HistoryEntry editEntry = new HistoryEntry(currentInstant(), HistoryEventType.EDITED, List.of());

        appointment.updateDetails(
                request.title(),
                property,
                description,
                newStart,
                newEnd,
                request.locked(),
                request.recurring(),
                request.recurrenceIntervalMonths(),
                seriesId,
                materials,
                editEntry);

        if (startsNewSeries) {
            materializeFutureOccurrences(
                    seriesId, request.title(), property, description, newStart, request.durationMinutes(),
                    request.locked(), request.recurrenceIntervalMonths(), materials);
        }

        return toResponse(appointment);
    }

    /**
     * Creates the occurrences following the given first occurrence's start, sharing its seriesId, mirroring the horizon a newly created recurring appointment materializes.
     */
    private void materializeFutureOccurrences(
            String seriesId,
            String title,
            Property property,
            String description,
            LocalDateTime firstStart,
            int durationMinutes,
            boolean locked,
            int recurrenceIntervalMonths,
            List<String> materials) {
        for (int occurrenceIndex = 1; occurrenceIndex < RECURRENCE_HORIZON_OCCURRENCES; occurrenceIndex++) {
            LocalDateTime occurrenceStart = firstStart.plusMonths((long) recurrenceIntervalMonths * occurrenceIndex);
            LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(durationMinutes);

            repository.save(new Appointment(
                    UUID.randomUUID().toString(),
                    seriesId,
                    title,
                    property,
                    description,
                    occurrenceStart,
                    occurrenceEnd,
                    locked,
                    true,
                    recurrenceIntervalMonths,
                    materials,
                    List.of(new HistoryEntry(currentInstant(), HistoryEventType.CREATED, List.of())),
                    null));
        }
    }

    /**
     * Marks an appointment as completed with the given actual end, falling back to the current time when {@code actualEnd} is null, rejecting an actual end that lies before the appointment's planned start.
     */
    public AppointmentResponse complete(String ownerId, String id, LocalDateTime actualEnd) {
        Appointment appointment = loadOrThrow(ownerId, id);
        LocalDateTime resolvedActualEnd =
                (actualEnd != null ? actualEnd : LocalDateTime.now()).truncatedTo(ChronoUnit.MICROS);
        if (resolvedActualEnd.isBefore(appointment.start())) {
            throw new InvalidActualEndException(id);
        }
        HistoryEntry completedEntry = new HistoryEntry(
                currentInstant(), HistoryEventType.COMPLETED, List.of(TIME_FORMAT.format(resolvedActualEnd)));

        appointment.changeActualEnd(resolvedActualEnd, completedEntry);
        return toResponse(appointment);
    }

    /**
     * Reverts a completed appointment back to its not-yet-completed state.
     */
    public AppointmentResponse reopen(String ownerId, String id) {
        Appointment appointment = loadOrThrow(ownerId, id);
        HistoryEntry reopenedEntry = new HistoryEntry(currentInstant(), HistoryEventType.REOPENED, List.of());

        appointment.changeActualEnd(null, reopenedEntry);
        return toResponse(appointment);
    }

    /**
     * Deletes a single appointment, or every not-yet-past occurrence of its recurring series when {@code scope} is "series".
     */
    public void delete(String ownerId, String id, String scope) {
        Appointment appointment = loadOrThrow(ownerId, id);
        if (!SCOPE_SERIES.equalsIgnoreCase(scope)) {
            repository.delete(appointment);
            return;
        }

        if (!appointment.recurring()) {
            throw new InvalidRecurrenceException("seriesScopeNotRecurring");
        }

        repository.findBySeriesIdAndPropertyOwnerId(appointment.seriesId(), ownerId).stream()
                .filter(candidate -> !candidate.start().isBefore(appointment.start()))
                .forEach(repository::delete);
    }

    /**
     * Throws an {@link AppointmentConflictException} carrying the next free slot of the given duration if the given
     * schedule overlaps with or comes closer than the account's buffer (its own setting or else the configured default) to any of its appointments other
     * than {@code ignoredAppointmentId} (null to ignore none), holding the account's transaction lock so two requests
     * racing for the same slot cannot both pass this check before either has persisted its change.
     */
    private void requireFreeSchedule(
            User owner, String ignoredAppointmentId, LocalDateTime start, int durationMinutes) {
        String ownerId = owner.id();
        userRepository.acquireTransactionLock(ownerId.hashCode());
        int bufferMinutes = schedulingProperties.bufferMinutesFor(owner.appointmentBufferMinutes());
        List<Appointment> otherAppointments = repository.findAllByPropertyOwnerIdOrderByStartAsc(ownerId).stream()
                .filter(existing -> !existing.id().equals(ignoredAppointmentId))
                .toList();
        LocalDateTime end = start.plusMinutes(durationMinutes);
        if (otherAppointments.stream().noneMatch(existing -> isTooCloseTo(existing, start, end, bufferMinutes))) {
            return;
        }
        LocalDateTime suggestedStart = findNextAvailableStart(otherAppointments, start, durationMinutes, bufferMinutes);
        throw new AppointmentConflictException(suggestedStart, suggestedStart.plusMinutes(durationMinutes));
    }

    /**
     * Returns the earliest start at or after the given start at which the given duration keeps the given buffer
     * to every given appointment's occupied range, never suggesting a Sunday since the company schedules no
     * appointments then.
     */
    private LocalDateTime findNextAvailableStart(
            List<Appointment> existingAppointments, LocalDateTime requestedStart, int durationMinutes, int bufferMinutes) {
        LocalDateTime candidateStart = requestedStart;
        boolean candidateChanged;
        do {
            candidateChanged = false;
            if (candidateStart.getDayOfWeek() == DayOfWeek.SUNDAY) {
                candidateStart = candidateStart.plusDays(1);
                candidateChanged = true;
                continue;
            }
            LocalDateTime candidateEnd = candidateStart.plusMinutes(durationMinutes);
            for (Appointment existing : existingAppointments) {
                if (isTooCloseTo(existing, candidateStart, candidateEnd, bufferMinutes)) {
                    candidateStart = occupiedEnd(existing).plusMinutes(bufferMinutes);
                    candidateChanged = true;
                    break;
                }
            }
        } while (candidateChanged);
        return candidateStart;
    }

    /**
     * Returns whether the given range overlaps with the existing appointment's occupied range widened by the
     * given buffer on both sides.
     */
    private boolean isTooCloseTo(Appointment existing, LocalDateTime start, LocalDateTime end, int bufferMinutes) {
        return rangesOverlap(
                existing.start().minusMinutes(bufferMinutes), occupiedEnd(existing).plusMinutes(bufferMinutes), start, end);
    }

    private User loadOwner(String ownerId) {
        return userRepository.findById(ownerId).orElseThrow(() -> new AccountNotFoundException(ownerId));
    }

    /**
     * Returns an appointment's occupied end: its actual end once completed (which may lie before or after its
     * originally planned end), its planned end otherwise.
     */
    private static LocalDateTime occupiedEnd(Appointment appointment) {
        return appointment.completed() ? appointment.actualEnd() : appointment.end();
    }

    /**
     * Returns whether the half-open ranges {@code [start1, end1)} and {@code [start2, end2)} overlap.
     */
    private static boolean rangesOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private static List<String> normalizeMaterials(List<String> materials) {
        return materials != null ? List.copyOf(materials) : List.of();
    }

    private static String normalizeDescription(String description) {
        return description != null ? description : "";
    }

    private Appointment loadOrThrow(String ownerId, String id) {
        return repository.findByIdAndPropertyOwnerId(id, ownerId).orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        Locale locale = LocaleContextHolder.getLocale();
        List<HistoryEntryResponse> history = appointment.history().stream()
                .map(entry -> new HistoryEntryResponse(entry.timestamp(), localize(entry, locale)))
                .toList();

        return new AppointmentResponse(
                appointment.id(),
                appointment.seriesId(),
                appointment.title(),
                appointment.property().id(),
                appointment.property().name(),
                appointment.property().address(),
                appointment.description(),
                appointment.start(),
                appointment.end(),
                appointment.locked(),
                appointment.recurring(),
                appointment.recurrenceIntervalMonths(),
                appointment.materials(),
                history,
                appointment.actualEnd(),
                appointment.completed());
    }

    private String localize(HistoryEntry entry, Locale locale) {
        String messageKey = switch (entry.type()) {
            case CREATED -> "appointment.history.created";
            case MOVED -> "appointment.history.moved";
            case COMPLETED -> "appointment.history.completed";
            case REOPENED -> "appointment.history.reopened";
            case EDITED -> "appointment.history.edited";
            case OPTIMIZED -> "appointment.history.optimized";
        };
        return messageSource.getMessage(messageKey, entry.messageArgs().toArray(), locale);
    }

    private Instant currentInstant() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private String formatRange(LocalDateTime start, LocalDateTime end) {
        return DATE_TIME_FORMAT.format(start) + "–" + TIME_FORMAT.format(end);
    }
}
