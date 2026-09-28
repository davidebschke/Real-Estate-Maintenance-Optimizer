package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payload for creating a new appointment.
 */
public record CreateAppointmentRequest(
        @NotBlank @Size(max = 50) String title,
        @NotBlank String propertyId,
        @Size(max = 2000) String description,
        @NotNull LocalDateTime start,
        @NotNull @Positive Integer durationMinutes,
        boolean locked,
        boolean recurring,
        Integer recurrenceIntervalMonths,
        List<@NotBlank @Size(max = 255) String> materials) {
}
