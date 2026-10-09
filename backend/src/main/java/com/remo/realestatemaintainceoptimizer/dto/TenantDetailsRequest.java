package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for the personal data of a tenant.
 */
public record TenantDetailsRequest(
        @NotBlank @Size(max = Tenant.NAME_MAX_LENGTH) String firstName,
        @NotBlank @Size(max = Tenant.NAME_MAX_LENGTH) String lastName) {
}
