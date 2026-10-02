package com.remo.realestatemaintainceoptimizer.security;

import java.nio.charset.StandardCharsets;

/**
 * The limits every password of this application has to respect, shared by the initial password, password changes and the request DTOs.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;
    public static final int MAX_INPUT_LENGTH = 128;

    private PasswordPolicy() {
    }

    /**
     * Returns whether the given password has fewer than {@value #MIN_LENGTH} characters.
     */
    public static boolean isTooShort(String password) {
        return password.length() < MIN_LENGTH;
    }

    /**
     * Returns whether the given password needs more than the {@value #MAX_BYTES} UTF-8 bytes BCrypt supports.
     */
    public static boolean isTooLong(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES;
    }
}
