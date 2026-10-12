package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for creating an apartment, optionally together with its first tenant; without a tenant the apartment starts vacant.
 */
public record ApartmentWithTenantRequest(@NotNull @Valid ApartmentDetailsRequest apartment, @Valid TenantDetailsRequest tenant) {
}
