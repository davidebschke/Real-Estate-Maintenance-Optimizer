package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when a property already has the maximum number of apartments or an apartment the maximum number of tenants, identified by a localizable reason code and carrying the exceeded limit.
 */
public class TenantLimitExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String REASON_APARTMENT_LIMIT = "apartmentLimitReached";
    public static final String REASON_TENANT_LIMIT = "tenantLimitReached";

    private final String reason;
    private final int exceededLimit;

    public TenantLimitExceededException(String reasonCode, int limit) {
        super(reasonCode);
        this.reason = reasonCode;
        this.exceededLimit = limit;
    }

    public String reasonCode() {
        return reason;
    }

    public int limit() {
        return exceededLimit;
    }
}
