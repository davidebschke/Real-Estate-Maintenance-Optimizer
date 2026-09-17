package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import com.remo.realestatemaintainceoptimizer.service.GeocodingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the geocoding proxy REST API against a stubbed GeocodingService, without any real network call.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GeocodingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeocodingService geocodingService;

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
}
