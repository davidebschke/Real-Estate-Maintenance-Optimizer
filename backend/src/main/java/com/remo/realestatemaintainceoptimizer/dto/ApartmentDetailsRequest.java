package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload for the floor, area and rent figures of an apartment.
 */
public record ApartmentDetailsRequest(
        @NotNull @Min(-3) @Max(100) Integer floor,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 5, fraction = 2) BigDecimal areaSquareMeters,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 2) BigDecimal totalRent,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 2) BigDecimal coldRent,
        @NotNull @DecimalMin("0") @Digits(integer = 7, fraction = 2) BigDecimal additionalCosts) {
}
