package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.config.StorageProperties;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentFileRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies appointment creation (including recurrence), rescheduling, and single-vs-series deletion.
 */
class AppointmentServiceTest {

    @TempDir
    private Path storageDirectory;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        AppointmentFileRepository repository =
                new AppointmentFileRepository(new StorageProperties(storageDirectory.toString()), new ObjectMapper());
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("locales/messages");
        messageSource.setDefaultEncoding("UTF-8");
        service = new AppointmentService(repository, messageSource);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void createsASingleNonRecurringAppointment() {
        AppointmentResponse response = service.create(createRequest(false, null));

        assertThat(response.recurring()).isFalse();
        assertThat(response.seriesId()).isNull();
        assertThat(response.history()).hasSize(1);
        assertThat(service.listAll()).hasSize(1);
    }

    @Test
    void recurringAppointmentRequiresARecurrenceInterval() {
        assertThatThrownBy(() -> service.create(createRequest(true, null)))
                .isInstanceOf(InvalidRecurrenceException.class);
    }

    @Test
    void recurringAppointmentMaterializesTwelveOccurrencesSharingOneSeriesId() {
        AppointmentResponse firstOccurrence = service.create(createRequest(true, 3));

        List<AppointmentResponse> all = service.listAll();

        assertThat(all).hasSize(AppointmentService.RECURRENCE_HORIZON_OCCURRENCES);
        assertThat(all).allMatch(response -> firstOccurrence.seriesId().equals(response.seriesId()));
        assertThat(all.get(1).start()).isEqualTo(firstOccurrence.start().plusMonths(3));
    }

    @Test
    void movingAnUnlockedAppointmentUpdatesItsScheduleAndHistory() {
        AppointmentResponse created = service.create(createRequest(false, null));
        LocalDateTime newStart = created.start().plusDays(1);

        AppointmentResponse moved = service.move(created.id(), new MoveAppointmentRequest(newStart, 120));

        assertThat(moved.start()).isEqualTo(newStart);
        assertThat(moved.history()).hasSize(2);
    }

    @Test
    void movingALockedAppointmentIsRejected() {
        CreateAppointmentRequest lockedRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "Pflichttermin",
                LocalDateTime.of(2026, 8, 11, 9, 0), 60, true, false, null, List.of());
        AppointmentResponse created = service.create(lockedRequest);

        assertThatThrownBy(() -> service.move(created.id(), new MoveAppointmentRequest(created.start().plusDays(1), 60)))
                .isInstanceOf(AppointmentLockedException.class);
    }

    @Test
    void completingAnAppointmentWithoutAnExplicitActualEndFallsBackToNow() {
        AppointmentResponse created = service.create(createRequest(false, null));

        AppointmentResponse completed = service.complete(created.id(), null);

        assertThat(completed.completed()).isTrue();
        assertThat(completed.actualEnd()).isNotNull();
        assertThat(completed.history()).hasSize(2);
    }

    @Test
    void completingAnAppointmentWithAnExplicitActualEndUsesIt() {
        AppointmentResponse created = service.create(createRequest(false, null));
        LocalDateTime explicitActualEnd = LocalDateTime.of(2026, 8, 10, 16, 30);

        AppointmentResponse completed = service.complete(created.id(), explicitActualEnd);

        assertThat(completed.actualEnd()).isEqualTo(explicitActualEnd);
    }

    @Test
    void reopeningACompletedAppointmentClearsActualEnd() {
        AppointmentResponse created = service.create(createRequest(false, null));
        service.complete(created.id(), null);

        AppointmentResponse reopened = service.reopen(created.id());

        assertThat(reopened.completed()).isFalse();
        assertThat(reopened.actualEnd()).isNull();
        assertThat(reopened.history()).hasSize(3);
    }

    @Test
    void deletingWithSingleScopeRemovesOnlyThatOccurrence() {
        AppointmentResponse firstOccurrence = service.create(createRequest(true, 3));

        service.delete(firstOccurrence.id(), "single");

        assertThat(service.listAll()).hasSize(AppointmentService.RECURRENCE_HORIZON_OCCURRENCES - 1);
    }

    @Test
    void deletingWithSeriesScopeRemovesThisAndAllFollowingOccurrences() {
        AppointmentResponse firstOccurrence = service.create(createRequest(true, 3));
        AppointmentResponse secondOccurrence = service.listAll().get(1);

        service.delete(secondOccurrence.id(), "series");

        assertThat(service.listAll()).containsExactly(firstOccurrence);
    }

    @Test
    void deletingWithSeriesScopeOnANonRecurringAppointmentIsRejected() {
        AppointmentResponse created = service.create(createRequest(false, null));

        assertThatThrownBy(() -> service.delete(created.id(), "series")).isInstanceOf(InvalidRecurrenceException.class);
    }

    private CreateAppointmentRequest createRequest(boolean recurring, Integer recurrenceIntervalMonths) {
        return new CreateAppointmentRequest(
                "Kellerreinigung Q3",
                "property-1",
                "Wohnanlage Sonnenhof",
                "Aachener Str. 512, 50933 Köln-Braunsenfeld",
                "Was ist zu tun?",
                LocalDateTime.of(2026, 8, 11, 13, 0),
                120,
                false,
                recurring,
                recurrenceIntervalMonths,
                List.of("Kehrmaschine", "Müllsäcke 10x"));
    }
}
