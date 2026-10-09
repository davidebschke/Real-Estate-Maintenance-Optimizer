package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Payload combining the data of an apartment with the data of one tenant, used to create an apartment with its first tenant and to edit a tenant together with their apartment.
 */
public record ApartmentWithTenantRequest(
        @NotNull @Valid ApartmentDetailsRequest apartment, @NotNull @Valid TenantDetailsRequest tenant) {
}
