package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists appointments in the {@code appointments} table via Spring Data JPA.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    /**
     * Returns every appointment, sorted by start time.
     */
    List<Appointment> findAllByOrderByStartAsc();

    /**
     * Returns every appointment sharing the given series id.
     */
    List<Appointment> findBySeriesId(String seriesId);
}
