package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Apartment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Returns the apartment with the given id if its property is owned by the given account, holding a write lock on its row until the transaction ends so concurrent tenant changes of one apartment run one after another.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Apartment a where a.id = :id and a.property.ownerId = :ownerId")
    Optional<Apartment> findByIdAndOwnerIdForUpdate(@Param("id") String id, @Param("ownerId") String ownerId);

    /**
     * Returns how many apartments the given property has.
     */
    long countByPropertyId(String propertyId);
}
