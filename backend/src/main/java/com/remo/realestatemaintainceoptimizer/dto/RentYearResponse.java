package com.remo.realestatemaintainceoptimizer.dto;

import java.util.List;

/**
 * The yearly rent payment table of a property, one row per tenant, with the payments of former tenants and the month ({@code YYYY-MM}) the check started in, null while it is not activated.
 */
public record RentYearResponse(
        int year, String trackingStart, List<TenantRentYearResponse> tenants, List<FormerTenantPaymentResponse> formerTenantPayments) {
}
