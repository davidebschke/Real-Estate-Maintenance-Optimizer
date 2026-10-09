package com.remo.realestatemaintainceoptimizer.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Length of one period of the savings statistics, i.e. one point of its line chart.
 */
public enum SavingsGranularity {
    WEEK,
    MONTH;

    /**
     * Returns the first day of the period containing the given day: its Monday for weeks, its first day for months.
     */
    public LocalDate periodStart(LocalDate day) {
        return this == WEEK ? day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : day.withDayOfMonth(1);
    }

    /**
     * Returns the first day of the period following the one starting at the given day.
     */
    public LocalDate nextPeriodStart(LocalDate periodStart) {
        return this == WEEK ? periodStart.plusWeeks(1) : periodStart.plusMonths(1);
    }
}
