package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The monthly rent of one apartment over time: the rent it had before its first recorded change, and from each recorded change's month on the rent that change set.
 */
public record RentSchedule(BigDecimal rentBeforeChanges, List<ApartmentRentChange> changesOldestFirst) {

    /**
     * Creates the schedule of an apartment with the given rent before its first change from its recorded rent changes in any order.
     */
    public static RentSchedule of(BigDecimal rentBeforeChanges, Collection<ApartmentRentChange> changes) {
        return new RentSchedule(
                rentBeforeChanges, changes.stream().sorted(Comparator.comparing(ApartmentRentChange::effectiveMonth)).toList());
    }

    /**
     * Returns the rent due for the given month, which is the rent of the latest change effective in or before that month, or the rent before the first change if there is none.
     */
    public BigDecimal rentFor(YearMonth month) {
        return changesOldestFirst.reversed().stream()
                .filter(change -> !change.effectiveMonth().isAfter(month))
                .findFirst()
                .map(ApartmentRentChange::rent)
                .orElse(rentBeforeChanges);
    }
}
