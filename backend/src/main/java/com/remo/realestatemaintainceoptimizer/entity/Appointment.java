package com.remo.realestatemaintainceoptimizer.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

/**
 * A single scheduled maintenance appointment, persisted as one row of the {@code appointments} table.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "series_id", length = 36)
    private String seriesId;

    @Column(nullable = false, length = 50)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Property property;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime start;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime end;

    @Column(nullable = false)
    private boolean locked;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "recurrence_interval_months")
    private Integer recurrenceIntervalMonths;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private List<String> materials = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "appointment_history_entries", joinColumns = @JoinColumn(name = "appointment_id"))
    @OrderColumn(name = "sort_order")
    private List<HistoryEntry> history = new ArrayList<>();

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Column(name = "recurrence_anchor_start")
    private LocalDateTime recurrenceAnchorStart;

    @Version
    private Long version;

    protected Appointment() {
    }

    public Appointment(
            String id,
            String seriesId,
            String title,
            Property property,
            String description,
            LocalDateTime start,
            LocalDateTime end,
            boolean locked,
            boolean recurring,
            Integer recurrenceIntervalMonths,
            List<String> materials,
            List<HistoryEntry> history,
            LocalDateTime actualEnd) {
        this.id = id;
        this.seriesId = seriesId;
        this.title = title;
        this.property = property;
        this.description = description;
        this.start = start;
        this.end = end;
        this.locked = locked;
        this.recurring = recurring;
        this.recurrenceIntervalMonths = recurrenceIntervalMonths;
        this.materials = new ArrayList<>(materials);
        this.history = new ArrayList<>(history);
        this.actualEnd = actualEnd;
    }

    /**
     * Returns whether this appointment has been marked as completed.
     */
    public boolean completed() {
        return actualEnd != null;
    }

    /**
     * Moves this appointment to new schedule bounds and appends the given history entry.
     */
    public void reschedule(LocalDateTime newStart, LocalDateTime newEnd, HistoryEntry historyEntry) {
        this.start = newStart;
        this.end = newEnd;
        history.add(historyEntry);
    }

    /**
     * Remembers the current start as the start this occurrence was originally planned for, unless an earlier automatic move already did.
     */
    public void anchorRecurrenceStart() {
        if (recurrenceAnchorStart == null) {
            recurrenceAnchorStart = start;
        }
    }

    /**
     * Returns the start automatic moves of this occurrence are bounded around: the originally planned start once an automatic move happened, the current start otherwise.
     */
    public LocalDateTime recurrenceAnchor() {
        return recurrenceAnchorStart != null ? recurrenceAnchorStart : start;
    }

    /**
     * Sets a new actual-end/completion state, where {@code null} means not completed, and appends the given history entry.
     */
    public void changeActualEnd(LocalDateTime newActualEnd, HistoryEntry historyEntry) {
        this.actualEnd = newActualEnd;
        history.add(historyEntry);
    }

    /**
     * Updates this appointment's title, property, schedule, locked/recurring state, description and materials, and appends the given history entry.
     */
    public void updateDetails(
            String newTitle,
            Property newProperty,
            String newDescription,
            LocalDateTime newStart,
            LocalDateTime newEnd,
            boolean newLocked,
            boolean newRecurring,
            Integer newRecurrenceIntervalMonths,
            String newSeriesId,
            List<String> newMaterials,
            HistoryEntry historyEntry) {
        this.title = newTitle;
        this.property = newProperty;
        this.description = newDescription;
        this.start = newStart;
        this.end = newEnd;
        this.locked = newLocked;
        this.recurring = newRecurring;
        this.recurrenceIntervalMonths = newRecurrenceIntervalMonths;
        this.seriesId = newSeriesId;
        this.materials = new ArrayList<>(newMaterials);
        history.add(historyEntry);
    }

    public String id() {
        return id;
    }

    public String seriesId() {
        return seriesId;
    }

    public String title() {
        return title;
    }

    public Property property() {
        return property;
    }

    public String description() {
        return description;
    }

    public LocalDateTime start() {
        return start;
    }

    public LocalDateTime end() {
        return end;
    }

    public boolean locked() {
        return locked;
    }

    public boolean recurring() {
        return recurring;
    }

    public Integer recurrenceIntervalMonths() {
        return recurrenceIntervalMonths;
    }

    public List<String> materials() {
        return List.copyOf(materials);
    }

    public List<HistoryEntry> history() {
        return List.copyOf(history);
    }

    public LocalDateTime actualEnd() {
        return actualEnd;
    }
}
