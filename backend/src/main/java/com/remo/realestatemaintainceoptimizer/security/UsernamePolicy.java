package com.remo.realestatemaintainceoptimizer.security;

/**
 * The limits every username a user may choose has to respect, shared by the entity, the request DTOs and the demo account creation.
 */
public final class UsernamePolicy {

    public static final String DEMO_PREFIX = "demo-";
    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 50;
    public static final String PATTERN =
            "^(?!(?i)" + DEMO_PREFIX + ")[A-Za-z0-9._-]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$";

    private UsernamePolicy() {
    }
}
