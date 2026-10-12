package com.remo.realestatemaintainceoptimizer.dto;

/**
 * A property as returned to API consumers, including the number of its apartments, the number of tenants living in them and the month, as {@code YYYY-MM}, from which its rent payments are checked, or null while the rent payment check is not activated.
 */
public record PropertyResponse(
        String id, String name, String address, String icon, Double latitude, Double longitude, long apartmentCount,
        long tenantCount, String rentTrackingStart) {
}
