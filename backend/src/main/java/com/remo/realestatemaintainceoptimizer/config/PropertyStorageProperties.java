package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds property-storage-related configuration under the {@code remo.storage.properties} prefix.
 */
@ConfigurationProperties(prefix = "remo.storage.properties")
public record PropertyStorageProperties(String directory) {
}
