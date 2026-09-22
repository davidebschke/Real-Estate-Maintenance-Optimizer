package com.remo.realestatemaintainceoptimizer.dto;

import java.time.LocalDateTime;

/**
 * Payload for marking an appointment as completed with an explicit actual end time, falling back to the current time when {@code actualEnd} is omitted.
 */
public record CompleteAppointmentRequest(LocalDateTime actualEnd) {
}
