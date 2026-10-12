package com.remo.realestatemaintainceoptimizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A payment noted as made by a tenant who was deleted since, named as {@code lastName, firstName}.
 */
public record FormerTenantPaymentResponse(
        String apartmentId,
        int floor,
        String formerTenantName,
        LocalDate paymentDate,
        BigDecimal amount,
        String reference,
        int periodYear,
        int periodMonth) {
}
