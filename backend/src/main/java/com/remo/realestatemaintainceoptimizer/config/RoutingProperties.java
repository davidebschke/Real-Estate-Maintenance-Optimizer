package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the openrouteservice configuration (switch, endpoint, API key, profile, request limits) under the {@code remo.routing} prefix; an enabled routing without an API key fails at startup.
 */
@ConfigurationProperties(prefix = "remo.routing")
public record RoutingProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://api.openrouteservice.org") String baseUrl,
        String apiKey,
        @DefaultValue("driving-car") String profile,
        @DefaultValue("20") int maxRequestsPerAccountPerMinute,
        @DefaultValue("30") int maxRequestsPerMinute) {

    public RoutingProperties {
        if (enabled && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException("remo.routing.api-key must be set while remo.routing.enabled is true");
        }
        if (maxRequestsPerAccountPerMinute < 1 || maxRequestsPerMinute < 1) {
            throw new IllegalArgumentException("remo.routing request limits must be at least 1");
        }
    }
}
