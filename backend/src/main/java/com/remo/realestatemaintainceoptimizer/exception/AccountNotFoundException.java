package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when the account behind an authenticated request no longer exists, e.g. an expired and deleted demo account.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String userId) {
        super("No account exists with id " + userId);
    }
}
