package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.HistoryEntryResponse;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentFileRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Business logic for creating, reading, rescheduling and deleting appointments, including recurring series.
 */
@Service
public class AppointmentService {

    static final String SCOPE_SERIES = "series";
    static final int RECURRENCE_HORIZON_OCCURRENCES = 12;

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AppointmentFileRepository repository;
    private final MessageSource messageSource;

    public AppointmentService(AppointmentFileRepository repository, MessageSource messageSource) {
        this.repository = repository;
        this.messageSource = messageSource;
    }

    /**
     * Returns every appointment, sorted by start time.
     */
    public List<AppointmentResponse> listAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Appointment::start))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the appointment with the given id.
     */
    public AppointmentResponse getById(String id) {
        return toResponse(loadOrThrow(id));
    }

    /**
     * Creates a new appointment, materializing a bounded horizon of future occurrences when recurring.
     */
    public AppointmentResponse create(CreateAppointmentRequest request) {
        if (request.recurring() && (request.recurrenceIntervalMonths() == null || request.recurrenceIntervalMonths() <= 0)) {
            throw new InvalidRecurrenceException("recurrenceIntervalRequired");
        }

        String seriesId = request.recurring() ? UUID.randomUUID().toString() : null;
        List<String> materials = request.materials() != null ? List.copyOf(request.materials()) : List.of();
        String description = request.description() != null ? request.description() : "";
        int occurrenceCount = request.recurring() ? RECURRENCE_HORIZON_OCCURRENCES : 1;

        Appointment firstOccurrence = null;
        for (int occurrenceIndex = 0; occurrenceIndex < occurrenceCount; occurrenceIndex++) {
            LocalDateTime occurrenceStart = request.recurring()
                    ? request.start().plusMonths((long) request.recurrenceIntervalMonths() * occurrenceIndex)
                    : request.start();
            LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(request.durationMinutes());

            Appointment occurrence = new Appointment(
                    UUID.randomUUID().toString(),
                    seriesId,
                    request.title(),
                    request.propertyId(),
                    request.propertyName(),
                    request.propertyAddress(),
                    description,
                    occurrenceStart,
                    occurrenceEnd,
                    request.locked(),
                    request.recurring(),
                    request.recurrenceIntervalMonths(),
                    materials,
                    List.of(new HistoryEntry(Instant.now(), HistoryEventType.CREATED, List.of())));

            repository.save(occurrence);
            if (occurrenceIndex == 0) {
                firstOccurrence = occurrence;
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

        LocalDateTime newStart = request.start();
        LocalDateTime newEnd = newStart.plusMinutes(request.durationMinutes());
        HistoryEntry moveEntry = new HistoryEntry(
                Instant.now(),
                HistoryEventType.MOVED,
                List.of(formatRange(appointment.start(), appointment.end()), formatRange(newStart, newEnd)));

        Appointment moved = appointment.withSchedule(newStart, newEnd, moveEntry);
        repository.save(moved);
        return toResponse(moved);
    }

    /**
     * Deletes a single appointment, or every not-yet-past occurrence of its recurring series when {@code scope} is "series".
     */
    public void delete(String id, String scope) {
        Appointment appointment = loadOrThrow(id);
        if (!SCOPE_SERIES.equalsIgnoreCase(scope)) {
            repository.deleteById(id);
            return;
        }

        if (!appointment.recurring()) {
            throw new InvalidRecurrenceException("seriesScopeNotRecurring");
        }

        repository.findBySeriesId(appointment.seriesId()).stream()
                .filter(candidate -> !candidate.start().isBefore(appointment.start()))
                .forEach(candidate -> repository.deleteById(candidate.id()));
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
                appointment.propertyId(),
                appointment.propertyName(),
                appointment.propertyAddress(),
                appointment.description(),
                appointment.start(),
                appointment.end(),
                appointment.locked(),
                appointment.recurring(),
                appointment.recurrenceIntervalMonths(),
                appointment.materials(),
                history);
    }

    private String localize(HistoryEntry entry, Locale locale) {
        String messageKey = switch (entry.type()) {
            case CREATED -> "appointment.history.created";
            case MOVED -> "appointment.history.moved";
        };
        return messageSource.getMessage(messageKey, entry.messageArgs().toArray(), locale);
    }

    private String formatRange(LocalDateTime start, LocalDateTime end) {
        return DATE_TIME_FORMAT.format(start) + "–" + TIME_FORMAT.format(end);
    }
}
