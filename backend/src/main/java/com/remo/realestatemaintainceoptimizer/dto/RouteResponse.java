package com.remo.realestatemaintainceoptimizer.dto;

import java.util.List;

/**
 * A calculated road route: its geometry as GeoJSON-ordered {@code [longitude, latitude]} pairs, the total distance in meters and driving time in seconds, and one leg per pair of consecutive stops.
 */
public record RouteResponse(
        List<List<Double>> geometry, double distanceMeters, double durationSeconds, List<RouteLeg> legs) {
}
