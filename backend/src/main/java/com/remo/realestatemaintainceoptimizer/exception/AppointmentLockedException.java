package com.remo.realestatemaintainceoptimizer.exception;

/**
 * Thrown when an attempt is made to reschedule an appointment marked as unverschiebbar (locked).
 */
public class AppointmentLockedException extends RuntimeException {

    public AppointmentLockedException(String appointmentId) {
        super("Appointment " + appointmentId + " is locked and cannot be rescheduled");
    }
}
