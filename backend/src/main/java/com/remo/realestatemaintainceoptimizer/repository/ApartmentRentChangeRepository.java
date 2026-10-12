package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persists the rent changes of apartments in the {@code apartment_rent_changes} table via Spring Data JPA.
 */
public interface ApartmentRentChangeRepository extends JpaRepository<ApartmentRentChange, String> {

    /**
     * Returns every rent change of the apartments of the given property.
     */
    List<ApartmentRentChange> findAllByApartmentPropertyId(String propertyId);

    /**
     * Returns every rent change of the apartments of the properties owned by the given account.
     */
    List<ApartmentRentChange> findAllByApartmentPropertyOwnerId(String ownerId);

    /**
     * Deletes every rent change of the apartment that is effective in the month starting on the given day or later.
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from ApartmentRentChange c where c.apartment.id = :apartmentId and c.effectiveMonth >= :effectiveMonth")
    void deleteFrom(@Param("apartmentId") String apartmentId, @Param("effectiveMonth") LocalDate effectiveMonth);
}
