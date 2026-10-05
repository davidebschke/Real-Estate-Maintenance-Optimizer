package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Payload for calculating the road route through an ordered list of positions.
 */
public record RouteRequest(
        @NotNull @Size(min = RouteRequest.MIN_COORDINATES, max = RouteRequest.MAX_COORDINATES)
                List<@NotNull @Valid RouteCoordinate> coordinates) {

    public static final int MIN_COORDINATES = 2;
    public static final int MAX_COORDINATES = 25;
}
