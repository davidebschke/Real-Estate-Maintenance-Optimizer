package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationProposalResponse;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalNotPendingException;
import com.remo.realestatemaintainceoptimizer.exception.OptimizationProposalOutdatedException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationProposalRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import com.remo.realestatemaintainceoptimizer.service.PlanningSnapshotFactory.PlanningSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lists the pending AI optimization proposals of an account and applies the user's decision on one of them, re-validating an accepted move against the current schedule first.
 */
@Service
@Transactional
@EnableConfigurationProperties({OptimizationProperties.class, AppointmentSchedulingProperties.class})
public class OptimizationProposalService {

    private final OptimizationProposalRepository proposalRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentService appointmentService;
    private final OptimizationPlanner planner;
    private final PlanningSnapshotFactory snapshotFactory;
    private final DistanceMatrixService distanceMatrixService;
    private final OptimizationProperties optimizationProperties;
    private final AppointmentSchedulingProperties schedulingProperties;
    private final TransactionTemplate writeTransaction;
    private final TransactionTemplate readOnlyTransaction;

    public OptimizationProposalService(
            OptimizationProposalRepository proposalRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AppointmentService appointmentService,
            OptimizationPlanner planner,
            PlanningSnapshotFactory snapshotFactory,
            DistanceMatrixService distanceMatrixService,
            OptimizationProperties optimizationProperties,
            AppointmentSchedulingProperties schedulingProperties,
            PlatformTransactionManager transactionManager) {
        this.proposalRepository = proposalRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.appointmentService = appointmentService;
        this.planner = planner;
        this.snapshotFactory = snapshotFactory;
        this.distanceMatrixService = distanceMatrixService;
        this.optimizationProperties = optimizationProperties;
        this.schedulingProperties = schedulingProperties;
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    /**
     * Returns every proposal of the given account that still awaits a decision and whose appointment still exists, sorted by proposed start.
     */
    @Transactional(readOnly = true)
    public List<OptimizationProposalResponse> listPending(String ownerId) {
        return proposalRepository.findAllByOwnerIdAndStatusOrderByProposedStartAsc(ownerId, OptimizationProposalStatus.PENDING)
                .stream()
                .filter(proposal -> proposal.appointmentId() != null)
                .map(OptimizationProposalResponse::from)
                .toList();
    }

    /**
     * Moves the proposal's appointment to the proposed slot once the move is confirmed to be still feasible and saving with the current schedule, storing the recalculated savings; an outdated proposal is marked expired before {@link OptimizationProposalOutdatedException} is thrown and its appointment stays untouched, and the travel matrix is fetched before the account lock is taken so no lock or connection waits for openrouteservice.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OptimizationProposalResponse accept(String ownerId, String proposalId) {
        List<Location> locations = readOnlyTransaction.execute(
                status -> affectedDaysSnapshot(ownerId, loadPending(ownerId, proposalId)).locations());
        TravelMatrix matrix = distanceMatrixService.matrix(locations);
        Optional<OptimizationProposalResponse> accepted =
                writeTransaction.execute(status -> applyIfStillValid(ownerId, proposalId, matrix));
        return accepted.orElseThrow(() -> new OptimizationProposalOutdatedException(proposalId));
    }

    private Optional<OptimizationProposalResponse> applyIfStillValid(String ownerId, String proposalId, TravelMatrix matrix) {
        userRepository.acquireTransactionLock(ownerId.hashCode());
        OptimizationProposal proposal = loadPending(ownerId, proposalId);
        Instant now = currentInstant();
        Optional<MoveOption> confirmedMove = revalidate(ownerId, proposal, matrix);
        if (confirmedMove.isEmpty()) {
            proposal.expire(now);
            return Optional.empty();
        }

        MoveOption move = confirmedMove.get();
        appointmentService.applyOptimizedSchedule(
                ownerId, proposal.appointmentId(), new MoveAppointmentRequest(move.newStart(), move.durationMinutes()));
        proposal.accept(move.savedDistanceMeters(), move.savedDurationSeconds(), now);
        return Optional.of(OptimizationProposalResponse.from(proposal));
    }

    /**
     * Marks a pending proposal as rejected, leaving its appointment untouched.
     */
    public OptimizationProposalResponse reject(String ownerId, String proposalId) {
        OptimizationProposal proposal = loadPending(ownerId, proposalId);
        proposal.reject(currentInstant());
        return OptimizationProposalResponse.from(proposal);
    }

    /**
     * Returns the planning view of the account's appointments locating the properties visited on the proposal's original and proposed day.
     */
    private PlanningSnapshot affectedDaysSnapshot(String ownerId, OptimizationProposal proposal) {
        Set<LocalDate> affectedDays =
                new HashSet<>(List.of(proposal.originalStart().toLocalDate(), proposal.proposedStart().toLocalDate()));
        return snapshotFactory.snapshot(appointmentRepository.findAllByPropertyOwnerIdOrderByStartAsc(ownerId), affectedDays::contains);
    }

    /**
     * Returns the proposal's move recalculated against the account's current schedule with the given travel matrix, empty when the appointment is gone, was changed since the proposal was made, or the move is no longer feasible or saving.
     */
    private Optional<MoveOption> revalidate(String ownerId, OptimizationProposal proposal, TravelMatrix matrix) {
        if (proposal.appointmentId() == null) {
            return Optional.empty();
        }
        Optional<Appointment> appointment = appointmentRepository.findByIdAndPropertyOwnerId(proposal.appointmentId(), ownerId);
        if (appointment.isEmpty()
                || !appointment.get().start().equals(proposal.originalStart())
                || !appointment.get().end().equals(proposal.originalEnd())) {
            return Optional.empty();
        }

        User owner = userRepository.findById(ownerId).orElseThrow(() -> new AccountNotFoundException(ownerId));
        PlanningSnapshot snapshot = affectedDaysSnapshot(ownerId, proposal);
        PlanningContext context = new PlanningContext(
                matrix,
                schedulingProperties.bufferMinutesFor(owner.appointmentBufferMinutes()),
                LocalDate.now(optimizationProperties.zone()));
        return snapshot.visits().stream()
                .filter(visit -> visit.appointmentId().equals(proposal.appointmentId()))
                .findFirst()
                .flatMap(visit -> planner.evaluateMove(snapshot.visits(), visit, proposal.proposedStart(), context));
    }

    private OptimizationProposal loadPending(String ownerId, String proposalId) {
        OptimizationProposal proposal = proposalRepository.findByIdAndOwnerId(proposalId, ownerId)
                .orElseThrow(() -> new OptimizationProposalNotFoundException(proposalId));
        if (!proposal.pending()) {
            throw new OptimizationProposalNotPendingException(proposalId);
        }
        return proposal;
    }

    private static Instant currentInstant() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
