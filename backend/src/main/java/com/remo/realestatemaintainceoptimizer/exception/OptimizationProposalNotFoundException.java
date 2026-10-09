package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when no optimization proposal with the given id exists for the requesting account.
 */
public class OptimizationProposalNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String missingProposalId;

    public OptimizationProposalNotFoundException(String proposalId) {
        super("No optimization proposal exists with id " + proposalId);
        this.missingProposalId = proposalId;
    }

    public String proposalId() {
        return missingProposalId;
    }
}
