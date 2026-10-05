package com.remo.realestatemaintainceoptimizer.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.RouteCoordinate;
import com.remo.realestatemaintainceoptimizer.dto.RouteLeg;
import com.remo.realestatemaintainceoptimizer.dto.RouteResponse;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.service.RoutingService;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the route REST API against a stubbed RoutingService, without any real network call: authentication, input validation at the boundary and the HTTP mapping of the service's failures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RoutingControllerTest {

    private static final String TWO_STOPS =
            "{\"coordinates\":[{\"latitude\":50.94,\"longitude\":6.87},{\"latitude\":50.95,\"longitude\":6.93}]}";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc anonymousMockMvc;

    @MockitoBean
    private RoutingService routingService;

    private User account;
    private MockMvc mockMvc;

    @BeforeEach
    void logIn() {
        account = TestAccounts.saveRegularAccount(userRepository);
        mockMvc = TestAccounts.mockMvcAs(context, jwtService, account);
    }

    private static String stops(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> "{\"latitude\":50.9,\"longitude\":6.9}")
                .collect(Collectors.joining(",", "{\"coordinates\":[", "]}"));
    }

    @Test
    void refusesToCalculateRoutesWithoutALoggedInAccount() throws Exception {
        anonymousMockMvc.perform(post("/api/routes")
                        .with(TestAccounts.withCsrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_STOPS))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(routingService);
    }

    @Test
    void returnsTheRouteCalculatedForTheLoggedInAccount() throws Exception {
        when(routingService.route(eq(account.id()), any()))
                .thenReturn(new RouteResponse(
                        List.of(List.of(6.87, 50.94), List.of(6.93, 50.95)), 5000.5, 700.0, List.of(new RouteLeg(5000.5, 700.0))));

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(TWO_STOPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.geometry[0][0]", equalTo(6.87)))
                .andExpect(jsonPath("$.geometry[1][1]", equalTo(50.95)))
                .andExpect(jsonPath("$.distanceMeters", equalTo(5000.5)))
                .andExpect(jsonPath("$.durationSeconds", equalTo(700.0)))
                .andExpect(jsonPath("$.legs[0].distanceMeters", equalTo(5000.5)));

        verify(routingService)
                .route(account.id(), List.of(new RouteCoordinate(50.94, 6.87), new RouteCoordinate(50.95, 6.93)));
    }

    @Test
    void rejectsFewerThanTwoCoordinates() throws Exception {
        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(stops(1)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(stops(0)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routingService);
    }

    @Test
    void rejectsMoreCoordinatesThanTheUpperBound() throws Exception {
        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(stops(26)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routingService);
    }

    @Test
    void acceptsExactlyTheUpperBoundOfCoordinates() throws Exception {
        when(routingService.route(eq(account.id()), any()))
                .thenReturn(new RouteResponse(List.of(), 0, 0, List.of()));

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(stops(25)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsOutOfRangeAndMissingCoordinateValues() throws Exception {
        String[] invalidBodies = {
            "{\"coordinates\":[{\"latitude\":91,\"longitude\":6.9},{\"latitude\":50.9,\"longitude\":6.9}]}",
            "{\"coordinates\":[{\"latitude\":50.9,\"longitude\":-181},{\"latitude\":50.9,\"longitude\":6.9}]}",
            "{\"coordinates\":[{\"latitude\":50.9},{\"latitude\":50.9,\"longitude\":6.9}]}",
            "{\"coordinates\":[null,{\"latitude\":50.9,\"longitude\":6.9}]}",
            "{}",
        };

        for (String body : invalidBodies) {
            mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(routingService);
    }

    @Test
    void answersAnUnavailableRoutingWithALocalizedServiceUnavailable() throws Exception {
        when(routingService.route(eq(account.id()), any())).thenThrow(new RoutingUnavailableException("disabled"));

        mockMvc.perform(post("/api/routes")
                        .header("Accept-Language", "de")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TWO_STOPS))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", equalTo("Die Straßenroute konnte gerade nicht berechnet werden.")));
    }

    @Test
    void answersAnExhaustedRequestBudgetWithTooManyRequests() throws Exception {
        when(routingService.route(eq(account.id()), any()))
                .thenThrow(new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_ROUTE_REQUESTS));

        mockMvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content(TWO_STOPS))
                .andExpect(status().isTooManyRequests());
    }
}
