package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * Verifies that every month is judged by the rent due in it: the rent before the first later change, the current rent after the latest change, in any order of the recorded changes.
 */
class RentScheduleTest {

    private static final Apartment APARTMENT =
            new Apartment("apartment-1", null, 1, new BigDecimal("60"), new BigDecimal("1000"), new BigDecimal("800"), new BigDecimal("200"));

    private static ApartmentRentChange change(YearMonth month, String previousRent) {
        return new ApartmentRentChange("change-" + month, APARTMENT, month, new BigDecimal(previousRent));
    }

    @Test
    void anApartmentWithoutChangesAlwaysOwesItsCurrentRent() {
        RentSchedule schedule = RentSchedule.of(new BigDecimal("900"), List.of());

        assertThat(schedule.rentFor(YearMonth.of(2020, 1))).isEqualByComparingTo("900");
        assertThat(schedule.rentFor(YearMonth.of(2030, 12))).isEqualByComparingTo("900");
    }

    @Test
    void aRaiseOnlyAppliesFromItsOwnMonthOn() {
        RentSchedule schedule = RentSchedule.of(new BigDecimal("950"), List.of(change(YearMonth.of(2026, 3), "900")));

        assertThat(schedule.rentFor(YearMonth.of(2026, 2))).isEqualByComparingTo("900");
        assertThat(schedule.rentFor(YearMonth.of(2026, 3))).isEqualByComparingTo("950");
        assertThat(schedule.rentFor(YearMonth.of(2027, 1))).isEqualByComparingTo("950");
    }

    @Test
    void severalChangesApplyInTheirOwnMonthsWhateverTheOrderTheyWereGivenIn() {
        List<ApartmentRentChange> changes = List.of(change(YearMonth.of(2026, 8), "950"), change(YearMonth.of(2026, 3), "900"));
        RentSchedule schedule = RentSchedule.of(new BigDecimal("1000"), changes);

        assertThat(schedule.rentFor(YearMonth.of(2026, 2))).isEqualByComparingTo("900");
        assertThat(schedule.rentFor(YearMonth.of(2026, 3))).isEqualByComparingTo("950");
        assertThat(schedule.rentFor(YearMonth.of(2026, 7))).isEqualByComparingTo("950");
        assertThat(schedule.rentFor(YearMonth.of(2026, 8))).isEqualByComparingTo("1000");
    }

    @Test
    void aRaiseDoesNotTurnAlreadyPaidEarlierMonthsIntoAnAlert() {
        RentStatusCalculator calculator = new RentStatusCalculator(new PaymentProperties(
                5, "claude-haiku-5-5", 8192, DataSize.ofMegabytes(5), 10, 200, ZoneId.of("Europe/Berlin")));
        RentSchedule schedule = RentSchedule.of(new BigDecimal("950"), List.of(change(YearMonth.of(2026, 3), "900")));
        Map<YearMonth, BigDecimal> paid = Map.of(
                YearMonth.of(2026, 1), new BigDecimal("900"),
                YearMonth.of(2026, 2), new BigDecimal("900"),
                YearMonth.of(2026, 3), new BigDecimal("950"));

        assertThat(calculator.alertLevel(schedule, paid, YearMonth.of(2026, 1), LocalDate.of(2026, 3, 20)))
                .isEqualTo(RentAlertLevel.NONE);
    }
}
