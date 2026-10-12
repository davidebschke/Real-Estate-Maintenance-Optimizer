package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persists rent payments in the {@code rent_payments} table via Spring Data JPA.
 */
public interface RentPaymentRepository extends JpaRepository<RentPayment, String> {

    /**
     * Returns the rent paid per apartment and month for every apartment of the given property.
     */
    @Query("""
            select p.tenant.apartment.id as apartmentId, p.periodYear as periodYear, p.periodMonth as periodMonth,
                   sum(p.amount) as totalAmount
            from RentPayment p
            where p.tenant.apartment.property.id = :propertyId
            group by p.tenant.apartment.id, p.periodYear, p.periodMonth
            """)
    List<ApartmentMonthPayment> sumByApartmentAndMonthForProperty(@Param("propertyId") String propertyId);

    /**
     * Returns the rent paid per apartment and month for every apartment of the given account.
     */
    @Query("""
            select p.tenant.apartment.id as apartmentId, p.periodYear as periodYear, p.periodMonth as periodMonth,
                   sum(p.amount) as totalAmount
            from RentPayment p
            where p.tenant.apartment.property.ownerId = :ownerId
            group by p.tenant.apartment.id, p.periodYear, p.periodMonth
            """)
    List<ApartmentMonthPayment> sumByApartmentAndMonthForOwner(@Param("ownerId") String ownerId);

    /**
     * Returns whether the tenant already has a payment with exactly this date, amount and reference.
     */
    boolean existsByTenantIdAndPaymentDateAndAmountAndReference(
            String tenantId, LocalDate paymentDate, BigDecimal amount, String reference);
}
