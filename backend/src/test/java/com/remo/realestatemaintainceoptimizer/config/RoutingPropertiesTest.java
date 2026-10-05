package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Verifies that {@code remo.routing.*} binds with its defaults and that an enabled routing without API key or a non-positive request limit is rejected at startup.
 */
class RoutingPropertiesTest {

    private static RoutingProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bindOrCreate("remo.routing", RoutingProperties.class);
    }

    @Test
    void routingIsDisabledByDefaultAndNeedsNoApiKeyThen() {
        RoutingProperties properties = bind(Map.of());

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.baseUrl()).isEqualTo("https://api.openrouteservice.org");
        assertThat(properties.profile()).isEqualTo("driving-car");
        assertThat(properties.maxRequestsPerAccountPerMinute()).isEqualTo(20);
        assertThat(properties.maxRequestsPerMinute()).isEqualTo(30);
        assertThat(properties.maxRequestsPerDay()).isEqualTo(1500);
    }

    @Test
    void bindsTheConfiguredValues() {
        RoutingProperties properties = bind(Map.of(
                "remo.routing.enabled", "true",
                "remo.routing.api-key", "secret",
                "remo.routing.base-url", "https://ors.example",
                "remo.routing.profile", "driving-hgv",
                "remo.routing.max-requests-per-account-per-minute", "5",
                "remo.routing.max-requests-per-minute", "10",
                "remo.routing.max-requests-per-day", "100"));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.apiKey()).isEqualTo("secret");
        assertThat(properties.baseUrl()).isEqualTo("https://ors.example");
        assertThat(properties.profile()).isEqualTo("driving-hgv");
        assertThat(properties.maxRequestsPerAccountPerMinute()).isEqualTo(5);
        assertThat(properties.maxRequestsPerMinute()).isEqualTo(10);
        assertThat(properties.maxRequestsPerDay()).isEqualTo(100);
    }

    @Test
    void rejectsEnabledRoutingWithoutAnApiKey() {
        assertThatThrownBy(() -> new RoutingProperties(true, "https://ors.test", null, "driving-car", 20, 30, 1500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoutingProperties(true, "https://ors.test", "  ", "driving-car", 20, 30, 1500))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANonPositiveRequestLimit() {
        assertThatThrownBy(() -> new RoutingProperties(false, "https://ors.test", null, "driving-car", 0, 30, 1500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoutingProperties(false, "https://ors.test", null, "driving-car", 20, 0, 1500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RoutingProperties(false, "https://ors.test", null, "driving-car", 20, 30, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
