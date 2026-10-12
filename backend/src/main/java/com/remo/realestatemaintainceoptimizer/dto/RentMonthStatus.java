package com.remo.realestatemaintainceoptimizer.dto;

/**
 * The payment state of one calendar month of an apartment's rent.
 */
public enum RentMonthStatus {
    PAID,
    PARTIAL,
    PENDING,
    OVERDUE,
    UPCOMING,
    UNTRACKED
}
