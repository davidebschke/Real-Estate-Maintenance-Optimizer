package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * An AI optimization proposal as returned to the frontend: which appointment moves where, what it saves and why.
 */
public record OptimizationProposalResponse(
        String id,
        String appointmentId,
        String appointmentTitle,
        String propertyName,
        boolean recurring,
        LocalDateTime originalStart,
        LocalDateTime originalEnd,
        LocalDateTime proposedStart,
        LocalDateTime proposedEnd,
        double savedDistanceMeters,
        double savedDurationSeconds,
        String reason,
        OptimizationProposalStatus status,
        Instant decidedAt) {

    /**
     * Creates the response for the given proposal.
     */
    public static OptimizationProposalResponse from(OptimizationProposal proposal) {
        return new OptimizationProposalResponse(
                proposal.id(),
                proposal.appointmentId(),
                proposal.appointmentTitle(),
                proposal.propertyName(),
                proposal.recurring(),
                proposal.originalStart(),
                proposal.originalEnd(),
                proposal.proposedStart(),
                proposal.proposedEnd(),
                proposal.savedDistanceMeters(),
                proposal.savedDurationSeconds(),
                proposal.reason(),
                proposal.status(),
                proposal.decidedAt());
    }
}
