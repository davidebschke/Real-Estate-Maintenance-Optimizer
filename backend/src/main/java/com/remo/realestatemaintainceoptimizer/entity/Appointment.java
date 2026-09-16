package com.remo.realestatemaintainceoptimizer.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * A single scheduled maintenance appointment, persisted as one JSON file per appointment.
 */
public record Appointment(
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
        List<HistoryEntry> history,
        LocalDateTime actualEnd) {

    /**
     * Returns whether this appointment has been marked as completed.
     */
    public boolean completed() {
        return actualEnd != null;
    }

    /**
     * Returns a copy of this appointment with new schedule bounds and an appended history entry.
     */
    public Appointment withSchedule(LocalDateTime newStart, LocalDateTime newEnd, HistoryEntry historyEntry) {
        return new Appointment(
                id, seriesId, title, propertyId, propertyName, propertyAddress, description,
                newStart, newEnd, locked, recurring, recurrenceIntervalMonths, materials,
                appendedHistory(historyEntry), actualEnd);
    }

    /**
     * Returns a copy of this appointment with a new actual-end/completion state and an appended history entry.
     */
    public Appointment withActualEnd(LocalDateTime newActualEnd, HistoryEntry historyEntry) {
        return new Appointment(
                id, seriesId, title, propertyId, propertyName, propertyAddress, description,
                start, end, locked, recurring, recurrenceIntervalMonths, materials,
                appendedHistory(historyEntry), newActualEnd);
    }

    private List<HistoryEntry> appendedHistory(HistoryEntry historyEntry) {
        return Stream.concat(history.stream(), Stream.of(historyEntry)).toList();
    }
}
