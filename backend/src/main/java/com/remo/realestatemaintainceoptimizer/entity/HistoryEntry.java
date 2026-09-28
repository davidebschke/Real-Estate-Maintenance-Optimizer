package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A single, localizable entry in an appointment's history log.
 */
@Embeddable
public record HistoryEntry(
        @Column(name = "occurred_at", nullable = false) Instant timestamp,
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) HistoryEventType type,
        @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "message_args", nullable = false) List<String> messageArgs) {
}
