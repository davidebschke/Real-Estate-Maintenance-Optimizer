package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.config.AppointmentSchedulingProperties;
import com.remo.realestatemaintainceoptimizer.entity.User;
import java.time.Instant;

/**
 * The logged-in account as returned to the frontend, including a demo account's expiry, remaining creation limits (null when unlimited) and the minimum gap in minutes kept between appointments.
 */
public record CurrentUserResponse(
        String username,
        String displayName,
        boolean demoAccount,
        Instant expiresAt,
        Integer remainingPropertyCreations,
        Integer remainingAppointmentCreations,
        int appointmentBufferMinutes) {

    /**
     * Creates the response for the given account, resolving its buffer against the configured default.
     */
    public static CurrentUserResponse from(User user, AppointmentSchedulingProperties schedulingProperties) {
        return new CurrentUserResponse(
                user.username(),
                user.displayName(),
                user.demoAccount(),
                user.expiresAt(),
                user.remainingPropertyCreations(),
                user.remainingAppointmentCreations(),
                schedulingProperties.bufferMinutesFor(user.appointmentBufferMinutes()));
    }
}
