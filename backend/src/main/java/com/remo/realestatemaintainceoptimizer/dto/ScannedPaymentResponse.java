package com.remo.realestatemaintainceoptimizer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One payment the AI found on a scanned bank statement, with the tenant it was assigned to, as returned to API consumers.
 */
public record ScannedPaymentResponse(
        LocalDate paymentDate,
        BigDecimal amount,
        String payerName,
        String reference,
        int periodYear,
        int periodMonth,
        String tenantId,
        TenantMatch match,
        boolean alreadyRecorded) {
}
