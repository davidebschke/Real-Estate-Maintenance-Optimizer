package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds appointment-storage-related configuration under the {@code remo.storage} prefix.
 */
@ConfigurationProperties(prefix = "remo.storage")
public record StorageProperties(String directory) {
}
