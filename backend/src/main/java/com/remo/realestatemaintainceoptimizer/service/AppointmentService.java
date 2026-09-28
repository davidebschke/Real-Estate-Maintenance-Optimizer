package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.HistoryEntryResponse;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for creating, reading, rescheduling and deleting appointments, including recurring series.
 */
@Service
@Transactional
public class AppointmentService {

    static final String SCOPE_SERIES = "series";
    static final int RECURRENCE_HORIZON_OCCURRENCES = 12;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentRepository repository;
    private final PropertyRepository propertyRepository;
    private final MessageSource messageSource;

    public AppointmentService(
            AppointmentRepository repository, PropertyRepository propertyRepository, MessageSource messageSource) {
        this.repository = repository;
        this.propertyRepository = propertyRepository;
        this.messageSource = messageSource;
    }

    /**
     * Returns every appointment, sorted by start time.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAll() {
        return repository.findAllByOrderByStartAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the appointment with the given id.
     */
    @Transactional(readOnly = true)
    public AppointmentResponse getById(String id) {
        return toResponse(loadOrThrow(id));
    }

    /**
     * Creates a new appointment for an existing property, materializing a bounded horizon of future occurrences when recurring.
     */
    public AppointmentResponse create(CreateAppointmentRequest request) {
        if (request.recurring() && (request.recurrenceIntervalMonths() == null || request.recurrenceIntervalMonths() <= 0)) {
            throw new InvalidRecurrenceException("recurrenceIntervalRequired");
        }

        LocalDateTime firstStart = request.start().truncatedTo(ChronoUnit.MICROS);
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new PropertyNotFoundException(request.propertyId()));
        String seriesId = request.recurring() ? UUID.randomUUID().toString() : null;
        List<String> materials = request.materials() != null ? List.copyOf(request.materials()) : List.of();
        String description = request.description() != null ? request.description() : "";
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
     * Reschedules an unlocked appointment to a new start and duration, rejecting locked appointments.
     */
    public AppointmentResponse move(String id, MoveAppointmentRequest request) {
        Appointment appointment = loadOrThrow(id);
        if (appointment.locked()) {
            throw new AppointmentLockedException(id);
        }

        LocalDateTime newStart = request.start().truncatedTo(ChronoUnit.MICROS);
        LocalDateTime newEnd = newStart.plusMinutes(request.durationMinutes());
        HistoryEntry moveEntry = new HistoryEntry(
                currentInstant(),
                HistoryEventType.MOVED,
                List.of(formatRange(appointment.start(), appointment.end()), formatRange(newStart, newEnd)));

        appointment.reschedule(newStart, newEnd, moveEntry);
        return toResponse(appointment);
    }

    /**
     * Marks an appointment as completed with the given actual end, falling back to the current time when {@code actualEnd} is null.
     */
    public AppointmentResponse complete(String id, LocalDateTime actualEnd) {
        Appointment appointment = loadOrThrow(id);
        LocalDateTime resolvedActualEnd =
                (actualEnd != null ? actualEnd : LocalDateTime.now()).truncatedTo(ChronoUnit.MICROS);
        HistoryEntry completedEntry = new HistoryEntry(
                currentInstant(), HistoryEventType.COMPLETED, List.of(TIME_FORMAT.format(resolvedActualEnd)));

        appointment.changeActualEnd(resolvedActualEnd, completedEntry);
        return toResponse(appointment);
    }

    /**
     * Reverts a completed appointment back to its not-yet-completed state.
     */
    public AppointmentResponse reopen(String id) {
        Appointment appointment = loadOrThrow(id);
        HistoryEntry reopenedEntry = new HistoryEntry(currentInstant(), HistoryEventType.REOPENED, List.of());

        appointment.changeActualEnd(null, reopenedEntry);
        return toResponse(appointment);
    }

    /**
     * Deletes a single appointment, or every not-yet-past occurrence of its recurring series when {@code scope} is "series".
     */
    public void delete(String id, String scope) {
        Appointment appointment = loadOrThrow(id);
        if (!SCOPE_SERIES.equalsIgnoreCase(scope)) {
            repository.delete(appointment);
            return;
        }

        if (!appointment.recurring()) {
            throw new InvalidRecurrenceException("seriesScopeNotRecurring");
        }

        repository.findBySeriesId(appointment.seriesId()).stream()
                .filter(candidate -> !candidate.start().isBefore(appointment.start()))
                .forEach(repository::delete);
    }

    private Appointment loadOrThrow(String id) {
        return repository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
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
