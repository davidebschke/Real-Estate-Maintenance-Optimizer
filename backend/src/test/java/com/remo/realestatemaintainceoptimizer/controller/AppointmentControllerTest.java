package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the full appointment REST API, including account isolation and demo limits, against a throwaway PostgreSQL database seeded with one account and property before each test.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AppointmentControllerTest {

    private static final String CREATE_REQUEST_BODY = """
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

    private static final String UPDATE_REQUEST_BODY = """
            {
              "title": "Kellerreinigung Q3 (aktualisiert)",
              "propertyId": "property-1",
              "description": "Neue Beschreibung",
              "start": "2026-08-11T13:00:00",
              "durationMinutes": 90,
              "locked": false,
              "recurring": false,
              "materials": ["Kehrmaschine"]
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    private MockMvc mockMvc;

    private MockMvc otherAccountMockMvc;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        User owner = TestAccounts.saveRegularAccount(userRepository);
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, owner);
        otherAccountMockMvc = TestAccounts.mockMvcAs(context, jwtService, TestAccounts.saveRegularAccount(userRepository));
        propertyRepository.save(new Property(
                "property-1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln-Braunsenfeld", "pi-building"));
    }

    @Test
    void anotherAccountCanNeitherSeeNorChangeAnAppointment() throws Exception {
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        otherAccountMockMvc.perform(get("/api/appointments")).andExpect(jsonPath("$", hasSize(0)));
        otherAccountMockMvc.perform(get("/api/appointments/{id}", id)).andExpect(status().isNotFound());
        otherAccountMockMvc.perform(patch("/api/appointments/{id}/schedule", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\": \"2026-08-12T09:00:00\", \"durationMinutes\": 60}"))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(put("/api/appointments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(patch("/api/appointments/{id}/complete", id)).andExpect(status().isNotFound());
        otherAccountMockMvc.perform(patch("/api/appointments/{id}/reopen", id)).andExpect(status().isNotFound());
        otherAccountMockMvc.perform(delete("/api/appointments/{id}", id)).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", equalTo(false)));
    }

    @Test
    void creatingAnAppointmentForAnotherAccountsPropertyReturnsNotFound() throws Exception {
        otherAccountMockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/appointments")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aDemoAccountBeyondItsCreationLimitGetsALocalizedForbidden() throws Exception {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 0);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo-Objekt", "Demostr. 1", "pi-building"));

        TestAccounts.mockMvcAs(context, jwtService, demo).perform(post("/api/appointments")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY.replace("property-1", "demo-property")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("A demo account can create at most 3 additional appointments.")));
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
    void creatingAnOverlappingAppointmentReturnsAConflictWithASuggestedNextFreeSlot() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andExpect(status().isCreated());

        String overlappingRequestBody = """
                {
                  "title": "Fensterreinigung",
                  "propertyId": "property-1",
                  "description": "",
                  "start": "2026-08-11T14:00:00",
                  "durationMinutes": 60,
                  "locked": false,
                  "recurring": false,
                  "materials": []
                }
                """;

        mockMvc.perform(post("/api/appointments")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingRequestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(
                        "$.message",
                        equalTo("The selected time range overlaps with an existing appointment of the same account or leaves less than the required buffer time to it.")))
                .andExpect(jsonPath("$.suggestedStart", equalTo("2026-08-11T15:15:00")))
                .andExpect(jsonPath("$.suggestedEnd", equalTo("2026-08-11T16:15:00")));

        mockMvc.perform(get("/api/appointments")).andExpect(jsonPath("$", hasSize(1)));
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
    void movingAnAppointmentIntoAnotherOneReturnsAConflictWithASuggestedSlotAndKeepsItUnchanged() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andExpect(status().isCreated());
        String movableRequestBody = CREATE_REQUEST_BODY.replace("2026-08-11T13:00:00", "2026-08-12T09:00:00");
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movableRequestBody))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/api/appointments/{id}/schedule", id)
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"start\": \"2026-08-11T14:00:00\", \"durationMinutes\": 60}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.suggestedStart", equalTo("2026-08-11T15:15:00")))
                .andExpect(jsonPath("$.suggestedEnd", equalTo("2026-08-11T16:15:00")));

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(jsonPath("$.start", equalTo("2026-08-12T09:00:00")));
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

        mockMvc.perform(put("/api/appointments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "TÜV-Termin",
                                  "propertyId": "property-1",
                                  "description": "",
                                  "start": "2026-08-12T09:00:00",
                                  "durationMinutes": 60,
                                  "locked": true,
                                  "recurring": false,
                                  "materials": []
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesAnAppointment() throws Exception {
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(put("/api/appointments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(id)))
                .andExpect(jsonPath("$.title", equalTo("Kellerreinigung Q3 (aktualisiert)")))
                .andExpect(jsonPath("$.description", equalTo("Neue Beschreibung")))
                .andExpect(jsonPath("$.materials[0]", equalTo("Kehrmaschine")));

        mockMvc.perform(get("/api/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Kellerreinigung Q3 (aktualisiert)")));
    }

    @Test
    void updatingForAnUnknownPropertyReturnsNotFound() throws Exception {
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(put("/api/appointments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY.replace("property-1", "unknown-property")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingAnUnknownAppointmentReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/appointments/{id}", "unknown-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingWithBlankTitleIsRejected() throws Exception {
        String response = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_REQUEST_BODY))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(put("/api/appointments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_BODY.replace("Kellerreinigung Q3 (aktualisiert)", "")))
                .andExpect(status().isBadRequest());
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
                        .content("{\"actualEnd\": \"2026-08-11T16:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", equalTo(true)))
                .andExpect(jsonPath("$.actualEnd", equalTo("2026-08-11T16:30:00")));
    }

    @Test
    void completingWithAnActualEndBeforeThePlannedStartIsRejected() throws Exception {
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
                .andExpect(status().isBadRequest());
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
