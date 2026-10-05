package com.remo.realestatemaintainceoptimizer.dto;

/**
 * The way of travelling a route is calculated for, carrying the openrouteservice profile that routes it.
 */
public enum RouteMode {
    CAR("driving-car"),
    WALKING("foot-walking");

    private final String routingProfile;

    RouteMode(String routingProfile) {
        this.routingProfile = routingProfile;
    }

    public String profile() {
        return routingProfile;
    }
}
