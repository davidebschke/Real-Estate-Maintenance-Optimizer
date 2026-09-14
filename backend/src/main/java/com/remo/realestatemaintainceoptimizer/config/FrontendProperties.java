package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds frontend-related configuration under the {@code remo.frontend} prefix.
 */
@ConfigurationProperties(prefix = "remo.frontend")
public record FrontendProperties(String baseUrl) {
}
