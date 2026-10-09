package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.config.RoutingProperties;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import com.remo.realestatemaintainceoptimizer.service.TravelMatrix.Travel;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Verifies the travel matrix calculation against a mocked openrouteservice matrix response: request shape, API key, mapping by location id, caching, the daily limit and the translation of every failure into a {@link RoutingUnavailableException}.
 */
class DistanceMatrixServiceTest {

    private static final String MATRIX_URL = "https://ors.test/v2/matrix/driving-car";
    private static final String MATRIX_JSON = """
            {"distances":[[0.0,5000.0],[5100.0,0.0]],"durations":[[0.0,600.0],[620.0,0.0]],"metadata":{}}
            """;
    private static final List<Location> LOCATIONS =
            List.of(new Location("west", 50.93, 6.89), new Location("east", 50.94, 6.97));

    private MockRestServiceServer mockServer;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
    }

    private DistanceMatrixService service(boolean routingEnabled, int maxRequestsPerDay) {
        return new DistanceMatrixService(
                new RoutingProperties(routingEnabled, "https://ors.test", routingEnabled ? "test-key" : null, 20, 30, 1500),
                new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500, 3, 60, 50,
                        maxRequestsPerDay, ZoneId.of("Europe/Berlin")),
                builder);
    }

    @Test
    void sendsTheLocationsSortedByIdWithLongitudeFirstAndMapsTheMatrixByLocationId() {
        mockServer.expect(requestTo(MATRIX_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "test-key"))
                .andExpect(jsonPath("$.locations[0][0]").value(6.97))
                .andExpect(jsonPath("$.locations[0][1]").value(50.94))
                .andExpect(jsonPath("$.metrics[0]").value("distance"))
                .andExpect(jsonPath("$.metrics[1]").value("duration"))
                .andRespond(withSuccess(MATRIX_JSON, MediaType.APPLICATION_JSON));

        TravelMatrix matrix = service(true, 400).matrix(LOCATIONS);

        assertThat(matrix.between("east", "west")).contains(new Travel(5000.0, 600.0));
        assertThat(matrix.between("west", "east")).contains(new Travel(5100.0, 620.0));
        mockServer.verify();
    }

    @Test
    void answersTheSameLocationSetFromTheCache() {
        mockServer.expect(once(), requestTo(MATRIX_URL)).andRespond(withSuccess(MATRIX_JSON, MediaType.APPLICATION_JSON));
        DistanceMatrixService service = service(true, 400);

        TravelMatrix first = service.matrix(LOCATIONS);
        TravelMatrix second = service.matrix(List.of(LOCATIONS.get(1), LOCATIONS.get(0)));

        assertThat(second).isSameAs(first);
        mockServer.verify();
    }

    @Test
    void needsNoRequestForFewerThanTwoLocations() {
        TravelMatrix matrix = service(true, 400).matrix(List.of(LOCATIONS.getFirst(), LOCATIONS.getFirst()));

        assertThat(matrix.between("west", "west")).contains(Travel.NONE);
        mockServer.verify();
    }

    @Test
    void refusesToCalculateWhileRoutingIsDisabled() {
        assertThatThrownBy(() -> service(false, 400).matrix(LOCATIONS)).isInstanceOf(RoutingDisabledException.class);
    }

    @Test
    void translatesAFailedRequestIntoAnUnavailableRouting() {
        mockServer.expect(requestTo(MATRIX_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service(true, 400).matrix(LOCATIONS))
                .isInstanceOf(RoutingUnavailableException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void rejectsAMatrixThatDoesNotMatchTheRequestedLocations() {
        mockServer.expect(requestTo(MATRIX_URL)).andRespond(withSuccess(
                "{\"distances\":[[0.0]],\"durations\":[[0.0,1.0],[1.0,0.0]]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service(true, 400).matrix(LOCATIONS)).isInstanceOf(RoutingUnavailableException.class);
    }

    @Test
    void stopsAtTheDailyRequestLimit() {
        mockServer.expect(once(), requestTo(MATRIX_URL)).andRespond(withSuccess(MATRIX_JSON, MediaType.APPLICATION_JSON));
        DistanceMatrixService service = service(true, 1);
        service.matrix(LOCATIONS);

        assertThatThrownBy(() -> service.matrix(List.of(LOCATIONS.getFirst(), new Location("north", 50.97, 6.95))))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("reasonCode").isEqualTo(RateLimitExceededException.REASON_ROUTE_QUOTA_EXHAUSTED);
    }
}
