package com.remo.realestatemaintainceoptimizer;

import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import com.remo.realestatemaintainceoptimizer.service.TravelMatrix;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

/**
 * Shared data for the optimization tests: two properties ten kilometres and fifteen driving minutes apart and working days inside the planning window.
 */
public final class OptimizationFixtures {

    public static final double WEST_EAST_METERS = 10_000.0;
    public static final double WEST_EAST_SECONDS = 900.0;

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;
    private static final double ROAD_DETOUR_FACTOR = 1.3;
    private static final double CITY_SPEED_METERS_PER_SECOND = 30_000.0 / 3_600.0;

    private OptimizationFixtures() {
    }

    /**
     * Returns the first Tuesday at least five weeks from today in the optimization's time zone, so it and the following days lie inside the planning window.
     */
    public static LocalDate firstPlannableTuesday() {
        return LocalDate.now(ZoneId.of("Europe/Berlin")).plusDays(35).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
    }

    /**
     * Returns a property in the west of Cologne owned by the given account.
     */
    public static Property westProperty(String ownerId) {
        return new Property(UUID.randomUUID().toString(), ownerId, "Wohnanlage West", "Aachener Str. 512, 50933 Köln",
                "pi-building", 50.937634, 6.8922212);
    }

    /**
     * Returns a property in the east of Cologne owned by the given account.
     */
    public static Property eastProperty(String ownerId) {
        return new Property(UUID.randomUUID().toString(), ownerId, "Rheinhaus Ost", "Deutzer Freiheit 70, 50679 Köln",
                "pi-building", 50.936150, 6.974960);
    }

    /**
     * Returns a one-hour appointment at the given property, locked or movable.
     */
    public static Appointment appointment(Property property, LocalDateTime start, boolean locked) {
        return appointment(property, start, locked, false);
    }

    /**
     * Returns a one-hour appointment at the given property, locked or movable and optionally part of a monthly series.
     */
    public static Appointment appointment(Property property, LocalDateTime start, boolean locked, boolean recurring) {
        return new Appointment(
                UUID.randomUUID().toString(),
                recurring ? UUID.randomUUID().toString() : null,
                "Heizungswartung",
                property,
                "",
                start,
                start.plusMinutes(60),
                locked,
                recurring,
                recurring ? 1 : null,
                List.of(),
                List.of(new HistoryEntry(Instant.now(), HistoryEventType.CREATED, List.of())),
                null);
    }

    /**
     * Returns a travel matrix estimating road travel from the straight-line distance between the given locations, longer by a typical detour factor and driven at city speed.
     */
    public static TravelMatrix straightLineMatrixFor(List<Location> locations) {
        List<String> ids = locations.stream().map(Location::id).toList();
        List<List<Double>> distances = locations.stream()
                .map(from -> locations.stream().map(to -> ROAD_DETOUR_FACTOR * straightLineMeters(from, to)).toList())
                .toList();
        List<List<Double>> durations = distances.stream()
                .map(row -> row.stream().map(meters -> meters / CITY_SPEED_METERS_PER_SECOND).toList())
                .toList();
        return new TravelMatrix(ids, distances, durations);
    }

    private static double straightLineMeters(Location from, Location to) {
        double latitudeDelta = Math.toRadians(to.latitude() - from.latitude());
        double longitudeDelta = Math.toRadians(to.longitude() - from.longitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(from.latitude())) * Math.cos(Math.toRadians(to.latitude()))
                        * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine));
    }

    /**
     * Returns the travel matrix between the given locations with every two different locations ten kilometres and fifteen minutes apart.
     */
    public static TravelMatrix matrixFor(List<Location> locations) {
        List<String> ids = locations.stream().map(Location::id).toList();
        List<List<Double>> distances = ids.stream()
                .map(from -> ids.stream().map(to -> from.equals(to) ? 0.0 : WEST_EAST_METERS).toList())
                .toList();
        List<List<Double>> durations = ids.stream()
                .map(from -> ids.stream().map(to -> from.equals(to) ? 0.0 : WEST_EAST_SECONDS).toList())
                .toList();
        return new TravelMatrix(ids, distances, durations);
    }
}
