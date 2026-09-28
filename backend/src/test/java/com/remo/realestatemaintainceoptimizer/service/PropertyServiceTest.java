package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.CreatePropertyRequest;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.exception.AppointmentNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.PropertyNotFoundException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies property listing (sorted by name), lookup by id, update, and cascading delete, including the not-found case, against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PropertyServiceTest {

    @Autowired
    private PropertyService service;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PropertyRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAllInBatch();
    }

    @Test
    void listsEveryPropertySortedByName() {
        repository.save(new Property("2", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        repository.save(new Property("1", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));

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
    void updatesNameAddressAndCoordinatesWhileKeepingIdAndIcon() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building", 50.94, 6.88));

        var response = service.update("1", new CreatePropertyRequest(
                "Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", 50.97, 6.95));

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.name()).isEqualTo("Wohnanlage Nordpark");
        assertThat(response.address()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(response.icon()).isEqualTo("pi-building");
        assertThat(response.latitude()).isEqualTo(50.97);
        assertThat(response.longitude()).isEqualTo(6.95);
        assertThat(service.getById("1").name()).isEqualTo("Wohnanlage Nordpark");
    }

    @Test
    void updatingAPropertyIsReflectedInItsAppointmentsOnly() {
        repository.save(new Property("1", "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        repository.save(new Property("2", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        AppointmentResponse appointment = appointmentService.create(createAppointmentRequest("1"));
        AppointmentResponse unrelated = appointmentService.create(createAppointmentRequest("2"));

        service.update("1", new CreatePropertyRequest("Wohnanlage Nordpark", "Nordparkstr. 3, 50733 Köln", null, null));

        AppointmentResponse updated = appointmentService.getById(appointment.id());
        assertThat(updated.propertyName()).isEqualTo("Wohnanlage Nordpark");
        assertThat(updated.propertyAddress()).isEqualTo("Nordparkstr. 3, 50733 Köln");
        assertThat(appointmentService.getById(unrelated.id()).propertyName()).isEqualTo("Wohnanlage Rheinblick");
    }

    @Test
    void throwsWhenUpdatingAnUnknownProperty() {
        assertThatThrownBy(() -> service.update("unknown", new CreatePropertyRequest("Name", "Address", null, null)))
                .isInstanceOf(PropertyNotFoundException.class);
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
        repository.save(new Property("2", "Wohnanlage Rheinblick", "Rheinuferstr. 8", "pi-building"));
        AppointmentResponse first = appointmentService.create(createAppointmentRequest("1"));
        AppointmentResponse second = appointmentService.create(createAppointmentRequest("1"));
        AppointmentResponse unrelated = appointmentService.create(createAppointmentRequest("2"));

        service.delete("1");

        assertThatThrownBy(() -> appointmentService.getById(first.id())).isInstanceOf(AppointmentNotFoundException.class);
        assertThatThrownBy(() -> appointmentService.getById(second.id())).isInstanceOf(AppointmentNotFoundException.class);
        assertThat(appointmentService.getById(unrelated.id())).isNotNull();
    }

    @Test
    void throwsWhenDeletingAnUnknownProperty() {
        assertThatThrownBy(() -> service.delete("unknown")).isInstanceOf(PropertyNotFoundException.class);
    }

    private CreateAppointmentRequest createAppointmentRequest(String propertyId) {
        return new CreateAppointmentRequest(
                "Kellerreinigung Q3",
                propertyId,
                "Was ist zu tun?",
                LocalDateTime.of(2026, 8, 11, 13, 0),
                120,
                false,
                false,
                null,
                List.of("Kehrmaschine"));
    }
}
