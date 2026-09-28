package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Property;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists properties in the {@code properties} table via Spring Data JPA.
 */
public interface PropertyRepository extends JpaRepository<Property, String> {

    /**
     * Returns every property, sorted by name.
     */
    List<Property> findAllByOrderByNameAsc();
}
