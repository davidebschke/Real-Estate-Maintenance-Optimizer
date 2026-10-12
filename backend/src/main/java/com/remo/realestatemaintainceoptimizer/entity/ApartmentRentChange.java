package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A change of an apartment's total rent that took effect in one month, remembering the rent before it so earlier months keep being judged by that rent, persisted as one row of the {@code apartment_rent_changes} table.
 */
@Entity
@Table(name = "apartment_rent_changes")
public class ApartmentRentChange {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Apartment apartment;

    @Column(name = "effective_month", nullable = false)
    private LocalDate effectiveMonth;

    @Column(name = "previous_rent", nullable = false, precision = 9, scale = 2)
    private BigDecimal previousRent;

    @Version
    private Long version;

    protected ApartmentRentChange() {
    }

    public ApartmentRentChange(String id, Apartment apartment, YearMonth effectiveMonth, BigDecimal previousRent) {
        this.id = id;
        this.apartment = apartment;
        this.effectiveMonth = effectiveMonth.atDay(1);
        this.previousRent = previousRent;
    }

    public String id() {
        return id;
    }

    public Apartment apartment() {
        return apartment;
    }

    public YearMonth effectiveMonth() {
        return YearMonth.from(effectiveMonth);
    }

    public BigDecimal previousRent() {
        return previousRent;
    }
}
