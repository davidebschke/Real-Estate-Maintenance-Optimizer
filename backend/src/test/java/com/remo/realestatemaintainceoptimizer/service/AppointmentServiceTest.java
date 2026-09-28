package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Verifies appointment creation (including recurrence), rescheduling, and single-vs-series deletion against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AppointmentServiceTest {

    @Autowired
    private AppointmentService service;

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private PropertyRepository propertyRepository;

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAllInBatch();
        propertyRepository.save(new Property(
                "property-1", "Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building"));
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
    void createdAppointmentTakesNameAndAddressFromItsProperty() {
        AppointmentResponse response = service.create(createRequest(false, null));

        assertThat(response.propertyId()).isEqualTo("property-1");
        assertThat(response.propertyName()).isEqualTo("Wohnanlage Sonnenhof");
        assertThat(response.propertyAddress()).isEqualTo("Aachener Str. 512, 50933 Köln-Braunsenfeld");
        assertThat(response.materials()).containsExactly("Kehrmaschine", "Müllsäcke 10x");
    }

    @Test
    void creatingAnAppointmentForAnUnknownPropertyIsRejected() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Kellerreinigung Q3", "unknown-property", "", LocalDateTime.of(2026, 8, 11, 13, 0),
                120, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(PropertyNotFoundException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void readingAnAppointmentBackReturnsTheSameDataAsItsCreation() {
        AppointmentResponse created = service.create(createRequest(false, null));

        assertThat(service.getById(created.id())).isEqualTo(created);
    }

    @Test
    void subMicrosecondStartTimesAreTruncatedToTheStoredPrecisionOnCreateAndMove() {
        LocalDateTime startWithNanos = LocalDateTime.of(2026, 8, 11, 13, 0, 0, 123_456_789);
        AppointmentResponse created = service.create(new CreateAppointmentRequest(
                "Kellerreinigung Q3", "property-1", "", startWithNanos, 120, false, false, null, List.of()));

        AppointmentResponse moved = service.move(created.id(), new MoveAppointmentRequest(startWithNanos.plusDays(1), 60));

        assertThat(created.start()).isEqualTo(LocalDateTime.of(2026, 8, 11, 13, 0, 0, 123_456_000));
        assertThat(moved.start()).isEqualTo(LocalDateTime.of(2026, 8, 12, 13, 0, 0, 123_456_000));
        assertThat(service.getById(created.id())).isEqualTo(moved);
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
        assertThat(service.getById(created.id()).start()).isEqualTo(newStart);
    }

    @Test
    void movingALockedAppointmentIsRejected() {
        CreateAppointmentRequest lockedRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin",
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
        assertThat(service.getById(created.id()).completed()).isFalse();
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
                "Was ist zu tun?",
                LocalDateTime.of(2026, 8, 11, 13, 0),
                120,
                false,
                recurring,
                recurrenceIntervalMonths,
                List.of("Kehrmaschine", "Müllsäcke 10x"));
    }
}
