package com.remo.realestatemaintainceoptimizer.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Road distances and driving times between every pair of a set of locations (property ids), as calculated once per optimization by the distance matrix service.
 */
public final class TravelMatrix {

    private final Map<String, Integer> indexByLocationId;
    private final List<List<Double>> distancesMeters;
    private final List<List<Double>> durationsSeconds;

    /**
     * Creates a matrix whose row and column {@code i} belong to {@code locationIds.get(i)} (ids must be distinct); a {@code null} entry marks an unreachable pair.
     */
    public TravelMatrix(List<String> locationIds, List<List<Double>> distancesMeters, List<List<Double>> durationsSeconds) {
        this.indexByLocationId = IntStream.range(0, locationIds.size())
                .boxed()
                .collect(Collectors.toUnmodifiableMap(locationIds::get, Function.identity()));
        this.distancesMeters = List.copyOf(distancesMeters.stream().map(TravelMatrix::copyRow).toList());
        this.durationsSeconds = List.copyOf(durationsSeconds.stream().map(TravelMatrix::copyRow).toList());
    }

    /**
     * Returns a matrix of locations that are all the same place, e.g. for a single location where no road travel is needed.
     */
    public static TravelMatrix withoutTravel(List<String> locationIds) {
        List<List<Double>> zeros = locationIds.stream()
                .map(ignored -> locationIds.stream().map(other -> 0.0).toList())
                .toList();
        return new TravelMatrix(locationIds, zeros, zeros);
    }

    /**
     * Returns whether the given location is part of this matrix.
     */
    public boolean contains(String locationId) {
        return indexByLocationId.containsKey(locationId);
    }

    /**
     * Returns the travel from one location to another, empty when either location is unknown or the pair is unreachable by road.
     */
    public Optional<Travel> between(String fromLocationId, String toLocationId) {
        if (fromLocationId.equals(toLocationId) && contains(fromLocationId)) {
            return Optional.of(Travel.NONE);
        }
        Integer fromIndex = indexByLocationId.get(fromLocationId);
        Integer toIndex = indexByLocationId.get(toLocationId);
        if (fromIndex == null || toIndex == null) {
            return Optional.empty();
        }
        Double distance = distancesMeters.get(fromIndex).get(toIndex);
        Double duration = durationsSeconds.get(fromIndex).get(toIndex);
        if (distance == null || duration == null) {
            return Optional.empty();
        }
        return Optional.of(new Travel(distance, duration));
    }

    private static List<Double> copyRow(List<Double> row) {
        return Collections.unmodifiableList(new ArrayList<>(row));
    }

    /**
     * Road distance in meters and driving time in seconds of one trip.
     */
    public record Travel(double distanceMeters, double durationSeconds) {

        public static final Travel NONE = new Travel(0, 0);

        /**
         * Returns the sum of this and the given trip.
         */
        public Travel plus(Travel other) {
            return new Travel(distanceMeters + other.distanceMeters, durationSeconds + other.durationSeconds);
        }

        /**
         * Returns this trip minus the given one, e.g. the saving of a cheaper route over a more expensive one.
         */
        public Travel minus(Travel other) {
            return new Travel(distanceMeters - other.distanceMeters, durationSeconds - other.durationSeconds);
        }
    }
}
