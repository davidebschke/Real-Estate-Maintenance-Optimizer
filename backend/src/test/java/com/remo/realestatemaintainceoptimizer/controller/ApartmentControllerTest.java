package com.remo.realestatemaintainceoptimizer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.ApartmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.TenantRepository;
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
 * Verifies the apartment and tenant REST API, including validation, account isolation and cascading deletes, against a throwaway PostgreSQL database seeded with one account and property before each test.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ApartmentControllerTest {

    private static final String CREATE_BODY = """
            {
              "apartment": {"floor": 2, "areaSquareMeters": 64.50, "totalRent": 850.00, "coldRent": 650.00, "additionalCosts": 200.00},
              "tenant": {"firstName": "Erika", "lastName": "Mustermann"}
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

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private MockMvc mockMvc;

    private MockMvc otherAccountMockMvc;

    @BeforeEach
    void seedProperty() {
        userRepository.deleteAllInBatch();
        User owner = TestAccounts.saveRegularAccount(userRepository);
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, owner);
        otherAccountMockMvc = TestAccounts.mockMvcAs(context, jwtService, TestAccounts.saveRegularAccount(userRepository));
        propertyRepository.save(new Property("1", owner.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
    }

    @Test
    void createsAnApartmentWithItsFirstTenantAndListsIt() throws Exception {
        mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyId", equalTo("1")))
                .andExpect(jsonPath("$.floor", equalTo(2)))
                .andExpect(jsonPath("$.areaSquareMeters", equalTo(64.5)))
                .andExpect(jsonPath("$.tenants", hasSize(1)))
                .andExpect(jsonPath("$.tenants[0].lastName", equalTo("Mustermann")));

        mockMvc.perform(get("/api/properties/{id}/apartments", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].coldRent", equalTo(650.0)))
                .andExpect(jsonPath("$[0].additionalCosts", equalTo(200.0)));
    }

    @Test
    void addsEditsAndDeletesTenantsOfAnApartment() throws Exception {
        String created = mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn().getResponse().getContentAsString();
        String apartmentId = JsonPath.read(created, "$.id");
        String firstTenantId = JsonPath.read(created, "$.tenants[0].id");

        mockMvc.perform(post("/api/apartments/{id}/tenants", apartmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Max\", \"lastName\": \"Mustermann\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenants", hasSize(2)));

        mockMvc.perform(put("/api/tenants/{id}", firstTenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "apartment": {"floor": 3, "areaSquareMeters": 70, "totalRent": 900, "coldRent": 700, "additionalCosts": 200},
                                  "tenant": {"firstName": "Erika", "lastName": "Musterfrau"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floor", equalTo(3)))
                .andExpect(jsonPath("$.tenants[*].lastName", containsInAnyOrder("Musterfrau", "Mustermann")));

        mockMvc.perform(delete("/api/tenants/{id}", firstTenantId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/properties/{id}/apartments", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tenants", hasSize(1)))
                .andExpect(jsonPath("$[0].tenants[0].firstName", equalTo("Max")));
    }

    @Test
    void deletingTheLastTenantRemovesTheApartment() throws Exception {
        String created = mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(delete("/api/tenants/{id}", (String) JsonPath.read(created, "$.tenants[0].id")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/properties/{id}/apartments", "1")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deletingThePropertyRemovesItsApartmentsAndTenants() throws Exception {
        mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY));

        mockMvc.perform(delete("/api/properties/{id}", "1")).andExpect(status().isNoContent());

        assertThat(apartmentRepository.count()).isZero();
        assertThat(tenantRepository.count()).isZero();
    }

    @Test
    void rejectsInvalidPayloadsWithBadRequest() throws Exception {
        String missingTenantName = CREATE_BODY.replace("\"firstName\": \"Erika\"", "\"firstName\": \"\"");
        String negativeRent = CREATE_BODY.replace("\"totalRent\": 850.00", "\"totalRent\": -1");
        String zeroArea = CREATE_BODY.replace("\"areaSquareMeters\": 64.50", "\"areaSquareMeters\": 0");
        String tooManyDecimals = CREATE_BODY.replace("\"coldRent\": 650.00", "\"coldRent\": 650.001");
        String floorOutOfRange = CREATE_BODY.replace("\"floor\": 2", "\"floor\": 101");
        String missingApartment = "{\"tenant\": {\"firstName\": \"Erika\", \"lastName\": \"Mustermann\"}}";

        for (String body : new String[] {
            missingTenantName, negativeRent, zeroArea, tooManyDecimals, floorOutOfRange, missingApartment
        }) {
            mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/properties/{id}/apartments", "1")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsAnInvalidAdditionalTenant() throws Exception {
        String created = mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/apartments/{id}/tenants", (String) JsonPath.read(created, "$.id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Max\", \"lastName\": \" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aDemoAccountBeyondItsTenantLimitGetsALocalizedForbidden() throws Exception {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 0, 0, 0);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo Objekt", "Demostr. 1", "pi-building"));

        TestAccounts.mockMvcAs(context, jwtService, demo).perform(post("/api/properties/{id}/apartments", "demo-property")
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", equalTo("Ein Demo-Account kann höchstens 10 zusätzliche Mieter anlegen.")));
        assertThat(apartmentRepository.count()).isZero();
    }

    @Test
    void unknownIdsReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/properties/{id}/apartments", "unknown")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/properties/{id}/apartments", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/apartments/{id}/tenants", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Max\", \"lastName\": \"Mustermann\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/tenants/{id}", "unknown")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tenants/{id}", "unknown")).andExpect(status().isNotFound());
    }

    @Test
    void anotherAccountNeitherSeesNorChangesTheApartments() throws Exception {
        String created = mockMvc.perform(post("/api/properties/{id}/apartments", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn().getResponse().getContentAsString();
        String apartmentId = JsonPath.read(created, "$.id");
        String tenantId = JsonPath.read(created, "$.tenants[0].id");

        otherAccountMockMvc.perform(get("/api/properties/{id}/apartments", "1")).andExpect(status().isNotFound());
        otherAccountMockMvc.perform(post("/api/properties/{id}/apartments", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(post("/api/apartments/{id}/tenants", apartmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"Hans\", \"lastName\": \"Fremd\"}"))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(put("/api/tenants/{id}", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isNotFound());
        otherAccountMockMvc.perform(delete("/api/tenants/{id}", tenantId)).andExpect(status().isNotFound());

        mockMvc.perform(get("/api/properties/{id}/apartments", "1"))
                .andExpect(jsonPath("$[0].tenants", hasSize(1)));
    }
}
