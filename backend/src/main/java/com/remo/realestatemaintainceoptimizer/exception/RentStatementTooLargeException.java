package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an uploaded bank statement exceeds the configured maximum size.
 */
public class RentStatementTooLargeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long limitBytes;

    public RentStatementTooLargeException(long maxBytes) {
        super("The statement exceeds " + maxBytes + " bytes");
        this.limitBytes = maxBytes;
    }

    public long maxBytes() {
        return limitBytes;
    }
}
