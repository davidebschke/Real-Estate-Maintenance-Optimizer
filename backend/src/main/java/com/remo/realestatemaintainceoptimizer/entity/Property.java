package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A managed real-estate object ("Objekt"), persisted as one row of the {@code properties} table.
 */
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 50)
    private String icon;

    private Double latitude;

    private Double longitude;

    @Version
    private Long version;

    protected Property() {
    }

    public Property(
            String id, String ownerId, String name, String address, String icon, Double latitude, Double longitude) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.address = address;
        this.icon = icon;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Creates a property without known coordinates, geocoded on demand via {@code GeocodingService}.
     */
    public Property(String id, String ownerId, String name, String address, String icon) {
        this(id, ownerId, name, address, icon, null, null);
    }

    /**
     * Replaces the name, address and coordinates of this property, keeping its id and icon.
     */
    public void updateDetails(String newName, String newAddress, Double newLatitude, Double newLongitude) {
        this.name = newName;
        this.address = newAddress;
        this.latitude = newLatitude;
        this.longitude = newLongitude;
    }

    public String id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

    public String icon() {
        return icon;
    }

    public Double latitude() {
        return latitude;
    }

    public Double longitude() {
        return longitude;
    }
}
