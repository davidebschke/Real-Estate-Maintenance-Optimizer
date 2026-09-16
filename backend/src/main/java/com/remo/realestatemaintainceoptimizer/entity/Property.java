package com.remo.realestatemaintainceoptimizer.entity;

/**
 * A managed real-estate object ("Objekt"), persisted as one JSON file per property.
 */
public record Property(String id, String name, String address, String icon) {
}
