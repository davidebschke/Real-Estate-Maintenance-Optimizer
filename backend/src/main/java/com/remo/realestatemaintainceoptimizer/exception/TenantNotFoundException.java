package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when no tenant exists for a given id.
 */
public class TenantNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String missingId;

    public TenantNotFoundException(String tenantId) {
        super("No tenant exists with id " + tenantId);
        this.missingId = tenantId;
    }

    public String tenantId() {
        return missingId;
    }
}
