package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentConflictException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentLockedException;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidActualEndException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidRecurrenceException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Verifies appointment creation (including recurrence), rescheduling, single-vs-series deletion, account isolation and demo creation limits against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AppointmentServiceTest {

    private static final int BUFFER_MINUTES = 5;

    @Autowired
    private AppointmentService service;

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    private User otherOwner;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        otherOwner = TestAccounts.saveRegularAccount(userRepository);
        propertyRepository.save(new Property(
                "property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building"));
        propertyRepository.save(new Property(
                "foreign-property", otherOwner.id(), "Fremdes Objekt", "Fremdstr. 1", "pi-building"));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void creatingAnAppointmentForAnotherAccountsPropertyIsRejected() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Kellerreinigung Q3", "foreign-property", "", LocalDateTime.of(2026, 8, 11, 13, 0),
                120, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), request)).isInstanceOf(PropertyNotFoundException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void creatingAnAppointmentOverlappingAnExistingOneOfTheSameAccountIsBlocked() {
        service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest overlapping = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", LocalDateTime.of(2026, 8, 11, 14, 0), 120, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), overlapping)).isInstanceOf(AppointmentConflictException.class);
        assertThat(service.listAll(owner.id())).hasSize(1);
    }

    @Test
    void theConflictExceptionSuggestsTheNextFreeSlotAfterEveryOverlappingAppointment() {
        service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest secondExisting = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "", LocalDateTime.of(2026, 8, 11, 15, 15), 60, false, false, null, List.of());
        service.create(owner.id(), secondExisting);
        CreateAppointmentRequest overlapping = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", LocalDateTime.of(2026, 8, 11, 13, 0), 60, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), overlapping))
                .isInstanceOfSatisfying(AppointmentConflictException.class, exception -> {
                    assertThat(exception.suggestedStart()).isEqualTo(LocalDateTime.of(2026, 8, 11, 16, 20));
                    assertThat(exception.suggestedEnd()).isEqualTo(LocalDateTime.of(2026, 8, 11, 17, 20));
                });
    }

    @Test
    void creatingAnAppointmentExactlyTheBufferAfterAnExistingOneIsAllowed() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest afterBuffer = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.end().plusMinutes(BUFFER_MINUTES), 60, false, false, null, List.of());

        assertThat(service.create(owner.id(), afterBuffer)).isNotNull();
        assertThat(service.listAll(owner.id())).hasSize(2);
    }

    @Test
    void anAccountWithAZeroBufferMayScheduleAnAppointmentDirectlyAfterAnother() {
        setBufferMinutes(owner, 0);
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest directlyAfter = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.end(), 60, false, false, null, List.of());

        assertThat(service.create(owner.id(), directlyAfter)).isNotNull();
    }

    @Test
    void anAccountsLongerBufferBlocksAnAppointmentThatTheDefaultBufferWouldAllow() {
        setBufferMinutes(owner, 60);
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest tooCloseForTheLongBuffer = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.end().plusMinutes(BUFFER_MINUTES), 60, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), tooCloseForTheLongBuffer))
                .isInstanceOfSatisfying(AppointmentConflictException.class, exception ->
                        assertThat(exception.suggestedStart()).isEqualTo(created.end().plusMinutes(60)));
    }

    @Test
    void creatingAnAppointmentCloserThanTheBufferAfterAnExistingOneIsBlockedWithoutOverlapping() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest tooClose = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.end().plusMinutes(BUFFER_MINUTES - 1), 60, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), tooClose))
                .isInstanceOfSatisfying(AppointmentConflictException.class, exception ->
                        assertThat(exception.suggestedStart()).isEqualTo(created.end().plusMinutes(BUFFER_MINUTES)));
        assertThat(service.listAll(owner.id())).hasSize(1);
    }

    @Test
    void creatingAnAppointmentEndingCloserThanTheBufferBeforeAnExistingOneIsBlocked() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest tooClose = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.start().minusMinutes(60 + BUFFER_MINUTES - 1), 60, false, false, null, List.of());
        CreateAppointmentRequest beforeBuffer = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.start().minusMinutes(60 + BUFFER_MINUTES), 60, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), tooClose)).isInstanceOf(AppointmentConflictException.class);
        assertThat(service.create(owner.id(), beforeBuffer)).isNotNull();
    }

    @Test
    void theSuggestedSlotKeepsTheBufferAndCanBeCreatedWithoutAnotherConflict() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest overlapping = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", created.start().plusMinutes(30), 60, false, false, null, List.of());

        AppointmentConflictException conflict = catchThrowableOfType(
                AppointmentConflictException.class, () -> service.create(owner.id(), overlapping));
        CreateAppointmentRequest atSuggestion = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", conflict.suggestedStart(), 60, false, false, null, List.of());

        assertThat(conflict.suggestedStart()).isEqualTo(created.end().plusMinutes(BUFFER_MINUTES));
        assertThat(service.create(owner.id(), atSuggestion)).isNotNull();
    }

    @Test
    void appointmentsOfDifferentAccountsAtTheSameTimeDoNotConflict() {
        service.create(owner.id(), createRequest(false, null));
        propertyRepository.save(new Property(
                "other-owner-property", otherOwner.id(), "Anderes Objekt", "Anderestr. 1", "pi-building"));
        CreateAppointmentRequest sameTimeOtherOwner = new CreateAppointmentRequest(
                "Fensterreinigung", "other-owner-property", "", LocalDateTime.of(2026, 8, 11, 13, 0), 120, false, false, null, List.of());

        assertThat(service.create(otherOwner.id(), sameTimeOtherOwner)).isNotNull();
    }

    @Test
    void aCompletedAppointmentOnlyBlocksUpToItsActualEnd() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        service.complete(owner.id(), created.id(), LocalDateTime.of(2026, 8, 11, 14, 0));
        CreateAppointmentRequest afterActualEnd = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", LocalDateTime.of(2026, 8, 11, 14, 15), 60, false, false, null, List.of());

        assertThat(service.create(owner.id(), afterActualEnd)).isNotNull();

        CreateAppointmentRequest beforeActualEnd = new CreateAppointmentRequest(
                "Kellerreinigung", "property-1", "", LocalDateTime.of(2026, 8, 11, 13, 30), 15, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), beforeActualEnd)).isInstanceOf(AppointmentConflictException.class);
    }

    @Test
    void aConflictingAppointmentDoesNotUseUpADemoAccountsCreationLimit() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 2);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo-Objekt", "Demostr. 1", "pi-building"));
        service.create(demo.id(), createRequestFor("demo-property", false, null));
        CreateAppointmentRequest overlapping = new CreateAppointmentRequest(
                "Fensterreinigung", "demo-property", "", LocalDateTime.of(2026, 8, 11, 14, 0), 60, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(demo.id(), overlapping)).isInstanceOf(AppointmentConflictException.class);

        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingAppointmentCreations()).isEqualTo(1);
    }

    @Test
    void theConflictExceptionNeverSuggestsASunday() {
        // 2026-08-15 is a Saturday; this appointment occupies its last hour through all of Sunday 2026-08-16.
        CreateAppointmentRequest spanningIntoSunday = new CreateAppointmentRequest(
                "Wartung", "property-1", "", LocalDateTime.of(2026, 8, 15, 23, 0), 1440, false, false, null, List.of());
        service.create(owner.id(), spanningIntoSunday);
        CreateAppointmentRequest overlapping = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", LocalDateTime.of(2026, 8, 15, 23, 0), 60, false, false, null, List.of());

        assertThatThrownBy(() -> service.create(owner.id(), overlapping))
                .isInstanceOfSatisfying(AppointmentConflictException.class, exception -> {
                    assertThat(exception.suggestedStart()).isEqualTo(LocalDateTime.of(2026, 8, 17, 23, 5));
                    assertThat(exception.suggestedStart().getDayOfWeek()).isNotEqualTo(DayOfWeek.SUNDAY);
                });
    }

    @Test
    void concurrentCreationOfOverlappingAppointmentsResultsInExactlyOneSuccess() throws Exception {
        CreateAppointmentRequest requestA = createRequest(false, null);
        CreateAppointmentRequest requestB = new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", requestA.start(), requestA.durationMinutes(), false, false, null, List.of());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        try {
            List<Future<Boolean>> results = executor.invokeAll(List.of(
                    attemptCreate(barrier, requestA), attemptCreate(barrier, requestB)));
            long successCount = results.stream().filter(AppointmentServiceTest::succeeded).count();

            assertThat(successCount).isEqualTo(1);
            assertThat(service.listAll(owner.id())).hasSize(1);
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Boolean> attemptCreate(CyclicBarrier barrier, CreateAppointmentRequest request) {
        return () -> {
            barrier.await();
            try {
                service.create(owner.id(), request);
                return true;
            } catch (AppointmentConflictException exception) {
                return false;
            }
        };
    }

    private static boolean succeeded(Future<Boolean> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void anotherAccountCanNeitherSeeNorChangeAnAppointment() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        MoveAppointmentRequest moveRequest = new MoveAppointmentRequest(created.start().plusDays(1), 60);

        assertThat(service.listAll(otherOwner.id())).isEmpty();
        assertThatThrownBy(() -> service.getById(otherOwner.id(), created.id()))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> service.move(otherOwner.id(), created.id(), moveRequest))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> service.update(otherOwner.id(), created.id(), updateRequest(created, "property-1")))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> service.complete(otherOwner.id(), created.id(), null))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> service.reopen(otherOwner.id(), created.id()))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> service.delete(otherOwner.id(), created.id(), "single"))
                .isInstanceOf(AppointmentNotFoundException.class);
        assertThat(service.getById(owner.id(), created.id())).isEqualTo(created);
    }

    @Test
    void aDemoAccountCanOnlyCreateAsManyAppointmentsAsItsRemainingLimitCountingASeriesOnce() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 2);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo-Objekt", "Demostr. 1", "pi-building"));
        CreateAppointmentRequest series = createRequestFor("demo-property", true, 3);
        CreateAppointmentRequest single = new CreateAppointmentRequest(
                "Kellerreinigung Q3", "demo-property", "", LocalDateTime.of(2026, 8, 12, 13, 0), 120, false, false, null, List.of());
        CreateAppointmentRequest thirdAttempt = new CreateAppointmentRequest(
                "Kellerreinigung Q3", "demo-property", "", LocalDateTime.of(2026, 8, 13, 13, 0), 120, false, false, null, List.of());

        service.create(demo.id(), series);
        service.create(demo.id(), single);

        assertThatThrownBy(() -> service.create(demo.id(), thirdAttempt)).isInstanceOf(CreationQuotaExceededException.class);
        assertThat(service.listAll(demo.id())).hasSize(AppointmentService.RECURRENCE_HORIZON_OCCURRENCES + 1);
    }

    @Test
    void aRejectedAppointmentDoesNotUseUpADemoAccountsLimit() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 1);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo-Objekt", "Demostr. 1", "pi-building"));

        assertThatThrownBy(() -> service.create(demo.id(), createRequestFor("unknown-property", false, null)))
                .isInstanceOf(PropertyNotFoundException.class);
        assertThatThrownBy(() -> service.create(demo.id(), createRequestFor("demo-property", true, null)))
                .isInstanceOf(InvalidRecurrenceException.class);

        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingAppointmentCreations()).isEqualTo(1);
    }

    @Test
    void createsASingleNonRecurringAppointment() {
        AppointmentResponse response = service.create(owner.id(), createRequest(false, null));

        assertThat(response.recurring()).isFalse();
        assertThat(response.seriesId()).isNull();
        assertThat(response.history()).hasSize(1);
        assertThat(service.listAll(owner.id())).hasSize(1);
    }

    @Test
    void createdAppointmentTakesNameAndAddressFromItsProperty() {
        AppointmentResponse response = service.create(owner.id(), createRequest(false, null));

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

        assertThatThrownBy(() -> service.create(owner.id(), request)).isInstanceOf(PropertyNotFoundException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void readingAnAppointmentBackReturnsTheSameDataAsItsCreation() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));

        assertThat(service.getById(owner.id(), created.id())).isEqualTo(created);
    }

    @Test
    void subMicrosecondStartTimesAreTruncatedToTheStoredPrecisionOnCreateAndMove() {
        LocalDateTime startWithNanos = LocalDateTime.of(2026, 8, 11, 13, 0, 0, 123_456_789);
        AppointmentResponse created = service.create(owner.id(), new CreateAppointmentRequest(
                "Kellerreinigung Q3", "property-1", "", startWithNanos, 120, false, false, null, List.of()));

        AppointmentResponse moved = service.move(owner.id(), created.id(), new MoveAppointmentRequest(startWithNanos.plusDays(1), 60));

        assertThat(created.start()).isEqualTo(LocalDateTime.of(2026, 8, 11, 13, 0, 0, 123_456_000));
        assertThat(moved.start()).isEqualTo(LocalDateTime.of(2026, 8, 12, 13, 0, 0, 123_456_000));
        assertThat(service.getById(owner.id(), created.id())).isEqualTo(moved);
    }

    @Test
    void recurringAppointmentRequiresARecurrenceInterval() {
        assertThatThrownBy(() -> service.create(owner.id(), createRequest(true, null)))
                .isInstanceOf(InvalidRecurrenceException.class);
    }

    @Test
    void recurringAppointmentMaterializesTwelveOccurrencesSharingOneSeriesId() {
        AppointmentResponse firstOccurrence = service.create(owner.id(), createRequest(true, 3));

        List<AppointmentResponse> all = service.listAll(owner.id());

        assertThat(all).hasSize(AppointmentService.RECURRENCE_HORIZON_OCCURRENCES);
        assertThat(all).allMatch(response -> firstOccurrence.seriesId().equals(response.seriesId()));
        assertThat(all.get(1).start()).isEqualTo(firstOccurrence.start().plusMonths(3));
    }

    @Test
    void movingAnUnlockedAppointmentUpdatesItsScheduleAndHistory() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        LocalDateTime newStart = created.start().plusDays(1);

        AppointmentResponse moved = service.move(owner.id(), created.id(), new MoveAppointmentRequest(newStart, 120));

        assertThat(moved.start()).isEqualTo(newStart);
        assertThat(moved.history()).hasSize(2);
        assertThat(service.getById(owner.id(), created.id()).start()).isEqualTo(newStart);
    }

    @Test
    void movingAnAppointmentIntoAnotherOneIsBlockedWithTheNextFreeSlotAndLeavesItUnchanged() {
        AppointmentResponse blocker = service.create(owner.id(), createRequest(false, null));
        AppointmentResponse movable = service.create(owner.id(), new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", LocalDateTime.of(2026, 8, 12, 9, 0), 60, false, false, null, List.of()));
        MoveAppointmentRequest intoBlocker = new MoveAppointmentRequest(blocker.start().plusMinutes(30), 60);

        assertThatThrownBy(() -> service.move(owner.id(), movable.id(), intoBlocker))
                .isInstanceOfSatisfying(AppointmentConflictException.class, exception -> {
                    assertThat(exception.suggestedStart()).isEqualTo(blocker.end().plusMinutes(BUFFER_MINUTES));
                    assertThat(exception.suggestedEnd()).isEqualTo(blocker.end().plusMinutes(BUFFER_MINUTES + 60));
                });
        assertThat(service.getById(owner.id(), movable.id())).isEqualTo(movable);
    }

    @Test
    void movingAnAppointmentCloserThanTheBufferToAnotherOneIsBlockedWithoutOverlapping() {
        AppointmentResponse blocker = service.create(owner.id(), createRequest(false, null));
        AppointmentResponse movable = service.create(owner.id(), new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", LocalDateTime.of(2026, 8, 12, 9, 0), 60, false, false, null, List.of()));

        assertThatThrownBy(() -> service.move(owner.id(), movable.id(), new MoveAppointmentRequest(blocker.end(), 60)))
                .isInstanceOf(AppointmentConflictException.class);
        assertThat(service.move(owner.id(), movable.id(), new MoveAppointmentRequest(blocker.end().plusMinutes(BUFFER_MINUTES), 60)))
                .isNotNull();
    }

    @Test
    void movingAnAppointmentOverlappingItsOwnPreviousTimeIsNotAConflict() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));

        AppointmentResponse shifted = service.move(owner.id(), created.id(), new MoveAppointmentRequest(created.start().plusMinutes(30), 120));

        assertThat(shifted.start()).isEqualTo(created.start().plusMinutes(30));
    }

    @Test
    void movingAnAppointmentToItsCurrentSlotIsAllowedEvenIfANeighbourAlreadyLiesWithinTheBuffer() {
        AppointmentResponse first = service.create(owner.id(), createRequest(false, null));
        AppointmentResponse second = service.create(owner.id(), new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", first.end().plusMinutes(BUFFER_MINUTES), 60, false, false, null, List.of()));
        service.update(owner.id(), second.id(), new CreateAppointmentRequest(
                "Fensterreinigung", "property-1", "", first.end(), 60, false, false, null, List.of()));

        AppointmentResponse unchanged = service.move(owner.id(), second.id(), new MoveAppointmentRequest(first.end(), 60));

        assertThat(unchanged.start()).isEqualTo(first.end());
    }

    @Test
    void movingAnAppointmentOntoAnotherAccountsAppointmentIsNotAConflict() {
        service.create(owner.id(), createRequest(false, null));
        propertyRepository.save(new Property(
                "other-owner-property", otherOwner.id(), "Anderes Objekt", "Anderestr. 1", "pi-building"));
        AppointmentResponse foreign = service.create(otherOwner.id(), new CreateAppointmentRequest(
                "Fensterreinigung", "other-owner-property", "", LocalDateTime.of(2026, 8, 12, 9, 0), 60, false, false, null, List.of()));

        AppointmentResponse moved = service.move(
                otherOwner.id(), foreign.id(), new MoveAppointmentRequest(LocalDateTime.of(2026, 8, 11, 13, 0), 120));

        assertThat(moved.start()).isEqualTo(LocalDateTime.of(2026, 8, 11, 13, 0));
    }

    @Test
    void movingALockedAppointmentIsRejected() {
        CreateAppointmentRequest lockedRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin",
                LocalDateTime.of(2026, 8, 11, 9, 0), 60, true, false, null, List.of());
        AppointmentResponse created = service.create(owner.id(), lockedRequest);

        assertThatThrownBy(() -> service.move(owner.id(), created.id(), new MoveAppointmentRequest(created.start().plusDays(1), 60)))
                .isInstanceOf(AppointmentLockedException.class);
    }

    @Test
    void updatingAnAppointmentChangesTitlePropertyScheduleDescriptionAndMaterials() {
        propertyRepository.save(new Property("property-2", owner.id(), "Nebengebäude", "Nebenstr. 2", "pi-building"));
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        LocalDateTime newStart = created.start().plusDays(2);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Fensterreinigung", "property-2", "Neue Beschreibung", newStart, 90, false, false, null, List.of("Fensterwischer"));

        AppointmentResponse updated = service.update(owner.id(), created.id(), request);

        assertThat(updated.title()).isEqualTo("Fensterreinigung");
        assertThat(updated.propertyId()).isEqualTo("property-2");
        assertThat(updated.description()).isEqualTo("Neue Beschreibung");
        assertThat(updated.start()).isEqualTo(newStart);
        assertThat(updated.end()).isEqualTo(newStart.plusMinutes(90));
        assertThat(updated.materials()).containsExactly("Fensterwischer");
        assertThat(updated.history()).hasSize(2);
        assertThat(service.getById(owner.id(), created.id())).isEqualTo(updated);
    }

    @Test
    void updatingAnAppointmentForAnUnknownPropertyIsRejected() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));

        assertThatThrownBy(() -> service.update(owner.id(), created.id(), updateRequest(created, "unknown-property")))
                .isInstanceOf(PropertyNotFoundException.class);
        assertThat(service.getById(owner.id(), created.id())).isEqualTo(created);
    }

    @Test
    void updatingAnUnknownAppointmentIsRejected() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));

        assertThatThrownBy(() -> service.update(owner.id(), "unknown-appointment", updateRequest(created, "property-1")))
                .isInstanceOf(AppointmentNotFoundException.class);
    }

    @Test
    void updatingALockedAppointmentWithoutChangingItsScheduleIsAllowed() {
        CreateAppointmentRequest lockedRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin",
                LocalDateTime.of(2026, 8, 11, 9, 0), 60, true, false, null, List.of());
        AppointmentResponse created = service.create(owner.id(), lockedRequest);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "TÜV-Termin (aktualisiert)", "property-1", "Pflichttermin", created.start(), 60, true, false, null, List.of());

        AppointmentResponse updated = service.update(owner.id(), created.id(), request);

        assertThat(updated.title()).isEqualTo("TÜV-Termin (aktualisiert)");
        assertThat(updated.start()).isEqualTo(created.start());
    }

    @Test
    void updatingALockedAppointmentsScheduleIsRejected() {
        CreateAppointmentRequest lockedRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin",
                LocalDateTime.of(2026, 8, 11, 9, 0), 60, true, false, null, List.of());
        AppointmentResponse created = service.create(owner.id(), lockedRequest);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin", created.start().plusDays(1), 60, true, false, null, List.of());

        assertThatThrownBy(() -> service.update(owner.id(), created.id(), request)).isInstanceOf(AppointmentLockedException.class);
    }

    @Test
    void updatingAnAppointmentCanUnlockItAndThenChangeItsSchedule() {
        CreateAppointmentRequest lockedRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin",
                LocalDateTime.of(2026, 8, 11, 9, 0), 60, true, false, null, List.of());
        AppointmentResponse created = service.create(owner.id(), lockedRequest);
        LocalDateTime newStart = created.start().plusDays(1);
        CreateAppointmentRequest unlockRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin", created.start(), 60, false, false, null, List.of());
        service.update(owner.id(), created.id(), unlockRequest);

        CreateAppointmentRequest rescheduleRequest = new CreateAppointmentRequest(
                "TÜV-Termin", "property-1", "Pflichttermin", newStart, 60, false, false, null, List.of());
        AppointmentResponse updated = service.update(owner.id(), created.id(), rescheduleRequest);

        assertThat(updated.locked()).isFalse();
        assertThat(updated.start()).isEqualTo(newStart);
    }

    @Test
    void updatingANonRecurringAppointmentToRecurringAssignsItANewSeriesIdAndMaterializesTheRemainingOccurrences() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                created.title(), "property-1", created.description(), created.start(), 60, false, true, 3, List.of());

        AppointmentResponse updated = service.update(owner.id(), created.id(), request);
        List<AppointmentResponse> all = service.listAll(owner.id());

        assertThat(updated.recurring()).isTrue();
        assertThat(updated.seriesId()).isNotNull();
        assertThat(updated.recurrenceIntervalMonths()).isEqualTo(3);
        assertThat(all).hasSize(AppointmentService.RECURRENCE_HORIZON_OCCURRENCES);
        assertThat(all).allMatch(response -> updated.seriesId().equals(response.seriesId()));
        assertThat(all.get(1).start()).isEqualTo(updated.start().plusMonths(3));
    }

    @Test
    void updatingARecurringAppointmentToNonRecurringDetachesItFromItsSeriesWithoutTouchingSiblings() {
        AppointmentResponse firstOccurrence = service.create(owner.id(), createRequest(true, 3));
        AppointmentResponse secondOccurrence = service.listAll(owner.id()).get(1);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                secondOccurrence.title(), "property-1", secondOccurrence.description(),
                secondOccurrence.start(), 60, false, false, null, List.of());

        AppointmentResponse detached = service.update(owner.id(), secondOccurrence.id(), request);
        service.delete(owner.id(), firstOccurrence.id(), "series");

        assertThat(detached.recurring()).isFalse();
        assertThat(detached.seriesId()).isNull();
        assertThat(service.listAll(owner.id())).containsExactly(detached);
    }

    @Test
    void togglingRecurringOnWithoutARecurrenceIntervalIsRejected() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                created.title(), "property-1", created.description(), created.start(), 60, false, true, null, List.of());

        assertThatThrownBy(() -> service.update(owner.id(), created.id(), request)).isInstanceOf(InvalidRecurrenceException.class);
    }

    @Test
    void completingAnAppointmentWithoutAnExplicitActualEndFallsBackToNow() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));

        AppointmentResponse completed = service.complete(owner.id(), created.id(), null);

        assertThat(completed.completed()).isTrue();
        assertThat(completed.actualEnd()).isNotNull();
        assertThat(completed.history()).hasSize(2);
    }

    @Test
    void completingAnAppointmentWithAnExplicitActualEndUsesIt() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        LocalDateTime explicitActualEnd = LocalDateTime.of(2026, 8, 11, 16, 30);

        AppointmentResponse completed = service.complete(owner.id(), created.id(), explicitActualEnd);

        assertThat(completed.actualEnd()).isEqualTo(explicitActualEnd);
    }

    @Test
    void completingAnAppointmentWithAnActualEndBeforeItsPlannedStartIsRejected() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        LocalDateTime actualEndBeforeStart = created.start().minusHours(1);

        assertThatThrownBy(() -> service.complete(owner.id(), created.id(), actualEndBeforeStart))
                .isInstanceOf(InvalidActualEndException.class);
        assertThat(service.getById(owner.id(), created.id()).completed()).isFalse();
    }

    @Test
    void reopeningACompletedAppointmentClearsActualEnd() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));
        service.complete(owner.id(), created.id(), null);

        AppointmentResponse reopened = service.reopen(owner.id(), created.id());

        assertThat(reopened.completed()).isFalse();
        assertThat(reopened.actualEnd()).isNull();
        assertThat(reopened.history()).hasSize(3);
        assertThat(service.getById(owner.id(), created.id()).completed()).isFalse();
    }

    @Test
    void deletingWithSingleScopeRemovesOnlyThatOccurrence() {
        AppointmentResponse firstOccurrence = service.create(owner.id(), createRequest(true, 3));

        service.delete(owner.id(), firstOccurrence.id(), "single");

        assertThat(service.listAll(owner.id())).hasSize(AppointmentService.RECURRENCE_HORIZON_OCCURRENCES - 1);
    }

    @Test
    void deletingWithSeriesScopeRemovesThisAndAllFollowingOccurrences() {
        AppointmentResponse firstOccurrence = service.create(owner.id(), createRequest(true, 3));
        AppointmentResponse secondOccurrence = service.listAll(owner.id()).get(1);

        service.delete(owner.id(), secondOccurrence.id(), "series");

        assertThat(service.listAll(owner.id())).containsExactly(firstOccurrence);
    }

    @Test
    void deletingWithSeriesScopeOnANonRecurringAppointmentIsRejected() {
        AppointmentResponse created = service.create(owner.id(), createRequest(false, null));

        assertThatThrownBy(() -> service.delete(owner.id(), created.id(), "series")).isInstanceOf(InvalidRecurrenceException.class);
    }

    private CreateAppointmentRequest createRequest(boolean recurring, Integer recurrenceIntervalMonths) {
        return createRequestFor("property-1", recurring, recurrenceIntervalMonths);
    }

    private CreateAppointmentRequest updateRequest(AppointmentResponse appointment, String propertyId) {
        return new CreateAppointmentRequest(
                appointment.title(), propertyId, appointment.description(), appointment.start(), 60, false, false, null, List.of());
    }

    private void setBufferMinutes(User account, int bufferMinutes) {
        User stored = userRepository.findById(account.id()).orElseThrow();
        stored.changeAppointmentBufferMinutes(bufferMinutes);
        userRepository.save(stored);
    }

    private CreateAppointmentRequest createRequestFor(String propertyId, boolean recurring, Integer recurrenceIntervalMonths) {
        return new CreateAppointmentRequest(
                "Kellerreinigung Q3",
                propertyId,
                "Was ist zu tun?",
                LocalDateTime.of(2026, 8, 11, 13, 0),
                120,
                false,
                recurring,
                recurrenceIntervalMonths,
                List.of("Kehrmaschine", "Müllsäcke 10x"));
    }
}
