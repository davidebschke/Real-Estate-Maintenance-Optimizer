package com.remo.realestatemaintainceoptimizer.entity;

/**
 * A managed real-estate object ("Objekt"), persisted as one JSON file per property.
 */
public record Property(String id, String name, String address, String icon, Double latitude, Double longitude) {

    /**
     * Creates a property without known coordinates, geocoded on demand via {@code GeocodingService}.
     */
    public Property(String id, String name, String address, String icon) {
        this(id, name, address, icon, null, null);
    }
}
