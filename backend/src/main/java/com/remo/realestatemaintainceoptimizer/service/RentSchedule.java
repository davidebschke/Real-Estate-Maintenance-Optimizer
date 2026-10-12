package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The monthly rent of one apartment over time: its current rent from the month of its latest change on, and before that the rent each change replaced.
 */
public record RentSchedule(BigDecimal currentRent, List<ApartmentRentChange> changesOldestFirst) {

    /**
     * Creates the schedule of an apartment with the given current rent from its recorded rent changes in any order.
     */
    public static RentSchedule of(BigDecimal currentRent, Collection<ApartmentRentChange> changes) {
        return new RentSchedule(
                currentRent, changes.stream().sorted(Comparator.comparing(ApartmentRentChange::effectiveMonth)).toList());
    }

    /**
     * Returns the rent due for the given month, which is the rent before the first change taking effect after that month, or the current rent if there is none.
     */
    public BigDecimal rentFor(YearMonth month) {
        return changesOldestFirst.stream()
                .filter(change -> change.effectiveMonth().isAfter(month))
                .findFirst()
                .map(ApartmentRentChange::previousRent)
                .orElse(currentRent);
    }
}
