package com.remo.realestatemaintainceoptimizer.dto;

import java.time.Instant;
import java.util.List;

/**
 * The outcome of one AI optimization run: how many appointments the planner could analyze, how many feasible moves it offered the AI and the proposals awaiting confirmation.
 */
public record OptimizationRunResponse(
        String runId,
        Instant createdAt,
        int analyzedAppointmentCount,
        int candidateCount,
        List<OptimizationProposalResponse> proposals) {
}
