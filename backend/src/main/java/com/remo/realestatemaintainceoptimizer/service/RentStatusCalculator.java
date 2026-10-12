package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Derives the payment state of an apartment's monthly rent from the amounts paid, without any I/O: a month counts as late once the configured due day of that month has passed, and months before the month the rent payment check was activated in are not tracked.
 */
@Component
@EnableConfigurationProperties(PaymentProperties.class)
public class RentStatusCalculator {

    private final PaymentProperties properties;

    public RentStatusCalculator(PaymentProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the state of the given month's rent: untracked before the tracking start, paid once the full rent arrived, upcoming for a later month, otherwise partial or overdue after the due day and pending before it.
     */
    public RentMonthStatus monthStatus(
            YearMonth month, BigDecimal monthlyRent, BigDecimal paidAmount, YearMonth trackingStart, LocalDate today) {
        if (month.isBefore(trackingStart)) {
            return RentMonthStatus.UNTRACKED;
        }
        if (paidAmount.compareTo(monthlyRent) >= 0) {
            return RentMonthStatus.PAID;
        }
        if (month.isAfter(YearMonth.from(today))) {
            return RentMonthStatus.UPCOMING;
        }
        if (!today.isAfter(month.atDay(properties.dueDay()))) {
            return RentMonthStatus.PENDING;
        }
        return paidAmount.signum() > 0 ? RentMonthStatus.PARTIAL : RentMonthStatus.OVERDUE;
    }

    /**
     * Returns the most urgent state over every tracked month from the tracking start up to the current one, each judged by the rent due in that month: overdue for a partial or overdue month, pending for a month still awaited, otherwise none.
     */
    public RentAlertLevel alertLevel(
            RentSchedule schedule, Map<YearMonth, BigDecimal> paidByMonth, YearMonth trackingStart, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        RentAlertLevel alert = RentAlertLevel.NONE;
        for (YearMonth month = trackingStart; !month.isAfter(current); month = month.plusMonths(1)) {
            RentMonthStatus status = monthStatus(
                    month, schedule.rentFor(month), paidByMonth.getOrDefault(month, BigDecimal.ZERO), trackingStart, today);
            if (status == RentMonthStatus.OVERDUE || status == RentMonthStatus.PARTIAL) {
                return RentAlertLevel.OVERDUE;
            }
            if (status == RentMonthStatus.PENDING) {
                alert = RentAlertLevel.PENDING;
            }
        }
        return alert;
    }
}
