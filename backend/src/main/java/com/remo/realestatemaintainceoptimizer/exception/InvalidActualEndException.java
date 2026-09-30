package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an appointment is completed with an actual end that lies before its planned start.
 */
public class InvalidActualEndException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidActualEndException(String appointmentId) {
        super("Actual end for appointment " + appointmentId + " lies before its planned start");
    }
}
