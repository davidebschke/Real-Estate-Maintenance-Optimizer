package com.remo.realestatemaintainceoptimizer.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.config.RoutingProperties;
import com.remo.realestatemaintainceoptimizer.dto.RouteMode;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingDisabledException;
import com.remo.realestatemaintainceoptimizer.exception.RoutingUnavailableException;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
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
 * Calculates the road distances and driving times by car between every pair of a set of locations via the openrouteservice matrix API, cached per location set and limited per day to protect the shared API quota.
 */
@Service
@EnableConfigurationProperties({RoutingProperties.class, OptimizationProperties.class})
public class DistanceMatrixService {

    static final int MAX_CACHED_MATRICES = 100;
    static final int MIN_LOCATIONS_TO_REQUEST = 2;
    static final Duration DAILY_LIMIT_WINDOW = Duration.ofDays(1);
    private static final String ALL_ACCOUNTS_KEY = "all-accounts";
    private static final List<String> METRICS = List.of("distance", "duration");
    private static final Logger log = LoggerFactory.getLogger(DistanceMatrixService.class);

    private final RoutingProperties routingProperties;
    private final RestClient restClient;
    private final SlidingWindowRateLimiter requestsPerDay;
    private final Map<List<Location>, TravelMatrix> cache = new ConcurrentHashMap<>();

    public DistanceMatrixService(
            RoutingProperties routingProperties,
            OptimizationProperties optimizationProperties,
            RestClient.Builder restClientBuilder) {
        this.routingProperties = routingProperties;
        this.restClient = restClientBuilder
                .baseUrl(routingProperties.baseUrl())
                .defaultHeader("Authorization", routingProperties.apiKey() == null ? "" : routingProperties.apiKey())
                .build();
        this.requestsPerDay = new SlidingWindowRateLimiter(
                optimizationProperties.maxMatrixRequestsPerDay(), DAILY_LIMIT_WINDOW, Clock.systemUTC());
    }

    /**
     * Returns the travel matrix between the given locations, throwing {@link RoutingDisabledException} while routing is switched off, {@link RateLimitExceededException} when the daily request budget is used up and {@link RoutingUnavailableException} when openrouteservice fails; fewer than two distinct locations need no request at all.
     */
    public TravelMatrix matrix(List<Location> locations) {
        if (!routingProperties.enabled()) {
            throw new RoutingDisabledException();
        }
        List<Location> sortedLocations = locations.stream()
                .distinct()
                .sorted(Comparator.comparing(Location::id))
                .toList();
        List<String> locationIds = sortedLocations.stream().map(Location::id).toList();
        if (sortedLocations.size() < MIN_LOCATIONS_TO_REQUEST) {
            return TravelMatrix.withoutTravel(locationIds);
        }
        TravelMatrix cached = cache.get(sortedLocations);
        if (cached != null) {
            return cached;
        }
        if (!requestsPerDay.tryAcquire(ALL_ACCOUNTS_KEY)) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_ROUTE_QUOTA_EXHAUSTED);
        }

        TravelMatrix matrix = fetchMatrix(sortedLocations, locationIds);
        if (cache.size() >= MAX_CACHED_MATRICES) {
            cache.clear();
        }
        cache.put(sortedLocations, matrix);
        return matrix;
    }

    private TravelMatrix fetchMatrix(List<Location> locations, List<String> locationIds) {
        List<List<Double>> longitudeLatitude = locations.stream()
                .map(location -> List.of(location.longitude(), location.latitude()))
                .toList();
        MatrixResponse response;
        try {
            response = restClient
                    .post()
                    .uri("/v2/matrix/{profile}", RouteMode.CAR.profile())
                    .body(Map.of("locations", longitudeLatitude, "metrics", METRICS))
                    .retrieve()
                    .body(MatrixResponse.class);
        } catch (RestClientException exception) {
            log.warn("openrouteservice matrix request failed", exception);
            throw new RoutingUnavailableException("openrouteservice matrix request failed", exception);
        }
        if (response == null
                || !isSquare(response.distances(), locations.size())
                || !isSquare(response.durations(), locations.size())) {
            log.warn("openrouteservice answered with an unusable matrix");
            throw new RoutingUnavailableException("openrouteservice answered with an unusable matrix");
        }
        return new TravelMatrix(locationIds, response.distances(), response.durations());
    }

    private static boolean isSquare(List<List<Double>> rows, int size) {
        return rows != null && rows.size() == size && rows.stream().allMatch(row -> row != null && row.size() == size);
    }

    /**
     * A place to calculate travel between, identified by the id the resulting matrix is queried with.
     */
    public record Location(String id, double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MatrixResponse(List<List<Double>> distances, List<List<Double>> durations) {
    }
}
