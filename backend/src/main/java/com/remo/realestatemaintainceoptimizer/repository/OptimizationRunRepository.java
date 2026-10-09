package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.OptimizationRun;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists AI optimization runs in the {@code optimization_runs} table via Spring Data JPA.
 */
public interface OptimizationRunRepository extends JpaRepository<OptimizationRun, String> {
}
