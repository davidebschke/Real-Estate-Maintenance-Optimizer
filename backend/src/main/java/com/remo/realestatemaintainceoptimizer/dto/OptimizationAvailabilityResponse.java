package com.remo.realestatemaintainceoptimizer.dto;

/**
 * Whether the AI appointment optimization can be started on this server and, if not, why.
 */
public record OptimizationAvailabilityResponse(boolean available, OptimizationUnavailableReason reason) {
}
