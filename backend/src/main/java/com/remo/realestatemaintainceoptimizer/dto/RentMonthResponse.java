package com.remo.realestatemaintainceoptimizer.dto;

import java.math.BigDecimal;

/**
 * The payment state and the paid rent of one calendar month.
 */
public record RentMonthResponse(int month, RentMonthStatus status, BigDecimal paidAmount) {
}
