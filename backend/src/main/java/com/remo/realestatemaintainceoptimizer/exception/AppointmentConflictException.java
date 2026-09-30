package com.remo.realestatemaintainceoptimizer.exception;

import java.time.LocalDateTime;

/**
 * Thrown when a new appointment's schedule overlaps with another, not-yet-deleted appointment of the same account, carrying the next free slot of the same duration as a suggestion.
 */
public class AppointmentConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient LocalDateTime nextFreeStart;
    private final transient LocalDateTime nextFreeEnd;

    public AppointmentConflictException(LocalDateTime suggestedStart, LocalDateTime suggestedEnd) {
        super("Requested schedule overlaps with an existing appointment of the same account");
        this.nextFreeStart = suggestedStart;
        this.nextFreeEnd = suggestedEnd;
    }

    public LocalDateTime suggestedStart() {
        return nextFreeStart;
    }

    public LocalDateTime suggestedEnd() {
        return nextFreeEnd;
    }
}
