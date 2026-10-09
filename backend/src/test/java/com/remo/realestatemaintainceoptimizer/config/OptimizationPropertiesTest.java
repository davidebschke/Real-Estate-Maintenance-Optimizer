package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Verifies that {@code remo.optimization.*} binds with its defaults (four weeks lead time, one year horizon, two weeks for recurring occurrences, 7 to 19 o'clock) and rejects inconsistent rules at startup.
 */
class OptimizationPropertiesTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static OptimizationProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bindOrCreate("remo.optimization", OptimizationProperties.class);
    }

    private static OptimizationProperties withWindow(int minLeadDays, int horizonDays) {
        return new OptimizationProperties(minLeadDays, horizonDays, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500,
                3, 60, 50, 400, BERLIN);
    }

    @Test
    void bindsTheDefaultsOfThePlanningRules() {
        OptimizationProperties properties = bind(Map.of());

        assertThat(properties.minLeadDays()).isEqualTo(28);
        assertThat(properties.horizonDays()).isEqualTo(365);
        assertThat(properties.maxRecurringShiftDays()).isEqualTo(14);
        assertThat(properties.workDayStart()).isEqualTo(LocalTime.of(7, 0));
        assertThat(properties.workDayEnd()).isEqualTo(LocalTime.of(19, 0));
        assertThat(properties.slotMinutes()).isEqualTo(15);
        assertThat(properties.minSavedDistanceMeters()).isEqualTo(500);
        assertThat(properties.maxCandidatesPerAppointment()).isEqualTo(3);
        assertThat(properties.maxCandidatesForAi()).isEqualTo(60);
        assertThat(properties.maxMatrixLocations()).isEqualTo(50);
        assertThat(properties.maxMatrixRequestsPerDay()).isEqualTo(400);
        assertThat(properties.zone()).isEqualTo(BERLIN);
    }

    @Test
    void bindsTheConfiguredValues() {
        OptimizationProperties properties = bind(Map.of(
                "remo.optimization.min-lead-days", "14",
                "remo.optimization.horizon-days", "180",
                "remo.optimization.max-recurring-shift-days", "7",
                "remo.optimization.work-day-start", "06:30",
                "remo.optimization.work-day-end", "17:00",
                "remo.optimization.zone", "UTC"));

        assertThat(properties.minLeadDays()).isEqualTo(14);
        assertThat(properties.horizonDays()).isEqualTo(180);
        assertThat(properties.maxRecurringShiftDays()).isEqualTo(7);
        assertThat(properties.workDayStart()).isEqualTo(LocalTime.of(6, 30));
        assertThat(properties.workDayEnd()).isEqualTo(LocalTime.of(17, 0));
        assertThat(properties.zone()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void rejectsAWindowWithoutLeadTimeOrWhoseHorizonIsNotAfterTheLeadTime() {
        assertThatThrownBy(() -> withWindow(0, 365)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withWindow(28, 28)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWorkingHoursThatEndBeforeTheyStart() {
        assertThatThrownBy(() -> new OptimizationProperties(28, 365, 14, LocalTime.of(19, 0), LocalTime.of(7, 0), 15, 500,
                3, 60, 50, 400, BERLIN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeRecurringShiftOrANonPositiveLimit() {
        assertThatThrownBy(() -> new OptimizationProperties(28, 365, -1, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500,
                3, 60, 50, 400, BERLIN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 0, 500,
                3, 60, 50, 400, BERLIN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500,
                0, 60, 50, 400, BERLIN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAMatrixLargerThanOpenrouteserviceAllows() {
        assertThatThrownBy(() -> new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500,
                3, 60, OptimizationProperties.MAX_ORS_MATRIX_LOCATIONS + 1, 400, BERLIN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500,
                3, 60, 1, 400, BERLIN)).isInstanceOf(IllegalArgumentException.class);
    }
}
