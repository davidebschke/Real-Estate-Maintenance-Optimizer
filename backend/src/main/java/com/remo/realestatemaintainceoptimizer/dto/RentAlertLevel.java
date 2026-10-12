package com.remo.realestatemaintainceoptimizer.dto;

/**
 * How urgently a tenant's rent payments need attention, from none over a payment still awaited to an overdue one.
 */
public enum RentAlertLevel {
    NONE,
    PENDING,
    OVERDUE
}
