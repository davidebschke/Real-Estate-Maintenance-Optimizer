package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.RentPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
            select p.apartment.id as apartmentId, p.periodYear as periodYear, p.periodMonth as periodMonth,
                   sum(p.amount) as totalAmount
            from RentPayment p
            where p.apartment.property.id = :propertyId
            group by p.apartment.id, p.periodYear, p.periodMonth
            """)
    List<ApartmentMonthPayment> sumByApartmentAndMonthForProperty(@Param("propertyId") String propertyId);

    /**
     * Returns the rent paid per apartment and month for every apartment of the given account.
     */
    @Query("""
            select p.apartment.id as apartmentId, p.periodYear as periodYear, p.periodMonth as periodMonth,
                   sum(p.amount) as totalAmount
            from RentPayment p
            where p.apartment.property.ownerId = :ownerId
            group by p.apartment.id, p.periodYear, p.periodMonth
            """)
    List<ApartmentMonthPayment> sumByApartmentAndMonthForOwner(@Param("ownerId") String ownerId);

    /**
     * Returns the payments of tenants that were deleted since, for rent periods of the given year in the given property, oldest first.
     */
    @Query("""
            select p from RentPayment p join fetch p.apartment a
            where a.property.id = :propertyId and p.tenant is null and p.periodYear = :year
            order by p.paymentDate, p.id
            """)
    List<RentPayment> findFormerTenantPayments(@Param("propertyId") String propertyId, @Param("year") int year);

    /**
     * Returns whether the apartment already has a payment with exactly this date, amount and reference.
     */
    boolean existsByApartmentIdAndPaymentDateAndAmountAndReference(
            String apartmentId, LocalDate paymentDate, BigDecimal amount, String reference);

    /**
     * Detaches every payment from the given tenant and notes the tenant's name on it instead, so the payments stay with their apartment.
     */
    @Modifying(flushAutomatically = true)
    @Query("update RentPayment p set p.tenant = null, p.formerTenantName = :name where p.tenant.id = :tenantId")
    int markTenantAsFormer(@Param("tenantId") String tenantId, @Param("name") String name);
}
