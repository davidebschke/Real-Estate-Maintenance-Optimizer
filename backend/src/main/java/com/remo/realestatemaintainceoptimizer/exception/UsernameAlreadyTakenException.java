package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a username is requested that already belongs to another account.
 */
public class UsernameAlreadyTakenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UsernameAlreadyTakenException(String username) {
        super("Username already taken: " + username);
    }
}
