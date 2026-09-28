package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the full appointment REST API against a throwaway PostgreSQL database, emptied and seeded with one property before each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

    @BeforeEach
    void seedProperty() {
        propertyRepository.deleteAllInBatch();
        propertyRepository.save(new Property(
                "property-1", "Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building"));
    }

    @Test
    void createdAppointmentIsThenListedAndReadable() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "description": "Was ist zu tun?",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": ["Kehrmaschine"]
                }
                """;

        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", equalTo("Kellerreinigung Q3")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyName", equalTo("Wohnanlage Sonnenhof")))
                .andExpect(jsonPath("$.propertyAddress", equalTo("Aachener Str. 512, 50933 Köln-Braunsenfeld")))
                .andExpect(jsonPath("$.materials[0]", equalTo("Kehrmaschine")));
    }

    @Test
    void creatingForAnUnknownPropertyReturnsNotFound() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "unknown-property",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/appointments")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void creatingWithABlankMaterialIsRejected() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": ["Kehrmaschine", " "]
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingWithBlankTitleIsRejected() throws Exception {
        String requestBody = """
                {
                  "title": "",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void gettingAnUnknownAppointmentReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/appointments/{id}", "unknown-id")).andExpect(status().isNotFound());
    }

    @Test
    void movingALockedAppointmentReturnsConflict() throws Exception {
        String requestBody = """
                {
                  "title": "TÜV-Termin",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T09:00:00",
                  "durationMinutes": 60,
                  "locked": true,
                  "recurring": false,
                  "materials": []
                }
                """;

        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/api/appointments/{id}/schedule", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\": \"2026-08-12T09:00:00\", \"durationMinutes\": 60}"))
                .andExpect(status().isConflict());
    }

    @Test
    void completingAndReopeningAnAppointmentUpdatesItsCompletedState() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/api/appointments/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", equalTo(true)));

        mockMvc.perform(patch("/api/appointments/{id}/reopen", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", equalTo(false)));
    }

    @Test
    void completingWithAnExplicitActualEndUsesTheGivenTimestamp() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/api/appointments/{id}/complete", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualEnd\": \"2026-08-10T16:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", equalTo(true)))
                .andExpect(jsonPath("$.actualEnd", equalTo("2026-08-10T16:30:00")));
    }

    @Test
    void deletingAnAppointmentRemovesIt() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/appointments/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/appointments/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deletingWithSeriesScopeOnANonRecurringAppointmentReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T13:00:00",
                  "durationMinutes": 120,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/appointments/{id}", id).param("scope", "series"))
                .andExpect(status().isBadRequest());
    }
}
