package com.remo.realestatemaintainceoptimizer.dto;

/**
 * How many of the confirmed payments were stored and how many were skipped because an identical payment already existed.
 */
public record SaveRentPaymentsResponse(int savedCount, int skippedCount) {
}
