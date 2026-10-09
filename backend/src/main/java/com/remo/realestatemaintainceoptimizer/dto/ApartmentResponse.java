package com.remo.realestatemaintainceoptimizer.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * An apartment with its rent figures and its tenants as returned to API consumers.
 */
public record ApartmentResponse(
        String id,
        String propertyId,
        int floor,
        BigDecimal areaSquareMeters,
        BigDecimal totalRent,
        BigDecimal coldRent,
        BigDecimal additionalCosts,
        List<TenantResponse> tenants) {
}
