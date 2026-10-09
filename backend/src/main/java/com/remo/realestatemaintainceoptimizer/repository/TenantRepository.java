package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persists tenants in the {@code tenants} table via Spring Data JPA.
 */
public interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Returns the tenant with the given id if the property of its apartment is owned by the given account.
     */
    Optional<Tenant> findByIdAndApartmentPropertyOwnerId(String id, String ownerId);

    /**
     * Returns the number of tenants living in the apartments of the given property.
     */
    long countByApartmentPropertyId(String propertyId);

    /**
     * Returns the tenant count of every property of the given account that has at least one tenant.
     */
    @Query("""
            select t.apartment.property.id as propertyId, count(t) as tenantCount
            from Tenant t
            where t.apartment.property.ownerId = :ownerId
            group by t.apartment.property.id
            """)
    List<PropertyTenantCount> countTenantsPerPropertyByOwnerId(@Param("ownerId") String ownerId);
}
