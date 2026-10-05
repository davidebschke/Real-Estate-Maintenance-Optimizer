package com.remo.realestatemaintainceoptimizer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.remo.realestatemaintainceoptimizer.config.RoutingProperties;
import com.remo.realestatemaintainceoptimizer.dto.RouteCoordinate;
import com.remo.realestatemaintainceoptimizer.dto.RouteLeg;
import com.remo.realestatemaintainceoptimizer.dto.RouteMode;
import com.remo.realestatemaintainceoptimizer.dto.RouteResponse;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calculates routes by car or on foot through ordered stops via the openrouteservice directions API, cached per mode and coordinate sequence and rate-limited per account and overall to protect the shared API quota.
 */
@Service
@EnableConfigurationProperties(RoutingProperties.class)
public class RoutingService {

    static final int MAX_CACHED_ROUTES = 500;
    static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    static final Duration DAILY_LIMIT_WINDOW = Duration.ofDays(1);
    private static final String ALL_ACCOUNTS_KEY = "all-accounts";
    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final RoutingProperties properties;
    private final RestClient restClient;
    private final SlidingWindowRateLimiter requestsPerAccount;
    private final SlidingWindowRateLimiter requestsOverall;
    private final SlidingWindowRateLimiter requestsPerDay;
    private final Map<RouteKey, RouteResponse> cache = new ConcurrentHashMap<>();

    public RoutingService(RoutingProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", properties.apiKey() == null ? "" : properties.apiKey())
                .build();
        this.requestsPerAccount = new SlidingWindowRateLimiter(
                properties.maxRequestsPerAccountPerMinute(), RATE_LIMIT_WINDOW, Clock.systemUTC());
        this.requestsOverall =
                new SlidingWindowRateLimiter(properties.maxRequestsPerMinute(), RATE_LIMIT_WINDOW, Clock.systemUTC());
        this.requestsPerDay =
                new SlidingWindowRateLimiter(properties.maxRequestsPerDay(), DAILY_LIMIT_WINDOW, Clock.systemUTC());
    }

    /**
     * Returns the route for the given mode through the given stops in order, throwing {@link RoutingDisabledException} while routing is switched off, {@link RateLimitExceededException} when a request budget is used up and {@link RoutingUnavailableException} when openrouteservice fails, so callers can degrade deliberately.
     */
    public RouteResponse route(String accountId, RouteMode mode, List<RouteCoordinate> coordinates) {
        if (!properties.enabled()) {
            throw new RoutingDisabledException();
        }
        RouteKey key = new RouteKey(mode, List.copyOf(coordinates));
        RouteResponse cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        acquireRequestBudget(accountId);

        RouteResponse route = fetchRoute(key);
        if (cache.size() >= MAX_CACHED_ROUTES) {
            cache.clear();
        }
        cache.put(key, route);
        return route;
    }

    private void acquireRequestBudget(String accountId) {
        if (!requestsPerAccount.tryAcquire(accountId)) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_ROUTE_REQUESTS);
        }
        if (!requestsOverall.tryAcquire(ALL_ACCOUNTS_KEY)) {
            requestsPerAccount.releaseLatest(accountId);
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_ROUTE_REQUESTS);
        }
        if (!requestsPerDay.tryAcquire(ALL_ACCOUNTS_KEY)) {
            requestsPerAccount.releaseLatest(accountId);
            requestsOverall.releaseLatest(ALL_ACCOUNTS_KEY);
            throw new RateLimitExceededException(RateLimitExceededException.REASON_ROUTE_QUOTA_EXHAUSTED);
        }
    }

    private RouteResponse fetchRoute(RouteKey key) {
        List<RouteCoordinate> stops = key.stops();
        List<List<Double>> orderedLongitudeLatitude = stops.stream()
                .map(stop -> List.of(stop.longitude(), stop.latitude()))
                .toList();
        DirectionsResponse response;
        try {
            response = restClient
                    .post()
                    .uri("/v2/directions/{profile}/geojson", key.mode().profile())
                    .body(Map.of("coordinates", orderedLongitudeLatitude))
                    .retrieve()
                    .body(DirectionsResponse.class);
        } catch (RestClientException exception) {
            log.warn("openrouteservice request failed", exception);
            throw new RoutingUnavailableException("openrouteservice request failed", exception);
        }
        return toRouteResponse(response, stops.size() - 1);
    }

    private RouteResponse toRouteResponse(DirectionsResponse response, int expectedLegCount) {
        if (response == null || response.features() == null || response.features().isEmpty()) {
            throw unusableResponse();
        }
        Feature feature = response.features().get(0);
        if (feature.geometry() == null
                || feature.geometry().coordinates() == null
                || feature.properties() == null
                || feature.properties().summary() == null
                || feature.properties().segments() == null
                || feature.properties().segments().size() != expectedLegCount) {
            throw unusableResponse();
        }
        List<RouteLeg> legs = feature.properties().segments().stream()
                .map(segment -> new RouteLeg(segment.distance(), segment.duration()))
                .toList();
        return new RouteResponse(
                feature.geometry().coordinates(),
                feature.properties().summary().distance(),
                feature.properties().summary().duration(),
                legs);
    }

    private RoutingUnavailableException unusableResponse() {
        log.warn("openrouteservice answered with an unusable route");
        return new RoutingUnavailableException("openrouteservice answered with an unusable route");
    }

    private record RouteKey(RouteMode mode, List<RouteCoordinate> stops) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DirectionsResponse(List<Feature> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Feature(Geometry geometry, FeatureProperties properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Geometry(List<List<Double>> coordinates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FeatureProperties(Summary summary, List<Summary> segments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Summary(double distance, double duration) {
    }
}
