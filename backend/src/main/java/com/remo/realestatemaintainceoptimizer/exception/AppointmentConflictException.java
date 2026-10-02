package com.remo.realestatemaintainceoptimizer.exception;

import java.time.LocalDateTime;

/**
 * Thrown when a created or moved appointment's schedule overlaps with, or comes closer than the configured buffer to, another not-yet-deleted appointment of the same account, carrying the next free slot of the same duration as a suggestion.
 */
public class AppointmentConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient LocalDateTime nextFreeStart;
    private final transient LocalDateTime nextFreeEnd;

    public AppointmentConflictException(LocalDateTime suggestedStart, LocalDateTime suggestedEnd) {
        super("Requested schedule overlaps with or is too close to an existing appointment of the same account");
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
