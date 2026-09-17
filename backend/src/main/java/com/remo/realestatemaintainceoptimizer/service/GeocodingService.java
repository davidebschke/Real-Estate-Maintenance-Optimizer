package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Resolves postal addresses to geographic coordinates via the OpenStreetMap Nominatim API, cached per address.
 */
@Service
public class GeocodingService {

    private static final Duration MIN_DELAY_BETWEEN_REQUESTS = Duration.ofSeconds(1);
    private static final GeocodingResponse NOT_FOUND = new GeocodingResponse(null, null);
    private static final Pattern GERMAN_POSTAL_CODE = Pattern.compile("\\b\\d{5}\\b");

    private final RestClient restClient;
    private final Map<String, GeocodingResponse> cache = new ConcurrentHashMap<>();
    private Instant nextRequestAt = Instant.EPOCH;

    public GeocodingService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://nominatim.openstreetmap.org")
                // Nominatim's usage policy blocks requests lacking an application-identifying User-Agent;
                // a browser cannot set this header itself, which is why this call is proxied server-side.
                .defaultHeader(
                        "User-Agent",
                        "RealEstateMaintenanceOptimizer/1.0 (+https://github.com/davidebschke/Real-Estate-Maintenance-Optimizer)")
                .build();
    }

    /**
     * Resolves the given address to its coordinates, falling back to a lookup by its German postal code alone
     * if the full street address has no exact match, returning null fields if even that could not be found.
     */
    public GeocodingResponse geocode(String address) {
        GeocodingResponse cached = cache.get(address);
        if (cached != null) {
            return cached;
        }

        GeocodingResponse result = fetchByFreeTextQuery(address);
        if (result.latitude() == null) {
            Matcher postalCodeMatcher = GERMAN_POSTAL_CODE.matcher(address);
            if (postalCodeMatcher.find()) {
                result = fetchByPostalCode(postalCodeMatcher.group());
            }
        }

        cache.put(address, result);
        return result;
    }

    private GeocodingResponse fetchByFreeTextQuery(String address) {
        return fetch(uriBuilder -> uriBuilder.path("/search").queryParam("q", address));
    }

    private GeocodingResponse fetchByPostalCode(String postalCode) {
        return fetch(uriBuilder -> uriBuilder
                .path("/search")
                .queryParam("postalcode", postalCode)
                .queryParam("country", "Germany"));
    }

    private synchronized GeocodingResponse fetch(UnaryOperator<UriBuilder> query) {
        waitForRateLimit();
        try {
            NominatimResult[] results = restClient
                    .get()
                    .uri(uriBuilder -> query.apply(uriBuilder)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(NominatimResult[].class);
            if (results == null || results.length == 0) {
                return NOT_FOUND;
            }
            return new GeocodingResponse(Double.parseDouble(results[0].lat()), Double.parseDouble(results[0].lon()));
        } catch (RestClientException | NumberFormatException exception) {
            return NOT_FOUND;
        }
    }

    private void waitForRateLimit() {
        Instant now = Instant.now();
        if (now.isBefore(nextRequestAt)) {
            try {
                Thread.sleep(Duration.between(now, nextRequestAt).toMillis());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        nextRequestAt = Instant.now().plus(MIN_DELAY_BETWEEN_REQUESTS);
    }

    private record NominatimResult(String lat, String lon) {
    }
}
