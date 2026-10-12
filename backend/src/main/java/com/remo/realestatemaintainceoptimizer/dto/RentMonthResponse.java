package com.remo.realestatemaintainceoptimizer.dto;

import java.math.BigDecimal;

/**
 * The payment state, the paid rent and the rent due for one calendar month.
 */
public record RentMonthResponse(int month, RentMonthStatus status, BigDecimal paidAmount, BigDecimal expectedAmount) {
}
