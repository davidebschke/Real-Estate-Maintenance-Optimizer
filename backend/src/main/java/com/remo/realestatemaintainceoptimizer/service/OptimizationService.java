package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.ai.AdvisorCandidate;
import com.remo.realestatemaintainceoptimizer.ai.AdvisorSelection;
import com.remo.realestatemaintainceoptimizer.ai.OptimizationAdvisor;
import com.remo.realestatemaintainceoptimizer.config.AiProperties;
import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationProposalResponse;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationRunResponse;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationRun;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationProposalRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationRunRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import com.remo.realestatemaintainceoptimizer.service.PlanningSnapshotFactory.AppointmentLabel;
import com.remo.realestatemaintainceoptimizer.service.PlanningSnapshotFactory.PlanningSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the AI appointment optimization of an account: the planner calculates every feasible, saving move inside the planning window, Claude selects which ones to propose, the planner re-validates the selection move by move and the result is stored as proposals awaiting the user's confirmation; the slow external calls (travel matrix, AI) run outside any database transaction.
 */
@Service
@EnableConfigurationProperties({OptimizationProperties.class, AiProperties.class, AppointmentSchedulingProperties.class})
public class OptimizationService {

    static final Duration ACCOUNT_LIMIT_WINDOW = Duration.ofHours(1);
    static final Duration DAILY_LIMIT_WINDOW = Duration.ofDays(1);
    private static final String ALL_ACCOUNTS_KEY = "all-accounts";
    private static final double METERS_PER_KILOMETER = 1000.0;
    private static final double SECONDS_PER_MINUTE = 60.0;

    private final OptimizationPlanner planner;
    private final PlanningSnapshotFactory snapshotFactory;
    private final DistanceMatrixService distanceMatrixService;
    private final OptimizationAdvisor advisor;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final OptimizationRunRepository runRepository;
    private final OptimizationProposalRepository proposalRepository;
    private final OptimizationProperties optimizationProperties;
    private final AppointmentSchedulingProperties schedulingProperties;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readOnlyTransaction;
    private final SlidingWindowRateLimiter runsPerAccount;
    private final SlidingWindowRateLimiter runsPerDay;

    public OptimizationService(
            OptimizationPlanner planner,
            PlanningSnapshotFactory snapshotFactory,
            DistanceMatrixService distanceMatrixService,
            OptimizationAdvisor advisor,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            OptimizationRunRepository runRepository,
            OptimizationProposalRepository proposalRepository,
            OptimizationProperties optimizationProperties,
            AiProperties aiProperties,
            AppointmentSchedulingProperties schedulingProperties,
            PlatformTransactionManager transactionManager) {
        this.planner = planner;
        this.snapshotFactory = snapshotFactory;
        this.distanceMatrixService = distanceMatrixService;
        this.advisor = advisor;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.runRepository = runRepository;
        this.proposalRepository = proposalRepository;
        this.optimizationProperties = optimizationProperties;
        this.schedulingProperties = schedulingProperties;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.runsPerAccount = new SlidingWindowRateLimiter(
                aiProperties.maxRunsPerAccountPerHour(), ACCOUNT_LIMIT_WINDOW, Clock.systemUTC());
        this.runsPerDay = new SlidingWindowRateLimiter(aiProperties.maxRunsPerDay(), DAILY_LIMIT_WINDOW, Clock.systemUTC());
    }

    /**
     * Optimizes the given account's appointments inside the planning window and stores the confirmed moves as pending proposals, replacing every proposal still pending from an earlier run; a demo account uses up its single run only once the AI was actually consulted and the result stored.
     */
    public OptimizationRunResponse run(String ownerId, Locale locale) {
        advisor.requireEnabled();
        acquireRunBudget(ownerId);
        LocalDate today = LocalDate.now(optimizationProperties.zone());

        PreparedRun prepared = readOnlyTransaction.execute(status -> prepare(ownerId, today));
        PlanningContext context = new PlanningContext(
                distanceMatrixService.matrix(prepared.snapshot().locations()), prepared.bufferMinutes(), today);
        List<MoveOption> options = planner.findMoveOptions(prepared.snapshot().visits(), context);
        List<AdvisorCandidate> candidates = toCandidates(options, prepared.snapshot().labels());
        boolean consultsAi = !candidates.isEmpty();
        List<AdvisorSelection> selections = consultsAi ? advisor.selectMoves(candidates, locale) : List.of();
        List<ConfirmedMove> confirmedMoves =
                confirmSequentially(prepared.snapshot().visits(), candidates, options, selections, context);

        return writeTransaction.execute(status -> persist(ownerId, prepared, candidates.size(), confirmedMoves, consultsAi));
    }

