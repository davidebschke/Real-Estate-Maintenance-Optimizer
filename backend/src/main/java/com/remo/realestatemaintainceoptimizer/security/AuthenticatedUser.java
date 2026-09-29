package com.remo.realestatemaintainceoptimizer.security;

import com.remo.realestatemaintainceoptimizer.entity.User;

/**
 * The account behind an authenticated request, exposed to controllers via {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(String id, String username, String displayName, boolean demoAccount) {

    /**
     * Creates the principal for the given account.
     */
    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.id(), user.username(), user.displayName(), user.demoAccount());
    }
}
