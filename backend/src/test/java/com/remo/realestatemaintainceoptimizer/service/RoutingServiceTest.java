package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.remo.realestatemaintainceoptimizer.config.RoutingProperties;
import com.remo.realestatemaintainceoptimizer.dto.RouteCoordinate;
import com.remo.realestatemaintainceoptimizer.dto.RouteResponse;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Verifies road route calculation against a mocked openrouteservice response, including the coordinate order, the API key header, caching, rate limiting and the translation of every failure into a {@link RoutingUnavailableException}.
 */
class RoutingServiceTest {

    private static final String DIRECTIONS_URL = "https://ors.test/v2/directions/driving-car/geojson";
    private static final String ROUTE_JSON = """
            {"type":"FeatureCollection","features":[{"type":"Feature",
             "geometry":{"type":"LineString","coordinates":[[6.87,50.94],[6.9,50.95],[6.93,50.96]]},
             "properties":{"summary":{"distance":9000.5,"duration":1200.0},
                           "segments":[{"distance":5000.5,"duration":700.0},{"distance":4000.0,"duration":500.0}]}}]}
            """;
    private static final List<RouteCoordinate> STOPS = List.of(
            new RouteCoordinate(50.94, 6.87), new RouteCoordinate(50.95, 6.9), new RouteCoordinate(50.96, 6.93));
    private static final List<RouteCoordinate> OTHER_STOPS =
            List.of(new RouteCoordinate(50.1, 6.1), new RouteCoordinate(50.2, 6.2), new RouteCoordinate(50.3, 6.3));

    private MockRestServiceServer mockServer;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
    }

    private RoutingService serviceWithLimits(int perAccountPerMinute, int overallPerMinute) {
        return new RoutingService(
                new RoutingProperties(true, "https://ors.test", "test-key", "driving-car", perAccountPerMinute, overallPerMinute),
                builder);
    }

    private RoutingService service() {
        return serviceWithLimits(20, 30);
    }

    @Test
    void sendsLongitudeBeforeLatitudeWithTheApiKeyAndMapsTheResponse() {
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "test-key"))
                .andExpect(jsonPath("$.coordinates[0][0]").value(6.87))
                .andExpect(jsonPath("$.coordinates[0][1]").value(50.94))
                .andExpect(jsonPath("$.coordinates.length()").value(3))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));

        RouteResponse route = service().route("account-1", STOPS);

        assertThat(route.geometry()).hasSize(3);
        assertThat(route.geometry().get(0)).containsExactly(6.87, 50.94);
        assertThat(route.distanceMeters()).isEqualTo(9000.5);
        assertThat(route.durationSeconds()).isEqualTo(1200.0);
        assertThat(route.legs()).hasSize(2);
        assertThat(route.legs().get(0).distanceMeters()).isEqualTo(5000.5);
        assertThat(route.legs().get(1).durationSeconds()).isEqualTo(500.0);
        mockServer.verify();
    }

    @Test
    void answersARepeatedCoordinateSequenceFromTheCacheWithoutASecondCall() {
        mockServer
                .expect(once(), requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        RoutingService service = service();

        RouteResponse first = service.route("account-1", STOPS);
        RouteResponse second = service.route("account-2", List.of(
                new RouteCoordinate(50.94, 6.87), new RouteCoordinate(50.95, 6.9), new RouteCoordinate(50.96, 6.93)));

        assertThat(second).isEqualTo(first);
        mockServer.verify();
    }

    @Test
    void callsOpenrouteserviceAgainForADifferentCoordinateSequence() {
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        RoutingService service = service();

        service.route("account-1", STOPS);
        service.route("account-1", OTHER_STOPS);

        mockServer.verify();
    }

    @Test
    void doesNotCacheAFailedCalculation() {
        mockServer.expect(requestTo(DIRECTIONS_URL)).andRespond(withServerError());
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        RoutingService service = service();

        assertThatThrownBy(() -> service.route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);
        assertThat(service.route("account-1", STOPS).legs()).hasSize(2);

        mockServer.verify();
    }

    @Test
    void reportsAnOpenrouteserviceServerErrorAsUnavailable() {
        mockServer.expect(requestTo(DIRECTIONS_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service().route("account-1", STOPS))
                .isInstanceOf(RoutingUnavailableException.class)
                .hasCauseInstanceOf(org.springframework.web.client.RestClientException.class);
    }

    @Test
    void reportsAnExceededOpenrouteserviceQuotaAsUnavailable() {
        mockServer.expect(requestTo(DIRECTIONS_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> service().route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);
    }

    @Test
    void reportsAnUnroutableCoordinateAsUnavailable() {
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":2010,\"message\":\"Could not find routable point\"}}"));

        assertThatThrownBy(() -> service().route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);
    }

    @Test
    void reportsATimeoutOrConnectionFailureAsUnavailable() {
        mockServer.expect(requestTo(DIRECTIONS_URL)).andRespond(withException(new IOException("Read timed out")));

        assertThatThrownBy(() -> service().route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);
    }

    @Test
    void reportsAnAnswerWithoutRouteFeaturesAsUnavailable() {
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service().route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);
    }

    @Test
    void reportsAnAnswerWithTheWrongNumberOfLegsAsUnavailable() {
        String oneLegOnly = """
                {"features":[{"geometry":{"coordinates":[[6.87,50.94],[6.93,50.96]]},
                 "properties":{"summary":{"distance":1.0,"duration":1.0},"segments":[{"distance":1.0,"duration":1.0}]}}]}
                """;
        mockServer.expect(requestTo(DIRECTIONS_URL)).andRespond(withSuccess(oneLegOnly, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service().route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);
    }

    @Test
    void doesNotCallOpenrouteserviceWhileRoutingIsDisabled() {
        RoutingService disabled = new RoutingService(
                new RoutingProperties(false, "https://ors.test", "", "driving-car", 20, 30), builder);

        assertThatThrownBy(() -> disabled.route("account-1", STOPS)).isInstanceOf(RoutingUnavailableException.class);

        mockServer.verify();
    }

    @Test
    void limitsTheRequestsOfASingleAccountButNotOfOthers() {
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        RoutingService service = serviceWithLimits(1, 30);

        service.route("account-1", STOPS);

        assertThatThrownBy(() -> service.route("account-1", OTHER_STOPS))
                .isInstanceOfSatisfying(
                        RateLimitExceededException.class,
                        exception -> assertThat(exception.reasonCode())
                                .isEqualTo(RateLimitExceededException.REASON_TOO_MANY_ROUTE_REQUESTS));
        service.route("account-2", OTHER_STOPS);
        mockServer.verify();
    }

    @Test
    void limitsTheRequestsOfAllAccountsTogetherAndGivesBackTheAccountsAttempt() {
        mockServer
                .expect(requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        RoutingService service = serviceWithLimits(1, 1);

        service.route("account-1", STOPS);

        assertThatThrownBy(() -> service.route("account-2", OTHER_STOPS))
                .isInstanceOf(RateLimitExceededException.class);
        mockServer.verify();
    }

    @Test
    void servesCachedRoutesEvenWhenTheRequestBudgetIsUsedUp() {
        mockServer
                .expect(once(), requestTo(DIRECTIONS_URL))
                .andRespond(withSuccess(ROUTE_JSON, MediaType.APPLICATION_JSON));
        RoutingService service = serviceWithLimits(1, 1);

        service.route("account-1", STOPS);

        assertThat(service.route("account-1", STOPS).legs()).hasSize(2);
        mockServer.verify();
    }
}
