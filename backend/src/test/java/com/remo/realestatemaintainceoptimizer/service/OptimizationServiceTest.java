package com.remo.realestatemaintainceoptimizer.service;

import static com.remo.realestatemaintainceoptimizer.OptimizationFixtures.appointment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.remo.realestatemaintainceoptimizer.OptimizationFixtures;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.ai.AdvisorCandidate;
import com.remo.realestatemaintainceoptimizer.ai.AdvisorSelection;
import com.remo.realestatemaintainceoptimizer.ai.OptimizationAdvisor;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationRunResponse;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AiDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.AiUnavailableException;
import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationProposalRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationRunRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.UsernamePolicy;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies an optimization run end to end against a real PostgreSQL database with a stubbed travel matrix and AI: stored proposals and savings, untouched appointments, superseded proposals, sequential re-validation of the AI's selection, the single run of a demo account and the run limit.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OptimizationServiceTest {

    private static final LocalDate DAY_ONE = OptimizationFixtures.firstPlannableTuesday();
    private static final LocalDate DAY_TWO = DAY_ONE.plusDays(1);
    private static final LocalDate DAY_THREE = DAY_ONE.plusDays(2);

    @Autowired
    private OptimizationService service;

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

    @Autowired
    private DemoDataSeeder demoDataSeeder;

    @MockitoBean
    private DistanceMatrixService distanceMatrixService;

    @MockitoBean
    private OptimizationAdvisor advisor;

    private User owner;
    private Property west;
    private Property east;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        west = propertyRepository.save(OptimizationFixtures.westProperty(owner.id()));
        east = propertyRepository.save(OptimizationFixtures.eastProperty(owner.id()));
        when(distanceMatrixService.matrix(anyList()))
                .thenAnswer(invocation -> OptimizationFixtures.matrixFor(invocation.<List<Location>>getArgument(0)));
        when(advisor.modelName()).thenReturn("claude-haiku-4-5");
        when(advisor.selectMoves(anyList(), any())).thenAnswer(invocation -> selectEveryCandidateOnDayTwo(invocation.getArgument(0)));
    }

    private static List<AdvisorSelection> selectEveryCandidateOnDayTwo(List<AdvisorCandidate> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.proposedStart().toLocalDate().equals(DAY_TWO))
                .map(candidate -> new AdvisorSelection(candidate.candidateId(), "Spart die Fahrt zwischen West und Ost."))
                .toList();
    }

    /** Day one visits west and then east (movable), day two only east: the AI should move day one's east visit next to day two's. */
    private Appointment seedSplitSchedule(Property westProperty, Property eastProperty) {
        appointmentRepository.save(appointment(westProperty, DAY_ONE.atTime(8, 0), true));
        Appointment movable = appointmentRepository.save(appointment(eastProperty, DAY_ONE.atTime(10, 0), false));
        appointmentRepository.save(appointment(eastProperty, DAY_TWO.atTime(8, 0), true));
        return movable;
    }

    @Test
    void storesTheSelectedMoveAsPendingProposalWithItsSavingsAndLeavesTheAppointmentUntouched() {
        Appointment movable = seedSplitSchedule(west, east);

        OptimizationRunResponse response = service.run(owner.id(), Locale.GERMAN);

        assertThat(response.analyzedAppointmentCount()).isEqualTo(1);
        assertThat(response.candidateCount()).isPositive();
        assertThat(response.proposals()).singleElement().satisfies(proposal -> {
            assertThat(proposal.appointmentId()).isEqualTo(movable.id());
            assertThat(proposal.propertyName()).isEqualTo("Rheinhaus Ost");
            assertThat(proposal.originalStart()).isEqualTo(DAY_ONE.atTime(10, 0));
            assertThat(proposal.proposedStart()).isEqualTo(DAY_TWO.atTime(9, 15));
            assertThat(proposal.proposedEnd()).isEqualTo(DAY_TWO.atTime(10, 15));
            assertThat(proposal.savedDistanceMeters()).isEqualTo(OptimizationFixtures.WEST_EAST_METERS);
            assertThat(proposal.savedDurationSeconds()).isEqualTo(OptimizationFixtures.WEST_EAST_SECONDS);
            assertThat(proposal.reason()).isEqualTo("Spart die Fahrt zwischen West und Ost.");
            assertThat(proposal.status()).isEqualTo(OptimizationProposalStatus.PENDING);
        });
        assertThat(appointmentRepository.findById(movable.id()).orElseThrow().start()).isEqualTo(DAY_ONE.atTime(10, 0));
        assertThat(runRepository.findById(response.runId())).hasValueSatisfying(run ->
                assertThat(run.model()).isEqualTo("claude-haiku-4-5"));
    }

    @Test
    void passesThePlannersSavingsAndTheRequestLocaleToTheAi() {
        seedSplitSchedule(west, east);

        service.run(owner.id(), Locale.ENGLISH);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AdvisorCandidate>> candidates = ArgumentCaptor.forClass(List.class);
        verify(advisor).selectMoves(candidates.capture(), eq(Locale.ENGLISH));
        assertThat(candidates.getValue()).first().satisfies(candidate -> {
            assertThat(candidate.candidateId()).isEqualTo("c1");
            assertThat(candidate.appointmentTitle()).isEqualTo("Heizungswartung");
            assertThat(candidate.savedKilometers()).isEqualTo(10.0);
            assertThat(candidate.savedDrivingMinutes()).isEqualTo(15.0);
        });
    }

    @Test
    void aNewRunExpiresTheProposalsStillPendingFromThePreviousRun() {
        seedSplitSchedule(west, east);
        String firstProposalId = service.run(owner.id(), Locale.GERMAN).proposals().getFirst().id();

        service.run(owner.id(), Locale.GERMAN);

        assertThat(proposalRepository.findById(firstProposalId))
                .hasValueSatisfying(proposal -> assertThat(proposal.status()).isEqualTo(OptimizationProposalStatus.EXPIRED));
        assertThat(proposalRepository.findAllByOwnerIdAndStatusOrderByProposedStartAsc(owner.id(), OptimizationProposalStatus.PENDING))
                .hasSize(1);
    }

    @Test
    void keepsOnlyTheFirstOfTwoSelectedMovesThatNoLongerFitTogether() {
        seedSplitSchedule(west, east);
        appointmentRepository.save(appointment(west, DAY_THREE.atTime(8, 0), true));
        appointmentRepository.save(appointment(east, DAY_THREE.atTime(10, 0), false));

        OptimizationRunResponse response = service.run(owner.id(), Locale.GERMAN);

        assertThat(response.proposals()).singleElement()
                .satisfies(proposal -> assertThat(proposal.proposedStart()).isEqualTo(DAY_TWO.atTime(9, 15)));
    }

    @Test
    void neverProposesALockedAppointment() {
        appointmentRepository.save(appointment(west, DAY_ONE.atTime(8, 0), true));
        appointmentRepository.save(appointment(east, DAY_ONE.atTime(10, 0), true));
        appointmentRepository.save(appointment(east, DAY_TWO.atTime(8, 0), true));

        OptimizationRunResponse response = service.run(owner.id(), Locale.GERMAN);

        assertThat(response.analyzedAppointmentCount()).isZero();
        assertThat(response.proposals()).isEmpty();
        verify(advisor, never()).selectMoves(anyList(), any());
    }

    @Test
    void aDemoAccountCanRunTheAiOptimizationOnlyOnce() {
        User demo = saveDemoAccountWithOneAiRun();
        seedSplitSchedule(propertyRepository.save(OptimizationFixtures.westProperty(demo.id())),
                propertyRepository.save(OptimizationFixtures.eastProperty(demo.id())));

        service.run(demo.id(), Locale.GERMAN);

        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingAiOptimizations()).isZero();
        assertThatThrownBy(() -> service.run(demo.id(), Locale.GERMAN))
                .isInstanceOf(CreationQuotaExceededException.class)
                .extracting("resource").isEqualTo(CreationQuotaExceededException.RESOURCE_AI_OPTIMIZATION);
        verify(advisor, times(1)).selectMoves(anyList(), any());
    }

    @Test
    void aRunWithoutCandidatesNeitherConsultsTheAiNorUsesUpTheDemoRun() {
        User demo = saveDemoAccountWithOneAiRun();

        OptimizationRunResponse response = service.run(demo.id(), Locale.GERMAN);

        assertThat(response.candidateCount()).isZero();
        assertThat(response.proposals()).isEmpty();
        verify(advisor, never()).selectMoves(anyList(), any());
        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingAiOptimizations()).isEqualTo(1);
    }

    @Test
    void aFailingAiStoresNothingAndKeepsTheDemoRun() {
        User demo = saveDemoAccountWithOneAiRun();
        seedSplitSchedule(propertyRepository.save(OptimizationFixtures.westProperty(demo.id())),
                propertyRepository.save(OptimizationFixtures.eastProperty(demo.id())));
        when(advisor.selectMoves(anyList(), any())).thenThrow(new AiUnavailableException("down"));

        assertThatThrownBy(() -> service.run(demo.id(), Locale.GERMAN)).isInstanceOf(AiUnavailableException.class);

        assertThat(runRepository.count()).isZero();
        assertThat(userRepository.findById(demo.id()).orElseThrow().remainingAiOptimizations()).isEqualTo(1);
    }

    @Test
    void aDisabledAiRejectsTheRunBeforeAnythingIsCalculated() {
        doThrow(new AiDisabledException()).when(advisor).requireEnabled();

        assertThatThrownBy(() -> service.run(owner.id(), Locale.GERMAN)).isInstanceOf(AiDisabledException.class);

        verifyNoInteractions(distanceMatrixService);
        assertThat(runRepository.count()).isZero();
    }

    @Test
    void limitsTheRunsThatConsultedTheAiPerAccountAndHour() {
        seedSplitSchedule(west, east);
        for (int run = 0; run < 5; run++) {
            service.run(owner.id(), Locale.GERMAN);
        }

        assertThatThrownBy(() -> service.run(owner.id(), Locale.GERMAN))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("reasonCode").isEqualTo(RateLimitExceededException.REASON_TOO_MANY_OPTIMIZATIONS);
    }

    @Test
    void runsThatNeverConsultedTheAiDoNotCountAgainstTheRunLimit() {
        for (int run = 0; run < 7; run++) {
            service.run(owner.id(), Locale.GERMAN);
        }

        verify(advisor, never()).selectMoves(anyList(), any());
    }

    @Test
    void runsFailingBeforeTheAiWasConsultedGiveTheirRunBudgetBack() {
        seedSplitSchedule(west, east);
        when(distanceMatrixService.matrix(anyList())).thenThrow(new RoutingUnavailableException("down"));

        for (int run = 0; run < 7; run++) {
            assertThatThrownBy(() -> service.run(owner.id(), Locale.GERMAN)).isInstanceOf(RoutingUnavailableException.class);
        }
    }

    @Test
    void rejectsASecondRunOfTheSameAccountWhileTheFirstIsStillRunning() {
        seedSplitSchedule(west, east);
        when(advisor.selectMoves(anyList(), any())).thenAnswer(invocation -> {
            assertThatThrownBy(() -> service.run(owner.id(), Locale.GERMAN))
                    .isInstanceOf(RateLimitExceededException.class)
                    .extracting("reasonCode").isEqualTo(RateLimitExceededException.REASON_OPTIMIZATION_IN_PROGRESS);
            return selectEveryCandidateOnDayTwo(invocation.getArgument(0));
        });

        OptimizationRunResponse response = service.run(owner.id(), Locale.GERMAN);

        assertThat(response.proposals()).hasSize(1);
        verify(advisor, times(1)).selectMoves(anyList(), any());
        assertThat(service.run(owner.id(), Locale.GERMAN).proposals()).hasSize(1);
    }

    @Test
    void storesNoProposalForAnAppointmentDeletedWhileTheAiWasDeciding() {
        Appointment movable = seedSplitSchedule(west, east);
        when(advisor.selectMoves(anyList(), any())).thenAnswer(invocation -> {
            appointmentRepository.deleteById(movable.id());
            return selectEveryCandidateOnDayTwo(invocation.getArgument(0));
        });

        OptimizationRunResponse response = service.run(owner.id(), Locale.GERMAN);

        assertThat(response.proposals()).isEmpty();
        assertThat(proposalRepository.count()).isZero();
        assertThat(runRepository.count()).isEqualTo(1);
    }

    @Test
    void theDemoDataOffersRouteSavingMovesInsideThePlanningWindow() {
        User demo = saveDemoAccountWithOneAiRun();
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
        demoDataSeeder.seed(demo.id(), today);
        when(distanceMatrixService.matrix(anyList()))
                .thenAnswer(invocation -> OptimizationFixtures.straightLineMatrixFor(invocation.<List<Location>>getArgument(0)));
        when(advisor.selectMoves(anyList(), any())).thenAnswer(invocation -> invocation.<List<AdvisorCandidate>>getArgument(0)
                .stream()
                .map(candidate -> new AdvisorSelection(candidate.candidateId(), ""))
                .toList());

        OptimizationRunResponse response = service.run(demo.id(), Locale.GERMAN);

        assertThat(response.analyzedAppointmentCount()).isPositive();
        assertThat(response.proposals()).isNotEmpty().allSatisfy(proposal -> {
            assertThat(proposal.savedDistanceMeters()).isPositive();
            assertThat(proposal.originalStart().toLocalDate()).isAfterOrEqualTo(today.plusDays(28));
            assertThat(proposal.proposedStart().toLocalDate()).isAfterOrEqualTo(today.plusDays(28));
        });
    }

    private User saveDemoAccountWithOneAiRun() {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return userRepository.save(new User(id, UsernamePolicy.DEMO_PREFIX + id.substring(0, 8), null, "Demo", true, now,
                now.plusSeconds(3600),
                3, 3, 10, 1));
    }
}
