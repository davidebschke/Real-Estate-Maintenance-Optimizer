package com.remo.realestatemaintainceoptimizer.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Verifies how the savings statistics group days into weeks starting on Monday and into calendar months.
 */
class SavingsGranularityTest {

    @Test
    void aWeekStartsOnTheMondayOfTheGivenDay() {
        assertThat(SavingsGranularity.WEEK.periodStart(LocalDate.of(2026, 10, 11))).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(SavingsGranularity.WEEK.periodStart(LocalDate.of(2026, 10, 5))).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(SavingsGranularity.WEEK.nextPeriodStart(LocalDate.of(2026, 10, 5))).isEqualTo(LocalDate.of(2026, 10, 12));
    }

    @Test
    void aMonthStartsOnItsFirstDay() {
        assertThat(SavingsGranularity.MONTH.periodStart(LocalDate.of(2026, 10, 31))).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(SavingsGranularity.MONTH.nextPeriodStart(LocalDate.of(2026, 12, 1))).isEqualTo(LocalDate.of(2027, 1, 1));
    }
}
