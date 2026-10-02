package com.remo.realestatemaintainceoptimizer.dto;

import java.time.LocalDateTime;

/**
 * Error response for an appointment creation or move blocked by an overlapping or too closely adjacent existing appointment, additionally carrying the next free slot of the same account and duration.
 */
public record AppointmentConflictResponse(String message, LocalDateTime suggestedStart, LocalDateTime suggestedEnd) {
}
