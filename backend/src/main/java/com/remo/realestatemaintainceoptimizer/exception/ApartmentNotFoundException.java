package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when no apartment exists for a given id.
 */
public class ApartmentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String missingId;

    public ApartmentNotFoundException(String apartmentId) {
        super("No apartment exists with id " + apartmentId);
        this.missingId = apartmentId;
    }

    public String apartmentId() {
        return missingId;
    }
}
