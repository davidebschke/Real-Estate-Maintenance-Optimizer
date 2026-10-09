package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import com.remo.realestatemaintainceoptimizer.service.PlanningSnapshotFactory.AppointmentLabel;
import com.remo.realestatemaintainceoptimizer.service.PlanningSnapshotFactory.PlanningSnapshot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies how appointments become the planning view: which properties the travel matrix covers, which visits may move and how long completed ones occupy the schedule.
 */
class PlanningSnapshotFactoryTest {

    private static final LocalDate RELEVANT_DAY = LocalDate.of(2026, 11, 3);
    private static final Property WEST = new Property("west", "owner", "West", "Weststr. 1", "pi-building", 50.93, 6.89);
    private static final Property EAST = new Property("east", "owner", "Ost", "Oststr. 1", "pi-building", 50.94, 6.97);
    private static final Property NORTH = new Property("north", "owner", "Nord", "Nordstr. 1", "pi-building", 50.97, 6.95);
    private static final Property WITHOUT_COORDINATES = new Property("nowhere", "owner", "Ohne Lage", "Unbekannt 1", "pi-building");

    private static OptimizationProperties propertiesWithMatrixSize(int maxMatrixLocations) {
        return new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500, 3, 60,
                maxMatrixLocations, 400, ZoneId.of("Europe/Berlin"));
    }

    private static Appointment appointment(String id, Property property, LocalDateTime start, boolean locked, LocalDateTime actualEnd) {
        return new Appointment(id, null, "Termin " + id, property, "", start, start.plusMinutes(60), locked, false, null,
                List.of(), List.of(), actualEnd);
    }

    @Test
    void locatesTheMostVisitedPropertiesWithCoordinatesOnTheRelevantDaysUpToTheMatrixSize() {
        List<Appointment> appointments = List.of(
                appointment("1", WEST, RELEVANT_DAY.atTime(8, 0), false, null),
                appointment("2", WEST, RELEVANT_DAY.atTime(10, 0), false, null),
                appointment("3", EAST, RELEVANT_DAY.atTime(12, 0), false, null),
                appointment("4", NORTH, RELEVANT_DAY.atTime(14, 0), false, null),
                appointment("5", NORTH, RELEVANT_DAY.plusDays(1).atTime(8, 0), false, null),
                appointment("6", WITHOUT_COORDINATES, RELEVANT_DAY.atTime(16, 0), false, null));

        PlanningSnapshot snapshot = new PlanningSnapshotFactory(propertiesWithMatrixSize(2))
                .snapshot(appointments, RELEVANT_DAY::equals);

        assertThat(snapshot.locations()).extracting(Location::id).containsExactly("west", "east");
        assertThat(snapshot.locations().getFirst()).isEqualTo(new Location("west", 50.93, 6.89));
        assertThat(snapshot.visits()).extracting(PlanningVisit::locationId)
                .containsExactly("west", "west", "east", null, null, null);
        assertThat(snapshot.labels().get("3")).isEqualTo(new AppointmentLabel("Termin 3", "Ost"));
    }

    @Test
    void onlyOpenUnlockedAppointmentsAreMovableAndACompletedOneOccupiesTheScheduleUntilItsActualEnd() {
        LocalDateTime start = RELEVANT_DAY.atTime(8, 0);
        List<Appointment> appointments = List.of(
                appointment("open", WEST, start, false, null),
                appointment("locked", WEST, start.plusHours(2), true, null),
                appointment("completed", WEST, start.plusHours(4), false, start.plusHours(5).plusMinutes(30)));

        List<PlanningVisit> visits = new PlanningSnapshotFactory(propertiesWithMatrixSize(50))
                .snapshot(appointments, RELEVANT_DAY::equals)
                .visits();

        assertThat(visits).extracting(PlanningVisit::movable).containsExactly(true, false, false);
        assertThat(visits.get(2).end()).isEqualTo(start.plusHours(5).plusMinutes(30));
        assertThat(visits.getFirst().located()).isTrue();
        assertThat(visits.getFirst().recurrenceAnchor()).isEqualTo(start);
    }
}
