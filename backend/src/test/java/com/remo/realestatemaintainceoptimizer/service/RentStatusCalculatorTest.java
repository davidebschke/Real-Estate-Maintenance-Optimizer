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
 * Verifies the month states (untracked, paid, upcoming, pending, partial, overdue), the tracking start and the alert level of an apartment's rent without any I/O, with the rent due on the 5th.
 */
class RentStatusCalculatorTest {

    private static final BigDecimal RENT = new BigDecimal("850.00");
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
    void trackingStartsAtTheEarliestPaidMonthOrTheCurrentMonthWithoutPayments() {
        assertThat(calculator.trackingStart(List.of(YearMonth.of(2026, 6), YearMonth.of(2026, 3)), AFTER_DUE_DAY))
                .isEqualTo(YearMonth.of(2026, 3));
        assertThat(calculator.trackingStart(List.of(), AFTER_DUE_DAY)).isEqualTo(OCTOBER);
        assertThat(calculator.trackingStart(List.of(YearMonth.of(2026, 12)), AFTER_DUE_DAY)).isEqualTo(OCTOBER);
    }

    @Test
    void noPaymentsAtAllRaisesAnOverdueAlertOnlyForTheCurrentMonthAfterTheDueDay() {
        assertThat(calculator.alertLevel(RENT, Map.of(), AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.OVERDUE);
        assertThat(calculator.alertLevel(RENT, Map.of(), BEFORE_DUE_DAY)).isEqualTo(RentAlertLevel.PENDING);
    }

    @Test
    void aMissedMonthBetweenPaidMonthsKeepsTheAlertOverdue() {
        Map<YearMonth, BigDecimal> paid = Map.of(
                YearMonth.of(2026, 8), RENT,
                OCTOBER, RENT);

        assertThat(calculator.alertLevel(RENT, paid, AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.OVERDUE);
    }

    @Test
    void aPartlyPaidMonthRaisesAnOverdueAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(
                YearMonth.of(2026, 9), RENT,
                OCTOBER, new BigDecimal("400.00"));

        assertThat(calculator.alertLevel(RENT, paid, AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.OVERDUE);
    }

    @Test
    void everyMonthPaidInFullRaisesNoAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(
                YearMonth.of(2026, 8), RENT,
                YearMonth.of(2026, 9), RENT,
                OCTOBER, RENT);

        assertThat(calculator.alertLevel(RENT, paid, AFTER_DUE_DAY)).isEqualTo(RentAlertLevel.NONE);
    }

    @Test
    void aCurrentMonthStillBeforeTheDueDayOnlyRaisesAPendingAlert() {
        Map<YearMonth, BigDecimal> paid = Map.of(YearMonth.of(2026, 9), RENT);

        assertThat(calculator.alertLevel(RENT, paid, BEFORE_DUE_DAY)).isEqualTo(RentAlertLevel.PENDING);
    }
}
