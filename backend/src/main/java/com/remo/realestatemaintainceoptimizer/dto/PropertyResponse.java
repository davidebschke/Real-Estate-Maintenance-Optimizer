package com.remo.realestatemaintainceoptimizer.dto;

/**
 * A property as returned to API consumers.
 */
public record PropertyResponse(String id, String name, String address, String icon, Double latitude, Double longitude) {
}
