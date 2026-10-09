package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists apartments and, through them, their tenants in the {@code apartments} and {@code tenants} tables via Spring Data JPA.
 */
public interface ApartmentRepository extends JpaRepository<Apartment, String> {

    /**
     * Returns every apartment of the given property together with its tenants.
     */
    @EntityGraph(attributePaths = "tenants")
    List<Apartment> findAllByPropertyId(String propertyId);

    /**
     * Returns the apartment with the given id if its property is owned by the given account.
     */
    Optional<Apartment> findByIdAndPropertyOwnerId(String id, String ownerId);

    /**
     * Returns how many apartments the given property has.
     */
    long countByPropertyId(String propertyId);
}
