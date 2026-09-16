package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when no property exists for a given id.
 */
public class PropertyNotFoundException extends RuntimeException {

    private final String propertyId;

    public PropertyNotFoundException(String propertyId) {
        super("No property exists with id " + propertyId);
        this.propertyId = propertyId;
    }

    public String propertyId() {
        return propertyId;
    }
}
