package com.remo.realestatemaintainceoptimizer.dto;

import java.util.List;

/**
 * The yearly rent payment table of a property, one row per tenant.
 */
public record RentYearResponse(int year, List<TenantRentYearResponse> tenants) {
}
