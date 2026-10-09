package com.remo.realestatemaintainceoptimizer.config;

import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the rules of the automatic appointment optimization (planning window, recurring shift bound, working hours, candidate limits) under the {@code remo.optimization} prefix.
 */
@ConfigurationProperties(prefix = "remo.optimization")
public record OptimizationProperties(
        @DefaultValue("28") int minLeadDays,
        @DefaultValue("365") int horizonDays,
        @DefaultValue("14") int maxRecurringShiftDays,
        @DefaultValue("07:00") LocalTime workDayStart,
        @DefaultValue("19:00") LocalTime workDayEnd,
        @DefaultValue("15") int slotMinutes,
        @DefaultValue("500") int minSavedDistanceMeters,
        @DefaultValue("3") int maxCandidatesPerAppointment,
        @DefaultValue("60") int maxCandidatesForAi,
        @DefaultValue("50") int maxMatrixLocations,
        @DefaultValue("400") int maxMatrixRequestsPerDay,
        @DefaultValue("Europe/Berlin") ZoneId zone) {

    public static final int MAX_ORS_MATRIX_LOCATIONS = 59;

    public OptimizationProperties {
        if (minLeadDays < 1 || horizonDays <= minLeadDays) {
            throw new IllegalArgumentException(
                    "remo.optimization.min-lead-days must be at least 1 and smaller than remo.optimization.horizon-days");
        }
        if (maxRecurringShiftDays < 0) {
            throw new IllegalArgumentException("remo.optimization.max-recurring-shift-days must not be negative");
        }
        if (!workDayStart.isBefore(workDayEnd)) {
            throw new IllegalArgumentException("remo.optimization.work-day-start must lie before remo.optimization.work-day-end");
        }
        if (slotMinutes < 1 || minSavedDistanceMeters < 0 || maxCandidatesPerAppointment < 1 || maxCandidatesForAi < 1
                || maxMatrixRequestsPerDay < 1) {
            throw new IllegalArgumentException("remo.optimization limits must be positive");
        }
        if (maxMatrixLocations < 2 || maxMatrixLocations > MAX_ORS_MATRIX_LOCATIONS) {
            throw new IllegalArgumentException(
                    "remo.optimization.max-matrix-locations must be between 2 and " + MAX_ORS_MATRIX_LOCATIONS);
        }
    }
}
