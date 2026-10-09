package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists tenants in the {@code tenants} table via Spring Data JPA.
 */
public interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Returns the tenant with the given id if the property of its apartment is owned by the given account.
     */
    Optional<Tenant> findByIdAndApartmentPropertyOwnerId(String id, String ownerId);
}
