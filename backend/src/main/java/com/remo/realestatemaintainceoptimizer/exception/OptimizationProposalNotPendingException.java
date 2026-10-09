package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an optimization proposal that was already accepted, rejected or expired is decided again.
 */
public class OptimizationProposalNotPendingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OptimizationProposalNotPendingException(String proposalId) {
        super("The optimization proposal " + proposalId + " is no longer pending");
    }
}
