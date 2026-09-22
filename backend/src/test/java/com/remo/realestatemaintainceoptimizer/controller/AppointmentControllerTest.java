package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the full appointment REST API against an isolated, temporary storage directory.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AppointmentControllerTest {

    @TempDir
    static Path storageDirectory;

    @DynamicPropertySource
    static void overrideStorageDirectory(DynamicPropertyRegistry registry) {
        registry.add("remo.storage.directory", storageDirectory::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createdAppointmentIsThenListedAndReadable() throws Exception {
        String requestBody = """
                {
                  "title": "Kellerreinigung Q3",
                  "propertyId": "property-1",
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512, 50933 Köln-Braunsenfeld",
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
                .andExpect(jsonPath("$.propertyName", equalTo("Wohnanlage Sonnenhof")));
    }

    @Test
    void creatingWithBlankTitleIsRejected() throws Exception {
        String requestBody = """
                {
                  "title": "",
                  "propertyId": "property-1",
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512",
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
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512",
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
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512",
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
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512",
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
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512",
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
                  "propertyName": "Wohnanlage Sonnenhof",
                  "propertyAddress": "Aachener Str. 512",
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
