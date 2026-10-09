package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a client exceeds a rate limit, identified by a localizable reason code.
 */
public class RateLimitExceededException extends RuntimeException {

    public static final String REASON_TOO_MANY_LOGIN_ATTEMPTS = "tooManyLoginAttempts";
    public static final String REASON_TOO_MANY_DEMO_ACCOUNTS = "tooManyDemoAccounts";
    public static final String REASON_DEMO_CAPACITY_REACHED = "demoCapacityReached";
    public static final String REASON_TOO_MANY_PASSWORD_CHANGE_ATTEMPTS = "tooManyPasswordChangeAttempts";
    public static final String REASON_TOO_MANY_USERNAME_CHANGES = "tooManyUsernameChanges";
    public static final String REASON_TOO_MANY_ROUTE_REQUESTS = "tooManyRouteRequests";
    public static final String REASON_ROUTE_QUOTA_EXHAUSTED = "routeQuotaExhausted";
    public static final String REASON_TOO_MANY_OPTIMIZATIONS = "tooManyOptimizations";
    public static final String REASON_OPTIMIZATION_QUOTA_EXHAUSTED = "optimizationQuotaExhausted";

    private final String reasonCode;

    public RateLimitExceededException(String reasonCode) {
        super(reasonCode);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
