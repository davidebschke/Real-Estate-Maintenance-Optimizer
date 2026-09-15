package com.remo.realestatemaintainceoptimizer.entity;

import java.time.Instant;
import java.util.List;

/**
 * A single, localizable entry in an appointment's history log.
 */
public record HistoryEntry(Instant timestamp, HistoryEventType type, List<String> messageArgs) {
}
