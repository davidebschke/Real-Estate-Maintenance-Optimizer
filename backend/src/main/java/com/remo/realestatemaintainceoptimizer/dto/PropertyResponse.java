package com.remo.realestatemaintainceoptimizer.dto;

/**
 * A property as returned to API consumers, including the number of tenants living in its apartments.
 */
public record PropertyResponse(
        String id, String name, String address, String icon, Double latitude, Double longitude, long tenantCount) {
}
