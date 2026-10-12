package com.remo.realestatemaintainceoptimizer.dto;

/**
 * A tenant whose rent is overdue or still awaited, with the property and apartment to mark.
 */
public record RentAlertResponse(String propertyId, String apartmentId, String tenantId, RentAlertLevel alert) {
}
