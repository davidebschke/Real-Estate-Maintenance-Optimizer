package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.ApartmentRentChange;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * Returns whether the apartment already has a rent change effective in the month starting on the given day.
     */
    boolean existsByApartmentIdAndEffectiveMonth(String apartmentId, LocalDate effectiveMonth);
}
