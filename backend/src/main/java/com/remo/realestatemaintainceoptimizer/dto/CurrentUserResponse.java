package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.entity.User;
import java.time.Instant;

/**
 * The logged-in account as returned to the frontend, including a demo account's expiry and remaining creation limits (null when unlimited).
 */
public record CurrentUserResponse(
        String username,
        String displayName,
        boolean demoAccount,
        Instant expiresAt,
        Integer remainingPropertyCreations,
        Integer remainingAppointmentCreations) {

    /**
     * Creates the response for the given account.
     */
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(
                user.username(),
                user.displayName(),
                user.demoAccount(),
                user.expiresAt(),
                user.remainingPropertyCreations(),
                user.remainingAppointmentCreations());
    }
}
