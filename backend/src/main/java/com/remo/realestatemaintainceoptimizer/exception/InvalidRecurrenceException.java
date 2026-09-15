package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a recurrence-related request is invalid, identified by a localizable reason code.
 */
public class InvalidRecurrenceException extends RuntimeException {

    private final String reasonCode;

    public InvalidRecurrenceException(String reasonCode) {
        super(reasonCode);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
