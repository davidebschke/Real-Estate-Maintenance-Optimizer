package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a new property.
 */
public record CreatePropertyRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 200) String address,
        Double latitude,
        Double longitude) {
}
