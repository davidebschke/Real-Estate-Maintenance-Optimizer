package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when rent payments are scanned or stored for a property whose rent payment check was not activated yet.
 */
public class RentTrackingNotActiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RentTrackingNotActiveException(String propertyId) {
        super("The rent payment check is not activated for property " + propertyId);
    }
}
