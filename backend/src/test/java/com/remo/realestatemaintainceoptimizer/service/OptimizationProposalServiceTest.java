package com.remo.realestatemaintainceoptimizer.service;

import static com.remo.realestatemaintainceoptimizer.OptimizationFixtures.appointment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.remo.realestatemaintainceoptimizer.OptimizationFixtures;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationProposalResponse;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationRun;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalNotPendingException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalOutdatedException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationProposalRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationRunRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies deciding on optimization proposals against a real PostgreSQL database with a stubbed travel matrix: an accepted move is re-validated, applied with an optimization history entry and its savings confirmed, while an outdated one is expired without touching the appointment.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OptimizationProposalServiceTest {

    private static final LocalDate DAY_ONE = OptimizationFixtures.firstPlannableTuesday();
    private static final LocalDate DAY_TWO = DAY_ONE.plusDays(1);
    private static final LocalDateTime PROPOSED_START = DAY_TWO.atTime(9, 15);

    @Autowired
    private OptimizationProposalService service;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private OptimizationRunRepository runRepository;

    @Autowired
    private OptimizationProposalRepository proposalRepository;

    @MockitoBean
    private DistanceMatrixService distanceMatrixService;

    private User owner;
    private Property west;
    private Property east;
    private OptimizationRun run;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        west = propertyRepository.save(OptimizationFixtures.westProperty(owner.id()));
        east = propertyRepository.save(OptimizationFixtures.eastProperty(owner.id()));
        run = runRepository.save(new OptimizationRun(UUID.randomUUID().toString(), owner.id(), Instant.now(), "claude-haiku-4-5", 1));
        when(distanceMatrixService.matrix(anyList()))
                .thenAnswer(invocation -> OptimizationFixtures.matrixFor(invocation.<List<Location>>getArgument(0)));
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    /** Day one visits west and then east (movable), day two only east; returns the pending proposal to move day one's east visit next to day two's. */
    private OptimizationProposal proposeSplitScheduleMove(boolean recurring) {
        appointmentRepository.save(appointment(west, DAY_ONE.atTime(8, 0), true));
        Appointment movable = appointmentRepository.save(appointment(east, DAY_ONE.atTime(10, 0), false, recurring));
        appointmentRepository.save(appointment(east, DAY_TWO.atTime(8, 0), true));
        return saveProposal(owner, movable, PROPOSED_START);
    }

    private OptimizationProposal saveProposal(User account, Appointment appointment, LocalDateTime proposedStart) {
        return proposalRepository.save(new OptimizationProposal(UUID.randomUUID().toString(), run.id(), account.id(), appointment,
                proposedStart, proposedStart.plusMinutes(60), 1.0, 1.0, "Spart Fahrzeit.", Instant.now()));
    }

    private OptimizationProposalStatus storedStatus(OptimizationProposal proposal) {
        return proposalRepository.findById(proposal.id()).orElseThrow().status();
    }

    @Test
    void acceptingMovesTheAppointmentRecordsTheOptimizationAndConfirmsTheRecalculatedSavings() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);

        OptimizationProposalResponse accepted = service.accept(owner.id(), proposal.id());

        assertThat(accepted.status()).isEqualTo(OptimizationProposalStatus.ACCEPTED);
        assertThat(accepted.decidedAt()).isNotNull();
        assertThat(accepted.savedDistanceMeters()).isEqualTo(OptimizationFixtures.WEST_EAST_METERS);
        assertThat(accepted.savedDurationSeconds()).isEqualTo(OptimizationFixtures.WEST_EAST_SECONDS);
        var moved = appointmentService.getById(owner.id(), proposal.appointmentId());
        assertThat(moved.start()).isEqualTo(PROPOSED_START);
        assertThat(moved.end()).isEqualTo(PROPOSED_START.plusMinutes(60));
        assertThat(moved.history().getLast().message()).startsWith("Moved by the AI optimization from");
        assertThat(storedStatus(proposal)).isEqualTo(OptimizationProposalStatus.ACCEPTED);
    }

    @Test
    void acceptingARecurringOccurrenceRemembersTheStartItWasOriginallyPlannedFor() {
        OptimizationProposal proposal = proposeSplitScheduleMove(true);

        service.accept(owner.id(), proposal.id());

        Appointment moved = appointmentRepository.findById(proposal.appointmentId()).orElseThrow();
        assertThat(moved.start()).isEqualTo(PROPOSED_START);
        assertThat(moved.recurrenceAnchor()).isEqualTo(DAY_ONE.atTime(10, 0));
    }

    @Test
    void aProposalWhoseAppointmentWasMovedMeanwhileIsExpiredAndTheAppointmentStaysWhereTheUserPutIt() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);
        LocalDateTime manualStart = DAY_ONE.atTime(15, 0);
        appointmentService.move(owner.id(), proposal.appointmentId(), new MoveAppointmentRequest(manualStart, 60));

        assertThatThrownBy(() -> service.accept(owner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalOutdatedException.class);

        assertThat(storedStatus(proposal)).isEqualTo(OptimizationProposalStatus.EXPIRED);
        assertThat(appointmentRepository.findById(proposal.appointmentId()).orElseThrow().start()).isEqualTo(manualStart);
    }

    @Test
    void aProposalWhoseTargetSlotWasTakenMeanwhileIsOutdated() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);
        appointmentRepository.save(appointment(west, DAY_TWO.atTime(10, 0), true));

        assertThatThrownBy(() -> service.accept(owner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalOutdatedException.class);

        assertThat(appointmentRepository.findById(proposal.appointmentId()).orElseThrow().start()).isEqualTo(DAY_ONE.atTime(10, 0));
    }

    @Test
    void aProposalWhoseAppointmentWasDeletedIsNoLongerListedAndCannotBeAccepted() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);
        appointmentService.delete(owner.id(), proposal.appointmentId(), "single");

        assertThat(service.listPending(owner.id())).isEmpty();
        assertThatThrownBy(() -> service.accept(owner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalOutdatedException.class);
        assertThat(storedStatus(proposal)).isEqualTo(OptimizationProposalStatus.EXPIRED);
    }

    @Test
    void rejectingKeepsTheAppointmentAndMarksTheProposalRejected() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);

        OptimizationProposalResponse rejected = service.reject(owner.id(), proposal.id());

        assertThat(rejected.status()).isEqualTo(OptimizationProposalStatus.REJECTED);
        assertThat(storedStatus(proposal)).isEqualTo(OptimizationProposalStatus.REJECTED);
        assertThat(appointmentRepository.findById(proposal.appointmentId()).orElseThrow().start()).isEqualTo(DAY_ONE.atTime(10, 0));
    }

    @Test
    void aDecidedProposalCannotBeDecidedAgain() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);
        service.reject(owner.id(), proposal.id());

        assertThatThrownBy(() -> service.accept(owner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalNotPendingException.class);
        assertThatThrownBy(() -> service.reject(owner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalNotPendingException.class);
    }

    @Test
    void anotherAccountsProposalIsNeitherListedNorDecidable() {
        OptimizationProposal proposal = proposeSplitScheduleMove(false);
        User otherOwner = TestAccounts.saveRegularAccount(userRepository);

        assertThat(service.listPending(otherOwner.id())).isEmpty();
        assertThatThrownBy(() -> service.accept(otherOwner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalNotFoundException.class);
        assertThatThrownBy(() -> service.reject(otherOwner.id(), proposal.id()))
                .isInstanceOf(OptimizationProposalNotFoundException.class);
        assertThat(storedStatus(proposal)).isEqualTo(OptimizationProposalStatus.PENDING);
    }

    @Test
    void listsThePendingProposalsSortedByTheirProposedStart() {
        Appointment first = appointmentRepository.save(appointment(east, DAY_ONE.atTime(10, 0), false));
        Appointment second = appointmentRepository.save(appointment(west, DAY_ONE.atTime(12, 0), false));
        OptimizationProposal later = saveProposal(owner, first, DAY_TWO.atTime(14, 0));
        OptimizationProposal earlier = saveProposal(owner, second, DAY_TWO.atTime(9, 0));
        service.reject(owner.id(), saveProposal(owner, first, DAY_TWO.atTime(7, 0)).id());

        assertThat(service.listPending(owner.id()))
                .extracting(OptimizationProposalResponse::id)
                .containsExactly(earlier.id(), later.id());
    }
}
