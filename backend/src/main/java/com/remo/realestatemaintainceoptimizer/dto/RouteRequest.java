package com.remo.realestatemaintainceoptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Payload for calculating the route through an ordered list of positions, by car unless another mode is given.
 */
public record RouteRequest(
        @NotNull @Size(min = RouteRequest.MIN_COORDINATES, max = RouteRequest.MAX_COORDINATES)
                List<@NotNull @Valid RouteCoordinate> coordinates,
        RouteMode mode) {

    public static final int MIN_COORDINATES = 2;
    public static final int MAX_COORDINATES = 25;

    public RouteRequest {
        if (mode == null) {
            mode = RouteMode.CAR;
        }
    }
}
