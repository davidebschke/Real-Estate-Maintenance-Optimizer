package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * An AI-suggested move of one appointment awaiting the user's confirmation, persisted as one row of the {@code optimization_proposals} table.
 */
@Entity
@Table(name = "optimization_proposals")
public class OptimizationProposal {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(name = "appointment_id", length = 36)
    private String appointmentId;

    @Column(name = "appointment_title", nullable = false, length = 50)
    private String appointmentTitle;

    @Column(name = "property_name", nullable = false, length = 50)
    private String propertyName;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "original_start", nullable = false)
    private LocalDateTime originalStart;

    @Column(name = "original_end", nullable = false)
    private LocalDateTime originalEnd;

    @Column(name = "proposed_start", nullable = false)
    private LocalDateTime proposedStart;

    @Column(name = "proposed_end", nullable = false)
    private LocalDateTime proposedEnd;

    @Column(name = "saved_distance_meters", nullable = false)
    private double savedDistanceMeters;

    @Column(name = "saved_duration_seconds", nullable = false)
    private double savedDurationSeconds;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OptimizationProposalStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    private Long version;

    protected OptimizationProposal() {
    }

    public OptimizationProposal(
            String id,
            String runId,
            String ownerId,
            Appointment appointment,
            LocalDateTime proposedStart,
            LocalDateTime proposedEnd,
            double savedDistanceMeters,
            double savedDurationSeconds,
            String reason,
            Instant createdAt) {
        this.id = id;
        this.runId = runId;
        this.ownerId = ownerId;
        this.appointmentId = appointment.id();
        this.appointmentTitle = appointment.title();
        this.propertyName = appointment.property().name();
        this.recurring = appointment.recurring();
        this.originalStart = appointment.start();
        this.originalEnd = appointment.end();
        this.proposedStart = proposedStart;
        this.proposedEnd = proposedEnd;
        this.savedDistanceMeters = savedDistanceMeters;
        this.savedDurationSeconds = savedDurationSeconds;
        this.reason = reason;
        this.status = OptimizationProposalStatus.PENDING;
        this.createdAt = createdAt;
    }

    /**
     * Returns whether this proposal still awaits the user's decision.
     */
    public boolean pending() {
        return status == OptimizationProposalStatus.PENDING;
    }

    /**
     * Marks this proposal as accepted with the savings recalculated at the moment of the decision.
     */
    public void accept(double confirmedSavedDistanceMeters, double confirmedSavedDurationSeconds, Instant decisionTime) {
        this.savedDistanceMeters = confirmedSavedDistanceMeters;
        this.savedDurationSeconds = confirmedSavedDurationSeconds;
        decide(OptimizationProposalStatus.ACCEPTED, decisionTime);
    }

    /**
     * Marks this proposal as rejected by the user.
     */
    public void reject(Instant decisionTime) {
        decide(OptimizationProposalStatus.REJECTED, decisionTime);
    }

    /**
     * Marks this proposal as no longer applicable, e.g. because a newer run superseded it or its appointment changed.
     */
    public void expire(Instant decisionTime) {
        decide(OptimizationProposalStatus.EXPIRED, decisionTime);
    }

    private void decide(OptimizationProposalStatus newStatus, Instant decisionTime) {
        this.status = newStatus;
        this.decidedAt = decisionTime;
    }

    public String id() {
        return id;
    }

    public String runId() {
        return runId;
    }

    public String ownerId() {
        return ownerId;
    }

    public String appointmentId() {
        return appointmentId;
    }

    public String appointmentTitle() {
        return appointmentTitle;
    }

    public String propertyName() {
        return propertyName;
    }

    public boolean recurring() {
        return recurring;
    }

    public LocalDateTime originalStart() {
        return originalStart;
    }

    public LocalDateTime originalEnd() {
        return originalEnd;
    }

    public LocalDateTime proposedStart() {
        return proposedStart;
    }

    public LocalDateTime proposedEnd() {
        return proposedEnd;
    }

    public double savedDistanceMeters() {
        return savedDistanceMeters;
    }

    public double savedDurationSeconds() {
        return savedDurationSeconds;
    }

    public String reason() {
        return reason;
    }

    public OptimizationProposalStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant decidedAt() {
        return decidedAt;
    }
}
