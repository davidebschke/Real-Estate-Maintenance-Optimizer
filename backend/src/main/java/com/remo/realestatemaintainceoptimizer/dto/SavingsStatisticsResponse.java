package com.remo.realestatemaintainceoptimizer.dto;

import java.util.List;

/**
 * Everything the AI optimization saved an account so far, in total and per period without gaps up to the current one.
 */
public record SavingsStatisticsResponse(
        SavingsGranularity granularity,
        double totalSavedDistanceMeters,
        double totalSavedDurationSeconds,
        int acceptedProposalCount,
        List<SavingsPeriodResponse> periods) {
}
