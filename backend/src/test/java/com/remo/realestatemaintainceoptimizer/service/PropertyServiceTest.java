package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.config.PropertyStorageProperties;
import com.remo.realestatemaintainceoptimizer.config.StorageProperties;
import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentFileRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyFileRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies property listing (sorted by name), lookup by id, and cascading delete, including the not-found case.
 */
class PropertyServiceTest {

    @TempDir
    private Path storageDirectory;

    @TempDir
    private Path appointmentStorageDirectory;

    private PropertyFileRepository repository;
    private AppointmentFileRepository appointmentRepository;
    private PropertyService service;

    @BeforeEach
    void setUp() {
        repository = new PropertyFileRepository(new PropertyStorageProperties(storageDirectory.toString()), new ObjectMapper());
        appointmentRepository =
                new AppointmentFileRepository(new StorageProperties(appointmentStorageDirectory.toString()), new ObjectMapper());
        service = new PropertyService(repository, appointmentRepository);
    }

    @Test
    void listsEveryPropertySortedByName() {
        repository.save(new Property("1", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        repository.save(new Property("2", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        assertThat(service.listAll()).extracting("name")
                .containsExactly("Wohnanlage Rheinblick", "Wohnanlage Sonnenhof");
    }

    @Test
    void returnsThePropertyWithTheGivenId() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        assertThat(service.getById("1").name()).isEqualTo("Wohnanlage Sonnenhof");
    }

    @Test
    void includesStoredCoordinatesInTheResponse() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building", 50.94, 6.88));

        var response = service.getById("1");

        assertThat(response.latitude()).isEqualTo(50.94);
        assertThat(response.longitude()).isEqualTo(6.88);
    }

    @Test
    void throwsWhenNoPropertyExistsForTheGivenId() {
        assertThatThrownBy(() -> service.getById("unknown")).isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void createsAPropertyWithAGeneratedIdAndTheDefaultIcon() {
        var response = service.create(new CreatePropertyRequest(
                "Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", 50.97, 6.95));

        assertThat(response.id()).isNotBlank();
        assertThat(response.name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(response.address()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(response.icon()).isEqualTo("pi-building");
        assertThat(response.latitude()).isEqualTo(50.97);
        assertThat(response.longitude()).isEqualTo(6.95);
        assertThat(service.getById(response.id()).name()).isEqualTo("Wohnanlage Nordpark");
    }

    @Test
    void createsAPropertyWithoutCoordinatesWhenNoneAreGiven() {
        var response = service.create(new CreatePropertyRequest("Wohnanlage Nordpark", "Nordparkstr. 3", null, null));

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
    }

    @Test
    void deletingAPropertyRemovesIt() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));

        service.delete("1");

        assertThatThrownBy(() -> service.getById("1")).isInstanceOf(PropertyNotFoundException.class);
    }

    @Test
    void deletingAPropertyAlsoDeletesItsAppointments() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        appointmentRepository.save(createAppointment("appointment-1", "1"));
        appointmentRepository.save(createAppointment("appointment-2", "1"));
        appointmentRepository.save(createAppointment("appointment-3", "other-property"));

        service.delete("1");

        assertThat(appointmentRepository.findById("appointment-1")).isEmpty();
        assertThat(appointmentRepository.findById("appointment-2")).isEmpty();
        assertThat(appointmentRepository.findById("appointment-3")).isPresent();
    }

    @Test
    void throwsWhenDeletingAnUnknownProperty() {
        assertThatThrownBy(() -> service.delete("unknown")).isInstanceOf(PropertyNotFoundException.class);
    }

    private Appointment createAppointment(String id, String propertyId) {
        LocalDateTime start = LocalDateTime.of(2026, 8, 11, 13, 0);
        return new Appointment(
                id,
                null,
                "Kellerreinigung Q3",
                propertyId,
                "Wohnanlage Sonnenhof",
                "Aachener Str. 512, 50933 Köln-Braunsenfeld",
                "Was ist zu tun?",
                start,
                start.plusHours(2),
                false,
                false,
                null,
                List.of("Kehrmaschine"),
                List.of(new HistoryEntry(Instant.parse("2026-08-01T10:00:00Z"), HistoryEventType.CREATED, List.of())),
                null);
    }
}
