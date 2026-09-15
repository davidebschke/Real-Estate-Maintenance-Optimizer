package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when no appointment exists for a given id.
 */
public class AppointmentNotFoundException extends RuntimeException {

    private final String appointmentId;

    public AppointmentNotFoundException(String appointmentId) {
        super("No appointment exists with id " + appointmentId);
        this.appointmentId = appointmentId;
    }

    public String appointmentId() {
        return appointmentId;
    }
}
