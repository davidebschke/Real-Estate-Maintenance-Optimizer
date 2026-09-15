package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.StorageProperties;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies that appointments are correctly saved as, read from, and deleted as individual JSON files.
 */
class AppointmentFileRepositoryTest {

    @TempDir
    private Path storageDirectory;

    private AppointmentFileRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AppointmentFileRepository(new StorageProperties(storageDirectory.toString()), new ObjectMapper());
    }

    @Test
    void savesAndReadsBackAnAppointment() {
        Appointment appointment = createAppointment("1", null);

        repository.save(appointment);

        assertThat(repository.findById("1")).contains(appointment);
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertThat(repository.findById("unknown")).isEmpty();
    }

    @Test
    void findAllReturnsEverySavedAppointment() {
        repository.save(createAppointment("1", null));
        repository.save(createAppointment("2", null));

        assertThat(repository.findAll()).extracting(Appointment::id).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void findAllSkipsAFileThatFailsToParse() throws IOException {
        repository.save(createAppointment("1", null));
        Files.writeString(storageDirectory.resolve("corrupt.json"), "{ not valid json");

        assertThat(repository.findAll()).extracting(Appointment::id).containsExactly("1");
    }

    @Test
    void findBySeriesIdReturnsOnlyMatchingAppointments() {
        repository.save(createAppointment("1", "series-a"));
        repository.save(createAppointment("2", "series-a"));
        repository.save(createAppointment("3", "series-b"));

        assertThat(repository.findBySeriesId("series-a")).extracting(Appointment::id).containsExactlyInAnyOrder("1", "2");
    }

    @Test
    void deleteByIdRemovesTheAppointment() {
        repository.save(createAppointment("1", null));

        repository.deleteById("1");

        assertThat(repository.findById("1")).isEmpty();
    }

    private Appointment createAppointment(String id, String seriesId) {
        LocalDateTime start = LocalDateTime.of(2026, 8, 11, 13, 0);
        return new Appointment(
                id,
                seriesId,
                "Kellerreinigung Q3",
                "property-1",
                "Wohnanlage Sonnenhof",
                "Aachener Str. 512, 50933 Köln-Braunsenfeld",
                "Was ist zu tun?",
                start,
                start.plusHours(2),
                false,
                seriesId != null,
                seriesId != null ? 3 : null,
                List.of("Kehrmaschine"),
                List.of(new HistoryEntry(Instant.parse("2026-08-01T10:00:00Z"), HistoryEventType.CREATED, List.of())));
    }
}
