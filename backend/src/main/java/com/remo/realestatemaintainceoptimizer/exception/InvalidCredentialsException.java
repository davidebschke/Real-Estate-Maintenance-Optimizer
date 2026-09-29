package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a login attempt does not match any account's username and password.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
