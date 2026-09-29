package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists appointments in the {@code appointments} table via Spring Data JPA, scoped to an account through each appointment's property.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    /**
     * Returns every appointment of the given account's properties, sorted by start time.
     */
    List<Appointment> findAllByPropertyOwnerIdOrderByStartAsc(String ownerId);

    /**
     * Returns the appointment with the given id if its property is owned by the given account.
     */
    Optional<Appointment> findByIdAndPropertyOwnerId(String id, String ownerId);

    /**
     * Returns every appointment sharing the given series id among the given account's properties.
     */
    List<Appointment> findBySeriesIdAndPropertyOwnerId(String seriesId, String ownerId);
}
