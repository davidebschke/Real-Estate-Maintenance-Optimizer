package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * An appointment as seen by the optimization planner: where it takes place (null when its location is unknown to the travel matrix), when it occupies the schedule and whether the planner may move it.
 */
public record PlanningVisit(
        String appointmentId,
        String locationId,
        LocalDateTime start,
        LocalDateTime end,
        boolean movable,
        boolean recurring,
        LocalDateTime recurrenceAnchor) {

    /**
     * Creates the planning view of the given appointment, located only when its property is one of the given matrix locations and movable only when it is neither locked nor completed.
     */
    public static PlanningVisit from(Appointment appointment, Set<String> locatedPropertyIds) {
        String propertyId = appointment.property().id();
        String locationId = locatedPropertyIds.contains(propertyId) ? propertyId : null;
        LocalDateTime occupiedEnd = appointment.completed() ? appointment.actualEnd() : appointment.end();
        return new PlanningVisit(
                appointment.id(),
                locationId,
                appointment.start(),
                occupiedEnd,
                !appointment.locked() && !appointment.completed(),
                appointment.recurring(),
                appointment.recurrenceAnchor());
    }

    /**
     * Returns whether the travel matrix knows this visit's location.
     */
    public boolean located() {
        return locationId != null;
    }

    /**
     * Returns the calendar day this visit starts on.
     */
    public LocalDate date() {
        return start.toLocalDate();
    }

    /**
     * Returns how long this visit occupies the schedule.
     */
    public Duration duration() {
        return Duration.between(start, end);
    }

    /**
     * Returns this visit moved to the given start, keeping its duration.
     */
    public PlanningVisit movedTo(LocalDateTime newStart) {
        return new PlanningVisit(
                appointmentId, locationId, newStart, newStart.plus(duration()), movable, recurring, recurrenceAnchor);
    }
}
