package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * One AI optimization run of an account, persisted as one row of the {@code optimization_runs} table.
 */
@Entity
@Table(name = "optimization_runs")
public class OptimizationRun {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "candidate_count", nullable = false)
    private int candidateCount;

    @Version
    private Long version;

    protected OptimizationRun() {
    }

    public OptimizationRun(String id, String ownerId, Instant createdAt, String model, int candidateCount) {
        this.id = id;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.model = model;
        this.candidateCount = candidateCount;
    }

    public String id() {
        return id;
    }

    public String ownerId() {
        return ownerId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String model() {
        return model;
    }

    public int candidateCount() {
        return candidateCount;
    }
}
