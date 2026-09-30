package com.remo.realestatemaintainceoptimizer.dto;

import java.time.LocalDateTime;

/**
 * Error response for an appointment creation blocked by an overlapping existing appointment, additionally carrying the next free slot of the same account and duration.
 */
public record AppointmentConflictResponse(String message, LocalDateTime suggestedStart, LocalDateTime suggestedEnd) {
}
