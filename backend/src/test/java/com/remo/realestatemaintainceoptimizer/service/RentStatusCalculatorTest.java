package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/**
 * Verifies the month states (untracked, paid, upcoming, pending, partial, overdue) and the alert level of an apartment's rent without any I/O, with the rent due on the 5th and every month judged by the rent due in it.
 */
class RentStatusCalculatorTest {

    private static final BigDecimal RENT = new BigDecimal("850.00");
    private static final RentSchedule FLAT_RENT = RentSchedule.of(RENT, List.of());
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    private static final LocalDate AFTER_DUE_DAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate ON_DUE_DAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate BEFORE_DUE_DAY = LocalDate.of(2026, 10, 3);

    private final RentStatusCalculator calculator = new RentStatusCalculator(
            new PaymentProperties(5, "claude-haiku-5-5", 8192, DataSize.ofMegabytes(5), 10, 200, ZoneId.of("Europe/Berlin")));

    private RentMonthStatus status(YearMonth month, String paid, YearMonth trackingStart, LocalDate today) {
        return calculator.monthStatus(month, RENT, new BigDecimal(paid), trackingStart, today);
    }

    @Test
    void aMonthBeforeTheTrackingStartIsUntrackedEvenWithoutPayment() {
        assertThat(status(YearMonth.of(2026, 3), "0", YearMonth.of(2026, 5), AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.UNTRACKED);
        assertThat(status(YearMonth.of(2026, 3), "850.00", YearMonth.of(2026, 5), AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.UNTRACKED);
    }

    @Test
    void theMonthOfTheTrackingStartCountsEvenWhenItWasActivatedLateInIt() {
        assertThat(status(YearMonth.of(2026, 1), "0", YearMonth.of(2026, 1), LocalDate.of(2026, 1, 15))).isEqualTo(RentMonthStatus.OVERDUE);
    }

    @Test
    void aFullyPaidMonthIsPaidAlsoWhenPaidInAdvance() {
        assertThat(status(OCTOBER, "850.00", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.PAID);
        assertThat(status(YearMonth.of(2026, 12), "850.00", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.PAID);
        assertThat(status(OCTOBER, "900.00", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.PAID);
    }

    @Test
    void aLaterMonthThatIsNotPaidYetIsUpcoming() {
        assertThat(status(YearMonth.of(2026, 11), "0", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.UPCOMING);
        assertThat(status(YearMonth.of(2026, 11), "100.00", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.UPCOMING);
    }

    @Test
    void theCurrentMonthIsPendingUpToAndIncludingTheDueDay() {
        assertThat(status(OCTOBER, "0", OCTOBER, BEFORE_DUE_DAY)).isEqualTo(RentMonthStatus.PENDING);
        assertThat(status(OCTOBER, "0", OCTOBER, ON_DUE_DAY)).isEqualTo(RentMonthStatus.PENDING);
        assertThat(status(OCTOBER, "300.00", OCTOBER, ON_DUE_DAY)).isEqualTo(RentMonthStatus.PENDING);
    }

    @Test
    void anUnpaidMonthAfterTheDueDayIsOverdueAndAPartlyPaidOneIsPartial() {
        assertThat(status(OCTOBER, "0", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.OVERDUE);
        assertThat(status(OCTOBER, "300.00", OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentMonthStatus.PARTIAL);
        assertThat(status(YearMonth.of(2026, 8), "0", YearMonth.of(2026, 8), BEFORE_DUE_DAY)).isEqualTo(RentMonthStatus.OVERDUE);
    }

    @Test
    void anUnpaidCurrentMonthRaisesAnOverdueAlertAfterTheDueDayAndAPendingOneBefore() {
        assertThat(calculator.alertLevel(FLAT_RENT, Map.of(), OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.OVERDUE);
        assertThat(calculator.alertLevel(FLAT_RENT, Map.of(), OCTOBER, BEFORE_DUE_DAY)).isEqualTo(RentAlertLevel.PENDING);
    }

    @Test
    void aMissedMonthBetweenPaidMonthsKeepsTheAlertOverdue() {
        Map<YearMonth, BigDecimal> paid = Map.of(YearMonth.of(2026, 8), RENT, OCTOBER, RENT);

        assertThat(calculator.alertLevel(FLAT_RENT, paid, YearMonth.of(2026, 8), AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.OVERDUE);
    }

    @Test
    void aPartlyPaidMonthRaisesAnOverdueAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(YearMonth.of(2026, 9), RENT, OCTOBER, new BigDecimal("400.00"));

        assertThat(calculator.alertLevel(FLAT_RENT, paid, YearMonth.of(2026, 9), AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.OVERDUE);
    }

    @Test
    void everyTrackedMonthPaidInFullRaisesNoAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(YearMonth.of(2026, 8), RENT, YearMonth.of(2026, 9), RENT, OCTOBER, RENT);

        assertThat(calculator.alertLevel(FLAT_RENT, paid, YearMonth.of(2026, 8), AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.NONE);
    }

    @Test
    void unpaidMonthsBeforeTheTrackingStartRaiseNoAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(OCTOBER, RENT);

        assertThat(calculator.alertLevel(FLAT_RENT, paid, OCTOBER, AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.NONE);
    }

    @Test
    void aCurrentMonthStillBeforeTheDueDayOnlyRaisesAPendingAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(YearMonth.of(2026, 9), RENT);

        assertThat(calculator.alertLevel(FLAT_RENT, paid, YearMonth.of(2026, 9), BEFORE_DUE_DAY)).isEqualTo(RentAlertLevel.PENDING);
    }
}
