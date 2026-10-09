package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A person renting an apartment, persisted as one row of the {@code tenants} table.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    public static final int NAME_MAX_LENGTH = 50;

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Apartment apartment;

    @Column(name = "first_name", nullable = false, length = NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = NAME_MAX_LENGTH)
    private String lastName;

    @Version
    private Long version;

    protected Tenant() {
    }

    public Tenant(String id, Apartment apartment, String firstName, String lastName) {
        this.id = id;
        this.apartment = apartment;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Replaces the first and last name of this tenant.
     */
    public void updateName(String newFirstName, String newLastName) {
        this.firstName = newFirstName;
        this.lastName = newLastName;
    }

    public String id() {
        return id;
    }

    public Apartment apartment() {
        return apartment;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }
}
