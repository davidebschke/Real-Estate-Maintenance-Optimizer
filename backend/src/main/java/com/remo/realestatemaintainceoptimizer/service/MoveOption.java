package com.remo.realestatemaintainceoptimizer.service;

import java.time.LocalDateTime;

/**
 * A feasible move of one visit to a new start, with the road distance and driving time it saves on the affected days.
 */
public record MoveOption(
        PlanningVisit visit, LocalDateTime newStart, LocalDateTime newEnd, double savedDistanceMeters, double savedDurationSeconds) {

    /**
     * Returns the duration of the moved visit in whole minutes.
     */
    public int durationMinutes() {
        return (int) visit.duration().toMinutes();
    }
}
