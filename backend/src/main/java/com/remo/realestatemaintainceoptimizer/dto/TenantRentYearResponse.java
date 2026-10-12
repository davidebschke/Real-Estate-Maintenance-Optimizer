package com.remo.realestatemaintainceoptimizer.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * One tenant's rent state for the twelve months of a year, shared with the co-tenants of the same apartment.
 */
public record TenantRentYearResponse(
        String tenantId,
        String apartmentId,
        String firstName,
        String lastName,
        int floor,
        BigDecimal monthlyRent,
        RentAlertLevel alert,
        List<RentMonthResponse> months) {
}
