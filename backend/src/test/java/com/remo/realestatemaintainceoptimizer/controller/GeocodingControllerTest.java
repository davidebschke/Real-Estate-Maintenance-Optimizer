package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.AddressValidationResponse;
import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.service.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the geocoding proxy REST API against a stubbed GeocodingService, without any real network call, and that it is only available to logged-in accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GeocodingControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc anonymousMockMvc;

    @MockitoBean
    private GeocodingService geocodingService;

    private MockMvc mockMvc;

    @BeforeEach
    void logIn() {
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, TestAccounts.saveRegularAccount(userRepository));
    }

    @Test
    void refusesToProxyLookupsWithoutALoggedInAccount() throws Exception {
        anonymousMockMvc.perform(get("/api/geocode").param("address", "Aachener Str. 512, 50933 Köln"))
                .andExpect(status().isUnauthorized());
        anonymousMockMvc.perform(get("/api/geocode/validate")
                        .param("street", "Aachener Str.")
                        .param("houseNumber", "512")
                        .param("postalCode", "50933")
                        .param("city", "Köln"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(geocodingService);
    }

    @Test
    void returnsTheCoordinatesResolvedByTheGeocodingService() throws Exception {
        when(geocodingService.geocode("Aachener Str. 512, 50933 Köln"))
                .thenReturn(new GeocodingResponse(50.9420135, 6.8771884));

        mockMvc.perform(get("/api/geocode").param("address", "Aachener Str. 512, 50933 Köln"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude", equalTo(50.9420135)))
                .andExpect(jsonPath("$.longitude", equalTo(6.8771884)));
    }

    @Test
    void returnsNullFieldsWhenTheAddressCannotBeResolved() throws Exception {
        when(geocodingService.geocode("Unbekannt")).thenReturn(new GeocodingResponse(null, null));

        mockMvc.perform(get("/api/geocode").param("address", "Unbekannt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude", nullValue()))
                .andExpect(jsonPath("$.longitude", nullValue()));
    }

    @Test
    void returnsTheSuggestionResolvedByTheGeocodingServiceForAnAddressValidation() throws Exception {
        when(geocodingService.validateAddress("Aachener Str.", "512", "99999", "Köln"))
                .thenReturn(AddressValidationResponse.suggestion("Aachener Str.", "512", "50933", "Köln"));

        mockMvc.perform(get("/api/geocode/validate")
                        .param("street", "Aachener Str.")
                        .param("houseNumber", "512")
                        .param("postalCode", "99999")
                        .param("city", "Köln"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("SUGGESTION")))
                .andExpect(jsonPath("$.suggestedPostalCode", equalTo("50933")))
                .andExpect(jsonPath("$.suggestedCity", equalTo("Köln")));
    }
}
