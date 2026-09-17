package com.remo.realestatemaintainceoptimizer.dto;

/**
 * Coordinates resolved for a geocoded address, with null fields when no match was found.
 */
public record GeocodingResponse(Double latitude, Double longitude) {
}
