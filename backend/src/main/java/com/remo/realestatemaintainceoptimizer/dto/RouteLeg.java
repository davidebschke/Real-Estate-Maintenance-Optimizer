package com.remo.realestatemaintainceoptimizer.dto;

/**
 * Distance in meters and driving time in seconds between two consecutive stops of a route.
 */
public record RouteLeg(double distanceMeters, double durationSeconds) {
}
