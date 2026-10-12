package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.PaymentProperties;
import com.remo.realestatemaintainceoptimizer.dto.RentAlertLevel;
import com.remo.realestatemaintainceoptimizer.dto.RentMonthStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Derives the payment state of an apartment's monthly rent from the amounts paid, without any I/O: a month counts as late once the configured due day of that month has passed, and months before the first recorded payment are not tracked.
 */
@Component
@EnableConfigurationProperties(PaymentProperties.class)
public class RentStatusCalculator {

    private final PaymentProperties properties;

    public RentStatusCalculator(PaymentProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the first month whose rent is tracked, which is the earliest month with a recorded payment or the current month while there is none.
     */
    public YearMonth trackingStart(Collection<YearMonth> paidMonths, LocalDate today) {
        YearMonth current = YearMonth.from(today);
        return paidMonths.stream().min(YearMonth::compareTo).filter(first -> first.isBefore(current)).orElse(current);
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
     * Returns the most urgent state over every tracked month up to the current one: overdue for a partial or overdue month, pending for a month still awaited, otherwise none.
     */
    public RentAlertLevel alertLevel(BigDecimal monthlyRent, Map<YearMonth, BigDecimal> paidByMonth, LocalDate today) {
        YearMonth trackingStart = trackingStart(paidByMonth.keySet(), today);
        YearMonth current = YearMonth.from(today);
        RentAlertLevel alert = RentAlertLevel.NONE;
        for (YearMonth month = trackingStart; !month.isAfter(current); month = month.plusMonths(1)) {
            RentMonthStatus status = monthStatus(
                    month, monthlyRent, paidByMonth.getOrDefault(month, BigDecimal.ZERO), trackingStart, today);
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
