package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an account with a usage limit (a demo account) tries to create or use more of a resource than it may.
 */
public class CreationQuotaExceededException extends RuntimeException {

    public static final String RESOURCE_PROPERTY = "property";
    public static final String RESOURCE_APPOINTMENT = "appointment";
    public static final String RESOURCE_TENANT = "tenant";
    public static final String RESOURCE_AI_OPTIMIZATION = "aiOptimization";

    private final String resource;

    public CreationQuotaExceededException(String resource) {
        super("The creation limit for " + resource + " is exhausted");
        this.resource = resource;
    }

    public String resource() {
        return resource;
    }
}
