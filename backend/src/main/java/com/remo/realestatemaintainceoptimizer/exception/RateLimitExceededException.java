package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a client exceeds a rate limit, identified by a localizable reason code.
 */
public class RateLimitExceededException extends RuntimeException {

    public static final String REASON_TOO_MANY_LOGIN_ATTEMPTS = "tooManyLoginAttempts";
    public static final String REASON_TOO_MANY_DEMO_ACCOUNTS = "tooManyDemoAccounts";
    public static final String REASON_DEMO_CAPACITY_REACHED = "demoCapacityReached";

    private final String reasonCode;

    public RateLimitExceededException(String reasonCode) {
        super(reasonCode);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
