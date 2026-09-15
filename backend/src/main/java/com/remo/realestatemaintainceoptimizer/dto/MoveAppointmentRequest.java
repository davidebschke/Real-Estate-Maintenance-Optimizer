package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

/**
 * Payload for rescheduling an existing, unlocked appointment.
 */
public record MoveAppointmentRequest(@NotNull LocalDateTime start, @NotNull @Positive Integer durationMinutes) {
}
