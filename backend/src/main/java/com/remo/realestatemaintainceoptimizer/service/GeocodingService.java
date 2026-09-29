package com.remo.realestatemaintainceoptimizer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.remo.realestatemaintainceoptimizer.dto.AddressValidationResponse;
import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

/**
 * Resolves postal addresses to geographic coordinates via the OpenStreetMap Nominatim API, cached per address.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final Duration MIN_DELAY_BETWEEN_REQUESTS = Duration.ofSeconds(1);
    private static final GeocodingResponse NOT_FOUND = new GeocodingResponse(null, null);
    private static final Pattern GERMAN_POSTAL_CODE = Pattern.compile("\\b\\d{5}\\b");
    private static final Pattern EXACT_GERMAN_POSTAL_CODE = Pattern.compile("^\\d{5}$");
    private static final Pattern CITY_DISTRICT_SUFFIX =
            Pattern.compile("(\\d{5}\\s+\\p{Lu}\\p{L}*)-\\p{Lu}\\p{L}*");

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
     * Resolves the given address to its coordinates, retrying without a trailing city-district suffix (e.g.
     * "Köln-Porz") and then falling back to a lookup by its German postal code alone if the full street address
     * has no exact match, returning null fields if none of these could find anything.
     */
    public GeocodingResponse geocode(String address) {
        GeocodingResponse cached = cache.get(address);
        if (cached != null) {
            return cached;
        }

        boolean anyRequestFailed = false;

        Optional<GeocodingResponse> attempt = attempt(() -> fetchByFreeTextQuery(address));
        GeocodingResponse result = attempt.orElse(NOT_FOUND);
        anyRequestFailed |= attempt.isEmpty();

        if (result.latitude() == null) {
            String addressWithoutCityDistrict = withoutCityDistrictSuffix(address);
            if (!addressWithoutCityDistrict.equals(address)) {
                attempt = attempt(() -> fetchByFreeTextQuery(addressWithoutCityDistrict));
                result = attempt.orElse(NOT_FOUND);
                anyRequestFailed |= attempt.isEmpty();
            }
        }
        if (result.latitude() == null) {
            Matcher postalCodeMatcher = GERMAN_POSTAL_CODE.matcher(address);
            if (postalCodeMatcher.find()) {
                attempt = attempt(() -> fetchByPostalCode(postalCodeMatcher.group()));
                result = attempt.orElse(NOT_FOUND);
                anyRequestFailed |= attempt.isEmpty();
            }
        }

        if (!anyRequestFailed) {
            cache.put(address, result);
        }
        return result;
    }

    /**
     * Runs the given Nominatim lookup, logging and reporting an empty result instead of throwing if the request
     * fails, so a transient failure is distinguishable from a genuine not-found and is never cached as one.
     */
    private Optional<GeocodingResponse> attempt(Supplier<GeocodingResponse> fetch) {
        try {
            return Optional.of(fetch.get());
        } catch (RestClientException exception) {
            log.warn("Nominatim request failed", exception);
            return Optional.empty();
        }
    }

    private String withoutCityDistrictSuffix(String address) {
        return CITY_DISTRICT_SUFFIX.matcher(address).replaceFirst("$1");
    }

    /**
     * Checks whether the given street/house-number/postal-code/city combination really exists, returning a single
     * unique correction suggestion (e.g. for a postal code that does not match an otherwise unique street) when one
     * can be derived, and a generic not-found result for anything ambiguous or entirely unresolvable.
     */
    public AddressValidationResponse validateAddress(String street, String houseNumber, String postalCode, String city) {
        String trimmedHouseNumber = houseNumber.trim();
        String trimmedPostalCode = postalCode.trim();
        String streetLine = (street.trim() + " " + trimmedHouseNumber).trim();

        if (EXACT_GERMAN_POSTAL_CODE.matcher(trimmedPostalCode).matches()) {
            List<NominatimResult> exactMatches = fetchStructured(streetLine, trimmedPostalCode, city.trim(), 1);
            if (!exactMatches.isEmpty()
                    && houseNumberMatches(exactMatches.get(0), trimmedHouseNumber)
                    && postalCodeMatches(exactMatches.get(0), trimmedPostalCode)) {
                AddressValidationResponse match = toMatch(exactMatches.get(0));
                if (match != null) return match;
            }
        }

        List<NominatimResult> withoutPostalCode = fetchStructured(streetLine, null, city.trim(), 2);
        if (withoutPostalCode.size() == 1) {
            NominatimResult result = withoutPostalCode.get(0);
            if (!houseNumberMatches(result, trimmedHouseNumber)) {
                return AddressValidationResponse.notFound();
            }
            Address address = result.address();
            if (address != null && address.postcode() != null && !address.postcode().equals(trimmedPostalCode)) {
                String suggestedStreet = address.road() != null ? address.road() : street.trim();
                return AddressValidationResponse.suggestion(
                        suggestedStreet, trimmedHouseNumber, address.postcode(), cityOf(address, city));
            }
            AddressValidationResponse match = toMatch(result);
            if (match != null) return match;
        }

        return AddressValidationResponse.notFound();
    }

    /**
     * Whether the given result's house number matches the requested one, trusting the structured search when
     * Nominatim did not return a house number to compare against.
     */
    private boolean houseNumberMatches(NominatimResult result, String requestedHouseNumber) {
        Address address = result.address();
        return address == null || address.houseNumber() == null || address.houseNumber().equals(requestedHouseNumber);
    }

    /**
     * Whether the given result's postal code matches the requested one, required to accept a "postalcode"-biased
     * structured search as a real match since Nominatim treats that parameter as a ranking hint, not a hard filter,
     * and can otherwise return the correct street/city with a different real postal code than the one requested.
     */
    private boolean postalCodeMatches(NominatimResult result, String requestedPostalCode) {
        Address address = result.address();
        return address == null || address.postcode() == null || address.postcode().equals(requestedPostalCode);
    }

    private AddressValidationResponse toMatch(NominatimResult result) {
        try {
            return AddressValidationResponse.match(Double.parseDouble(result.lat()), Double.parseDouble(result.lon()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String cityOf(Address address, String fallback) {
        if (address.city() != null) return address.city();
        if (address.town() != null) return address.town();
        if (address.village() != null) return address.village();
        return fallback.trim();
    }

    private List<NominatimResult> fetchStructured(String street, String postalCode, String city, int limit) {
        try {
            return fetchRaw(uriBuilder -> {
                uriBuilder = uriBuilder.path("/search").queryParam("street", street);
                if (postalCode != null && !postalCode.isBlank()) {
                    uriBuilder = uriBuilder.queryParam("postalcode", postalCode);
                }
                return uriBuilder.queryParam("city", city).queryParam("country", "Germany").queryParam("addressdetails", 1);
            }, limit);
        } catch (RestClientException exception) {
            log.warn("Nominatim request failed", exception);
            return List.of();
        }
    }

    private GeocodingResponse fetchByFreeTextQuery(String address) {
        return toGeocodingResponse(fetchRaw(uriBuilder -> uriBuilder.path("/search").queryParam("q", address), 1));
    }

    private GeocodingResponse fetchByPostalCode(String postalCode) {
        return toGeocodingResponse(fetchRaw(
                uriBuilder -> uriBuilder.path("/search").queryParam("postalcode", postalCode).queryParam("country", "Germany"),
                1));
    }

    private GeocodingResponse toGeocodingResponse(List<NominatimResult> results) {
        if (results.isEmpty()) {
            return NOT_FOUND;
        }
        try {
            return new GeocodingResponse(Double.parseDouble(results.get(0).lat()), Double.parseDouble(results.get(0).lon()));
        } catch (NumberFormatException exception) {
            return NOT_FOUND;
        }
    }

    /**
     * Runs the given Nominatim search query, rate-limited and cleared to a JSON result list; callers decide how to
     * degrade on a {@link RestClientException} since a failed request must not be cached as a genuine not-found.
     */
    private synchronized List<NominatimResult> fetchRaw(UnaryOperator<UriBuilder> query, int limit) {
        waitForRateLimit();
        NominatimResult[] results = restClient
                .get()
                .uri(uriBuilder -> query.apply(uriBuilder)
                        .queryParam("format", "json")
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(NominatimResult[].class);
        return results == null ? List.of() : List.of(results);
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

    private record NominatimResult(String lat, String lon, Address address) {
    }

    private record Address(
            String road,
            @JsonProperty("house_number") String houseNumber,
            String postcode,
            String city,
            String town,
            String village) {
    }
}
