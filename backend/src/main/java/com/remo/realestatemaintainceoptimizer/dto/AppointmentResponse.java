package com.remo.realestatemaintainceoptimizer.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * An appointment as returned to API consumers, with a localized history log.
 */
public record AppointmentResponse(
        String id,
        String seriesId,
        String title,
        String propertyId,
        String propertyName,
        String propertyAddress,
        String description,
        LocalDateTime start,
        LocalDateTime end,
        boolean locked,
        boolean recurring,
        Integer recurrenceIntervalMonths,
        List<String> materials,
        List<HistoryEntryResponse> history,
        LocalDateTime actualEnd,
        boolean completed) {
}
