package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Property;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists properties in the {@code properties} table via Spring Data JPA.
 */
public interface PropertyRepository extends JpaRepository<Property, String> {

    /**
     * Returns every property owned by the given account, sorted by name.
     */
    List<Property> findAllByOwnerIdOrderByNameAsc(String ownerId);

    /**
     * Returns the property with the given id if it is owned by the given account.
     */
    Optional<Property> findByIdAndOwnerId(String id, String ownerId);
}
