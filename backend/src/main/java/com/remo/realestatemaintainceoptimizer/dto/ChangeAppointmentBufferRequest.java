package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for changing the minimum gap in minutes kept between two appointments of the account.
 */
public record ChangeAppointmentBufferRequest(
        @NotNull @Min(0) @Max(AppointmentSchedulingProperties.MAX_BUFFER_MINUTES) Integer appointmentBufferMinutes) {
}
