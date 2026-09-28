package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Verifies appointment persistence (materials, history, completion), the derived queries, and the database-level cascade on property deletion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class AppointmentRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 11, 13, 0);
    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private EntityManager entityManager;

    private Property property;

    @BeforeEach
    void seedProperty() {
        property = propertyRepository.save(
                new Property("property-1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
    }

    @Test
    void persistsAndReloadsMaterialsHistoryAndActualEnd() {
        repository.save(new Appointment(
                "appointment-1", null, "Kellerreinigung Q3", property, "Was ist zu tun?",
                START, START.plusHours(2), true, false, null,
                List.of("Kehrmaschine", "Müllsäcke 10x"),
                List.of(new HistoryEntry(CREATED_AT, HistoryEventType.CREATED, List.of())),
                START.plusHours(3)));
        flushAndClear();

        Appointment reloaded = repository.findById("appointment-1").orElseThrow();

        assertThat(reloaded.title()).isEqualTo("Kellerreinigung Q3");
        assertThat(reloaded.property().name()).isEqualTo("Wohnanlage Sonnenhof");
        assertThat(reloaded.start()).isEqualTo(START);
        assertThat(reloaded.end()).isEqualTo(START.plusHours(2));
        assertThat(reloaded.locked()).isTrue();
        assertThat(reloaded.materials()).containsExactly("Kehrmaschine", "Müllsäcke 10x");
        assertThat(reloaded.history()).containsExactly(new HistoryEntry(CREATED_AT, HistoryEventType.CREATED, List.of()));
        assertThat(reloaded.actualEnd()).isEqualTo(START.plusHours(3));
        assertThat(reloaded.completed()).isTrue();
    }

    @Test
    void appendedHistoryEntriesKeepTheirOrderAndMessageArguments() {
        repository.save(createAppointment("appointment-1", null, START));
        flushAndClear();
        HistoryEntry moveEntry = new HistoryEntry(
                Instant.parse("2026-08-02T10:00:00Z"),
                HistoryEventType.MOVED,
                List.of("11.08.2026 13:00–15:00", "12.08.2026 13:00–15:00"));

        repository.findById("appointment-1").orElseThrow()
                .reschedule(START.plusDays(1), START.plusDays(1).plusHours(2), moveEntry);
        flushAndClear();

        Appointment reloaded = repository.findById("appointment-1").orElseThrow();
        assertThat(reloaded.start()).isEqualTo(START.plusDays(1));
        assertThat(reloaded.history()).extracting(HistoryEntry::type)
                .containsExactly(HistoryEventType.CREATED, HistoryEventType.MOVED);
        assertThat(reloaded.history().get(1).messageArgs())
                .containsExactly("11.08.2026 13:00–15:00", "12.08.2026 13:00–15:00");
    }

    @Test
    void listsEveryAppointmentSortedByStart() {
        repository.save(createAppointment("later", null, START.plusDays(2)));
        repository.save(createAppointment("earlier", null, START));
        flushAndClear();

        assertThat(repository.findAllByOrderByStartAsc()).extracting(Appointment::id).containsExactly("earlier", "later");
    }

    @Test
    void findsOnlyAppointmentsOfTheGivenSeries() {
        repository.save(createAppointment("series-a-1", "series-a", START));
        repository.save(createAppointment("series-a-2", "series-a", START.plusMonths(3)));
        repository.save(createAppointment("series-b-1", "series-b", START));
        flushAndClear();

        assertThat(repository.findBySeriesId("series-a")).extracting(Appointment::id)
                .containsExactlyInAnyOrder("series-a-1", "series-a-2");
    }

    @Test
    void deletingAPropertyCascadesToItsAppointmentsOnly() {
        Property otherProperty = propertyRepository.save(
                new Property("property-2", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        repository.save(createAppointment("appointment-1", null, START));
        repository.save(new Appointment(
                "appointment-2", null, "Treppenhausreinigung", otherProperty, "", START, START.plusHours(1),
                false, false, null, List.of(), List.of(), null));
        flushAndClear();

        propertyRepository.delete(propertyRepository.findById("property-1").orElseThrow());
        flushAndClear();

        assertThat(repository.findById("appointment-1")).isEmpty();
        assertThat(repository.findById("appointment-2")).isPresent();
    }

    private Appointment createAppointment(String id, String seriesId, LocalDateTime start) {
        boolean recurring = seriesId != null;
        return new Appointment(
                id, seriesId, "Kellerreinigung Q3", property, "Was ist zu tun?",
                start, start.plusHours(2), false, recurring, recurring ? 3 : null,
                List.of("Kehrmaschine"),
                List.of(new HistoryEntry(CREATED_AT, HistoryEventType.CREATED, List.of())),
                null);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
