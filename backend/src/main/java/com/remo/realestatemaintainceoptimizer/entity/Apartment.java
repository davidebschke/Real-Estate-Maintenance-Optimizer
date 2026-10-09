package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A rented apartment of a property together with its rent figures and the tenants living in it, persisted as one row of the {@code apartments} table.
 */
@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Property property;

    @Column(nullable = false)
    private int floor;

    @Column(name = "area_square_meters", nullable = false, precision = 7, scale = 2)
    private BigDecimal areaSquareMeters;

    @Column(name = "total_rent", nullable = false, precision = 9, scale = 2)
    private BigDecimal totalRent;

    @Column(name = "cold_rent", nullable = false, precision = 9, scale = 2)
    private BigDecimal coldRent;

    @Column(name = "additional_costs", nullable = false, precision = 9, scale = 2)
    private BigDecimal additionalCosts;

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tenant> tenants = new ArrayList<>();

    @Version
    private Long version;

    protected Apartment() {
    }

    public Apartment(
            String id,
            Property property,
            int floor,
            BigDecimal areaSquareMeters,
            BigDecimal totalRent,
            BigDecimal coldRent,
            BigDecimal additionalCosts) {
        this.id = id;
        this.property = property;
        this.floor = floor;
        this.areaSquareMeters = areaSquareMeters;
        this.totalRent = totalRent;
        this.coldRent = coldRent;
        this.additionalCosts = additionalCosts;
    }

    /**
     * Replaces the floor, area and rent figures of this apartment.
     */
    public void updateDetails(
            int newFloor,
            BigDecimal newAreaSquareMeters,
            BigDecimal newTotalRent,
            BigDecimal newColdRent,
            BigDecimal newAdditionalCosts) {
        this.floor = newFloor;
        this.areaSquareMeters = newAreaSquareMeters;
        this.totalRent = newTotalRent;
        this.coldRent = newColdRent;
        this.additionalCosts = newAdditionalCosts;
    }

    /**
     * Adds a new tenant with the given id and name to this apartment and returns it.
     */
    public Tenant addTenant(String tenantId, String firstName, String lastName) {
        Tenant tenant = new Tenant(tenantId, this, firstName, lastName);
        tenants.add(tenant);
        return tenant;
    }

    /**
     * Removes the given tenant from this apartment, deleting it from the database.
     */
    public void removeTenant(Tenant tenant) {
        tenants.remove(tenant);
    }

    /**
     * Returns whether exactly one tenant lives in this apartment.
     */
    public boolean hasSingleTenant() {
        return tenants.size() == 1;
    }

    public String id() {
        return id;
    }

    public Property property() {
        return property;
    }

    public int floor() {
        return floor;
    }

    public BigDecimal areaSquareMeters() {
        return areaSquareMeters;
    }

    public BigDecimal totalRent() {
        return totalRent;
    }

    public BigDecimal coldRent() {
        return coldRent;
    }

    public BigDecimal additionalCosts() {
        return additionalCosts;
    }

    public List<Tenant> tenants() {
        return List.copyOf(tenants);
    }
}
