package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.remo.realestatemaintainceoptimizer.dto.AddressValidationResponse;
import com.remo.realestatemaintainceoptimizer.dto.AddressValidationStatus;
import com.remo.realestatemaintainceoptimizer.dto.GeocodingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Verifies address geocoding against a mocked Nominatim HTTP response, including the city-district-suffix and
 * postal-code fallbacks, error handling, per-address caching, and structured address validation.
 */
class GeocodingServiceTest {

    private MockRestServiceServer mockServer;
    private GeocodingService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new GeocodingService(builder);
    }

    @Test
    void resolvesAnAddressUsingTheFreeTextQuery() {
        mockServer
                .expect(requestTo(containsString("/search?q=Aachener")))
                .andExpect(header("User-Agent", containsString("RealEstateMaintenanceOptimizer")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"50.9420135\",\"lon\":\"6.8771884\"}]", MediaType.APPLICATION_JSON));

        GeocodingResponse response = service.geocode("Aachener Str. 512, 50933 Köln");

        assertThat(response.latitude()).isEqualTo(50.9420135);
        assertThat(response.longitude()).isEqualTo(6.8771884);
    }

    @Test
    void fallsBackToTheAddressWithoutTheCityDistrictSuffixWhenTheFullAddressHasNoMatch() {
        mockServer
                .expect(requestTo(containsString("q=Subbelrather%20Str.%20121,%2050868%20K%C3%B6ln-Porz")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("q=Subbelrather%20Str.%20121,%2050868%20K%C3%B6ln&")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"50.9504085\",\"lon\":\"6.9276608\"}]", MediaType.APPLICATION_JSON));

        GeocodingResponse response = service.geocode("Subbelrather Str. 121, 50868 Köln-Porz");

        assertThat(response.latitude()).isEqualTo(50.9504085);
        assertThat(response.longitude()).isEqualTo(6.9276608);
    }

    @Test
    void fallsBackToThePostalCodeWhenNeitherTheFullAddressNorItsCityMatch() {
        mockServer
                .expect(requestTo(containsString("q=Aachener%20Str.%20512,%2050933%20K%C3%B6ln-Braunsenfeld")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("q=Aachener%20Str.%20512,%2050933%20K%C3%B6ln&")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("postalcode=50933")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"50.9420135\",\"lon\":\"6.8771884\"}]", MediaType.APPLICATION_JSON));

        GeocodingResponse response = service.geocode("Aachener Str. 512, 50933 Köln-Braunsenfeld");

        assertThat(response.latitude()).isEqualTo(50.9420135);
        assertThat(response.longitude()).isEqualTo(6.8771884);
    }

    @Test
    void returnsNullFieldsWhenNoLookupFindsAMatch() {
        mockServer.expect(requestTo(containsString("q=Unbekannt"))).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("postalcode=99999")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        GeocodingResponse response = service.geocode("Unbekannt 1, 99999 Nirgendwo");

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
    }

    @Test
    void returnsNullFieldsInsteadOfThrowingWhenTheRequestFails() {
        mockServer.expect(requestTo(containsString("q=Fehler"))).andRespond(withServerError());
        mockServer.expect(requestTo(containsString("postalcode=12345"))).andRespond(withServerError());

        GeocodingResponse response = service.geocode("Fehler 1, 12345 Nirgendwo");

        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
    }

    @Test
    void cachesTheResultPerAddressInsteadOfRequestingItAgain() {
        mockServer
                .expect(requestTo(containsString("q=Aachener")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"50.9420135\",\"lon\":\"6.8771884\"}]", MediaType.APPLICATION_JSON));

        service.geocode("Aachener Str. 512, 50933 Köln");
        service.geocode("Aachener Str. 512, 50933 Köln");

        mockServer.verify();
    }

    @Test
    void validatesAnAddressThatExistsExactlyAsEntered() {
        mockServer
                .expect(requestTo(containsString("street=Aachener%20Str.%20512")))
                .andExpect(requestTo(containsString("postalcode=50933")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"50.9420135\",\"lon\":\"6.8771884\"}]", MediaType.APPLICATION_JSON));

        AddressValidationResponse response = service.validateAddress("Aachener Str.", "512", "50933", "Köln");

        assertThat(response.status()).isEqualTo(AddressValidationStatus.MATCH);
        assertThat(response.latitude()).isEqualTo(50.9420135);
        assertThat(response.longitude()).isEqualTo(6.8771884);
    }

    @Test
    void suggestsTheCorrectPostalCodeWhenTheStreetIsOtherwiseUnique() {
        mockServer
                .expect(requestTo(containsString("street=Aachener%20Str.%20512")))
                .andExpect(requestTo(containsString("postalcode=99999")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("street=Aachener%20Str.%20512")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"50.9420135\",\"lon\":\"6.8771884\",\"address\":"
                                + "{\"road\":\"Aachener Straße\",\"house_number\":\"512\",\"postcode\":\"50933\",\"city\":\"Köln\"}}]",
                        MediaType.APPLICATION_JSON));

        AddressValidationResponse response = service.validateAddress("Aachener Str.", "512", "99999", "Köln");

        assertThat(response.status()).isEqualTo(AddressValidationStatus.SUGGESTION);
        assertThat(response.suggestedStreet()).isEqualTo("Aachener Str.");
        assertThat(response.suggestedHouseNumber()).isEqualTo("512");
        assertThat(response.suggestedPostalCode()).isEqualTo("50933");
        assertThat(response.suggestedCity()).isEqualTo("Köln");
    }

    @Test
    void returnsNotFoundWhenTheStreetMatchesSeveralPossibleAddresses() {
        mockServer
                .expect(requestTo(containsString("street=Hauptstr.%201")))
                .andExpect(requestTo(containsString("postalcode=99999")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("street=Hauptstr.%201")))
                .andRespond(withSuccess(
                        "[{\"lat\":\"1\",\"lon\":\"1\"},{\"lat\":\"2\",\"lon\":\"2\"}]", MediaType.APPLICATION_JSON));

        AddressValidationResponse response = service.validateAddress("Hauptstr.", "1", "99999", "Musterstadt");

        assertThat(response.status()).isEqualTo(AddressValidationStatus.NOT_FOUND);
    }

    @Test
    void returnsNotFoundWhenTheAddressCannotBeResolvedAtAll() {
        mockServer
                .expect(requestTo(containsString("street=Unbekannt%201")))
                .andExpect(requestTo(containsString("postalcode=99999")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        mockServer
                .expect(requestTo(containsString("street=Unbekannt%201")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        AddressValidationResponse response = service.validateAddress("Unbekannt", "1", "99999", "Nirgendwo");

        assertThat(response.status()).isEqualTo(AddressValidationStatus.NOT_FOUND);
    }
}
