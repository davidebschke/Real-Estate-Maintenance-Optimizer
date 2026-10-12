package com.remo.realestatemaintainceoptimizer.dto;

import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for one confirmed rent payment of a tenant.
 */
public record RentPaymentRequest(
        @NotBlank @Size(max = 36) String tenantId,
        @NotNull LocalDate paymentDate,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 2) BigDecimal amount,
        @NotNull @Size(max = RentPayment.REFERENCE_MAX_LENGTH) String reference,
        @NotNull @Min(RentPaymentRequest.FIRST_YEAR) @Max(RentPaymentRequest.LAST_YEAR) Integer periodYear,
        @NotNull @Min(1) @Max(12) Integer periodMonth) {

    public static final int FIRST_YEAR = 2000;
    public static final int LAST_YEAR = 2100;
}
