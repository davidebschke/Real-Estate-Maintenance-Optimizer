package com.remo.realestatemaintainceoptimizer.dto;

import java.time.LocalDate;

/**
 * Road distance and driving time saved by the optimization proposals accepted within one period.
 */
public record SavingsPeriodResponse(
        LocalDate periodStart, double savedDistanceMeters, double savedDurationSeconds, int acceptedProposalCount) {
}
