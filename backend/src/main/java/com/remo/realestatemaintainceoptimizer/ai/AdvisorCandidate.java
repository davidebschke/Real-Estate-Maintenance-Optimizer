package com.remo.realestatemaintainceoptimizer.ai;

import java.time.LocalDateTime;

/**
 * One pre-validated appointment move the AI may select, with the savings the planner calculated for it.
 */
public record AdvisorCandidate(
        String candidateId,
        String appointmentId,
        String appointmentTitle,
        String propertyName,
        boolean recurring,
        LocalDateTime currentStart,
        LocalDateTime proposedStart,
        LocalDateTime proposedEnd,
        double savedKilometers,
        double savedDrivingMinutes) {
}
