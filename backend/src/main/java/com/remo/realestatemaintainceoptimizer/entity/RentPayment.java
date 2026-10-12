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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * A rent payment of a tenant for one calendar month, persisted as one row of the {@code rent_payments} table; the source statement itself is never stored.
 */
@Entity
@Table(name = "rent_payments")
public class RentPayment {

    public static final int REFERENCE_MAX_LENGTH = 200;

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Tenant tenant;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = REFERENCE_MAX_LENGTH)
    private String reference;

    @Column(name = "period_year", nullable = false)
    private short periodYear;

    @Column(name = "period_month", nullable = false)
    private short periodMonth;

    @Version
    private Long version;

    protected RentPayment() {
    }

    public RentPayment(
            String id,
            Tenant tenant,
            LocalDate paymentDate,
            BigDecimal amount,
            String reference,
            int periodYear,
            int periodMonth) {
        this.id = id;
        this.tenant = tenant;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.reference = reference;
        this.periodYear = (short) periodYear;
        this.periodMonth = (short) periodMonth;
    }

    public String id() {
        return id;
    }

    public Tenant tenant() {
        return tenant;
    }

    public LocalDate paymentDate() {
        return paymentDate;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String reference() {
        return reference;
    }

    public int periodYear() {
        return periodYear;
    }

    public int periodMonth() {
        return periodMonth;
    }
}
