package com.remo.realestatemaintainceoptimizer.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.remo.realestatemaintainceoptimizer.dto.AddressValidationResponse;
import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

        GeocodingResponse result = fetchByFreeTextQuery(address);
        if (result.latitude() == null) {
            String addressWithoutCityDistrict = withoutCityDistrictSuffix(address);
            if (!addressWithoutCityDistrict.equals(address)) {
                result = fetchByFreeTextQuery(addressWithoutCityDistrict);
            }
        }
        if (result.latitude() == null) {
            Matcher postalCodeMatcher = GERMAN_POSTAL_CODE.matcher(address);
            if (postalCodeMatcher.find()) {
                result = fetchByPostalCode(postalCodeMatcher.group());
            }
        }

        cache.put(address, result);
        return result;
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
        String streetLine = (street.trim() + " " + houseNumber.trim()).trim();

        List<NominatimResult> exactMatches = fetchStructured(streetLine, postalCode.trim(), city.trim(), 1);
        if (!exactMatches.isEmpty()) {
            AddressValidationResponse match = toMatch(exactMatches.get(0));
            if (match != null) return match;
        }

        List<NominatimResult> withoutPostalCode = fetchStructured(streetLine, null, city.trim(), 2);
        if (withoutPostalCode.size() == 1) {
            NominatimResult result = withoutPostalCode.get(0);
            Address address = result.address();
            if (address != null && address.postcode() != null && !address.postcode().equals(postalCode.trim())) {
                return AddressValidationResponse.suggestion(street.trim(), houseNumber.trim(), address.postcode(), cityOf(address, city));
            }
            AddressValidationResponse match = toMatch(result);
            if (match != null) return match;
        }

        return AddressValidationResponse.notFound();
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
        return fallback;
    }

    private synchronized List<NominatimResult> fetchStructured(String street, String postalCode, String city, int limit) {
        waitForRateLimit();
        try {
            NominatimResult[] results = restClient
                    .get()
                    .uri(uriBuilder -> {
                        uriBuilder = uriBuilder.path("/search").queryParam("street", street);
                        if (postalCode != null && !postalCode.isBlank()) {
                            uriBuilder = uriBuilder.queryParam("postalcode", postalCode);
                        }
                        return uriBuilder
                                .queryParam("city", city)
                                .queryParam("country", "Germany")
                                .queryParam("format", "json")
                                .queryParam("addressdetails", 1)
                                .queryParam("limit", limit)
                                .build();
                    })
                    .retrieve()
                    .body(NominatimResult[].class);
            return results == null ? List.of() : List.of(results);
        } catch (RestClientException exception) {
            return List.of();
        }
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
