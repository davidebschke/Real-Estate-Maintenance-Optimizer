package com.remo.realestatemaintainceoptimizer.dto;

import java.time.Instant;

/**
 * A single, already-localized history log entry as returned to API consumers.
 */
public record HistoryEntryResponse(Instant timestamp, String message) {
}