    private void acquireRunBudget(String ownerId) {
        if (!runsPerAccount.tryAcquire(ownerId)) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_OPTIMIZATIONS);
        }
        if (!runsPerDay.tryAcquire(ALL_ACCOUNTS_KEY)) {
            runsPerAccount.releaseLatest(ownerId);
            throw new RateLimitExceededException(RateLimitExceededException.REASON_OPTIMIZATION_QUOTA_EXHAUSTED);
        }
    }

    private PreparedRun prepare(String ownerId, LocalDate today) {
        User owner = loadOwner(ownerId);
        owner.requireAiOptimizationAvailable();
        List<Appointment> appointments = appointmentRepository.findAllByPropertyOwnerIdOrderByStartAsc(ownerId);
        PlanningSnapshot snapshot = snapshotFactory.snapshot(appointments, day -> planner.isInWindow(day, today));
        int analyzedAppointmentCount = (int) snapshot.visits().stream()
                .filter(visit -> planner.isPlannable(visit, today))
                .count();
        return new PreparedRun(
                snapshot, schedulingProperties.bufferMinutesFor(owner.appointmentBufferMinutes()), analyzedAppointmentCount);
    }

    private static List<AdvisorCandidate> toCandidates(List<MoveOption> options, Map<String, AppointmentLabel> labels) {
        return IntStream.range(0, options.size())
                .mapToObj(index -> {
                    MoveOption option = options.get(index);
                    AppointmentLabel label = labels.get(option.visit().appointmentId());
                    return new AdvisorCandidate(
                            "c" + (index + 1),
                            option.visit().appointmentId(),
                            label.title(),
                            label.propertyName(),
                            option.visit().recurring(),
                            option.visit().start(),
                            option.newStart(),
                            option.newEnd(),
                            roundToTenths(option.savedDistanceMeters() / METERS_PER_KILOMETER),
                            roundToTenths(option.savedDurationSeconds() / SECONDS_PER_MINUTE));
                })
                .toList();
    }

    private static double roundToTenths(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * Applies the AI's selections one after another to a working copy of the schedule, keeping only the first selection per appointment and only moves that are still feasible and saving once the previously kept moves are in place.
     */
    private List<ConfirmedMove> confirmSequentially(
            List<PlanningVisit> visits,
            List<AdvisorCandidate> candidates,
            List<MoveOption> options,
            List<AdvisorSelection> selections,
            PlanningContext context) {
        Map<String, Integer> optionIndexByCandidateId = IntStream.range(0, candidates.size())
                .boxed()
                .collect(Collectors.toMap(index -> candidates.get(index).candidateId(), Function.identity()));
        List<PlanningVisit> schedule = visits;
        List<ConfirmedMove> confirmedMoves = new ArrayList<>();
        Set<String> movedAppointmentIds = new HashSet<>();
        for (AdvisorSelection selection : selections) {
            MoveOption selected = options.get(optionIndexByCandidateId.get(selection.candidateId()));
            if (!movedAppointmentIds.add(selected.visit().appointmentId())) {
                continue;
            }
            List<PlanningVisit> currentSchedule = schedule;
            Optional<MoveOption> revalidated = currentSchedule.stream()
                    .filter(visit -> visit.appointmentId().equals(selected.visit().appointmentId()))
                    .findFirst()
                    .flatMap(visit -> planner.evaluateMove(currentSchedule, visit, selected.newStart(), context));
            if (revalidated.isPresent()) {
                confirmedMoves.add(new ConfirmedMove(revalidated.get(), selection.reason()));
                schedule = planner.applyMove(schedule, revalidated.get());
            }
        }
        return confirmedMoves;
    }

    private OptimizationRunResponse persist(
            String ownerId, PreparedRun prepared, int candidateCount, List<ConfirmedMove> confirmedMoves, boolean consultedAi) {
        userRepository.acquireTransactionLock(ownerId.hashCode());
        User owner = loadOwner(ownerId);
        if (consultedAi) {
            owner.consumeAiOptimization();
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        proposalRepository.findAllByOwnerIdAndStatusOrderByProposedStartAsc(ownerId, OptimizationProposalStatus.PENDING)
                .forEach(proposal -> proposal.expire(now));
        OptimizationRun run = runRepository.save(
                new OptimizationRun(UUID.randomUUID().toString(), ownerId, now, advisor.modelName(), candidateCount));

        List<OptimizationProposalResponse> proposals = new ArrayList<>();
        for (ConfirmedMove confirmedMove : confirmedMoves) {
            MoveOption move = confirmedMove.move();
            appointmentRepository.findByIdAndPropertyOwnerId(move.visit().appointmentId(), ownerId)
                    .filter(appointment -> appointment.start().equals(move.visit().start()))
                    .map(appointment -> proposalRepository.save(new OptimizationProposal(
                            UUID.randomUUID().toString(),
                            run.id(),
                            ownerId,
                            appointment,
                            move.newStart(),
                            move.newEnd(),
                            move.savedDistanceMeters(),
                            move.savedDurationSeconds(),
                            confirmedMove.reason(),
                            now)))
                    .map(OptimizationProposalResponse::from)
                    .ifPresent(proposals::add);
        }
        return new OptimizationRunResponse(run.id(), now, prepared.analyzedAppointmentCount(), candidateCount, proposals);
    }

    private User loadOwner(String ownerId) {
        return userRepository.findById(ownerId).orElseThrow(() -> new AccountNotFoundException(ownerId));
    }

    private record PreparedRun(PlanningSnapshot snapshot, int bufferMinutes, int analyzedAppointmentCount) {
    }

    private record ConfirmedMove(MoveOption move, String reason) {
    }
}
