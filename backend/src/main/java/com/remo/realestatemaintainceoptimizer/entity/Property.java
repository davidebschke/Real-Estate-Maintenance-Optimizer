package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.YearMonth;

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

    @Column(name = "rent_tracking_start")
    private LocalDate rentTrackingStart;

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

    /**
     * Activates the rent payment check from the given month on, keeping an earlier start if it was already activated.
     */
    public void activateRentTracking(YearMonth month) {
        if (rentTrackingStart == null) {
            this.rentTrackingStart = month.atDay(1);
        }
    }

    /**
     * Returns the first month whose rent payments are checked, or null while the rent payment check is not activated.
     */
    public YearMonth rentTrackingStart() {
        return rentTrackingStart == null ? null : YearMonth.from(rentTrackingStart);
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
