package com.remo.realestatemaintainceoptimizer.dto;

/**
 * The month, as {@code YYYY-MM}, from which a property's rent payments are checked.
 */
public record RentTrackingResponse(String trackingStart) {
}
