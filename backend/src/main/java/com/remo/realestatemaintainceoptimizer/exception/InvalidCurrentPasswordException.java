package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when the current password given for a password change does not match the account's password.
 */
public class InvalidCurrentPasswordException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
