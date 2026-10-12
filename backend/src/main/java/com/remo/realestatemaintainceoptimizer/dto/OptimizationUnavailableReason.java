package com.remo.realestatemaintainceoptimizer.dto;

/**
 * Why the AI appointment optimization cannot be started: the AI itself or the road routing its travel matrix needs is switched off.
 */
public enum OptimizationUnavailableReason {
    AI_DISABLED,
    ROUTING_DISABLED
}
