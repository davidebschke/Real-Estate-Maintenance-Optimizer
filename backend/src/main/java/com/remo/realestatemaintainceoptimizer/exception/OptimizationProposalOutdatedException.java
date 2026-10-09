package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an optimization proposal is accepted although its appointment or schedule changed so that the move is no longer possible or no longer saves anything.
 */
public class OptimizationProposalOutdatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OptimizationProposalOutdatedException(String proposalId) {
        super("The optimization proposal " + proposalId + " is outdated");
    }
}
