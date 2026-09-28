package com.remo.realestatemaintainceoptimizer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the property create, read, update and delete REST API against a throwaway PostgreSQL database, emptied and seeded with one property before each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private Property seededProperty;

    @BeforeEach
    void seedProperty() {
        propertyRepository.deleteAllInBatch();
        seededProperty = propertyRepository.save(new Property(
                "1", "Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building", 50.94, 6.88));
    }

    @Test
    void listsEverySeededProperty() throws Exception {
        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo("Wohnanlage Sonnenhof")));
    }

    @Test
    void getsAPropertyById() throws Exception {
        mockMvc.perform(get("/api/properties/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", equalTo("Aachener Str. 512, 50933 Köln-Braunsenfeld")))
                .andExpect(jsonPath("$.latitude", equalTo(50.94)))
                .andExpect(jsonPath("$.longitude", equalTo(6.88)));
    }

    @Test
    void gettingAnUnknownPropertyReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/properties/{id}", "unknown-id")).andExpect(status().isNotFound());
    }

    @Test
    void createsAProperty() throws Exception {
        String requestBody = """
                {
                  "name": "Wohnanlage Nordpark",
                  "address": "Nordparkstr. 3, 50733 Köln",
                  "latitude": 50.97,
                  "longitude": 6.95
                }
                """;

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Wohnanlage Nordpark")))
                .andExpect(jsonPath("$.icon", equalTo("pi-building")));
    }

    @Test
    void creatingAPropertyWithoutANameReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "",
                  "address": "Nordparkstr. 3, 50733 Köln"
                }
                """;

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAProperty() throws Exception {
        String requestBody = """
                {
                  "name": "Wohnanlage Nordpark",
                  "address": "Nordparkstr. 3, 50733 Köln",
                  "latitude": 50.97,
                  "longitude": 6.95
                }
                """;

        mockMvc.perform(put("/api/properties/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo("1")))
                .andExpect(jsonPath("$.name", equalTo("Wohnanlage Nordpark")))
                .andExpect(jsonPath("$.address", equalTo("Nordparkstr. 3, 50733 Köln")))
                .andExpect(jsonPath("$.icon", equalTo("pi-building")));
    }

    @Test
    void updatingAPropertyWithoutANameReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "name": "",
                  "address": "Nordparkstr. 3, 50733 Köln"
                }
                """;

        mockMvc.perform(put("/api/properties/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingAnUnknownPropertyReturnsNotFound() throws Exception {
        String requestBody = """
                {
                  "name": "Wohnanlage Nordpark",
                  "address": "Nordparkstr. 3, 50733 Köln"
                }
                """;

        mockMvc.perform(put("/api/properties/{id}", "unknown-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAPropertyRemovesItAndItsAppointments() throws Exception {
        appointmentRepository.save(createAppointment("appointment-1", seededProperty));

        mockMvc.perform(delete("/api/properties/{id}", "1")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/properties/{id}", "1")).andExpect(status().isNotFound());
        assertThat(appointmentRepository.findById("appointment-1")).isEmpty();
    }

    @Test
    void deletingAnUnknownPropertyReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/properties/{id}", "unknown-id")).andExpect(status().isNotFound());
    }

    private Appointment createAppointment(String id, Property property) {
        LocalDateTime start = LocalDateTime.of(2026, 8, 11, 13, 0);
        return new Appointment(
                id,
                null,
                "Kellerreinigung Q3",
                property,
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
