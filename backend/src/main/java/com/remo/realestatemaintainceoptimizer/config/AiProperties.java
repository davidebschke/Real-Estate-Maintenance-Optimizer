package com.remo.realestatemaintainceoptimizer.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds the Claude (Anthropic) connection used by the AI appointment optimization (switch, API key, model, timeout, request limits) under the {@code remo.ai} prefix.
 */
@ConfigurationProperties(prefix = "remo.ai")
public record AiProperties(
        @DefaultValue("false") boolean enabled,
        String apiKey,
        @DefaultValue("claude-haiku-4-5") String model,
        @DefaultValue("60s") Duration timeout,
        @DefaultValue("4096") int maxTokens,
        @DefaultValue("5") int maxRunsPerAccountPerHour,
        @DefaultValue("200") int maxRunsPerDay) {

    public AiProperties {
        if (enabled && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException("remo.ai.api-key must be set while remo.ai.enabled is true");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("remo.ai.model must not be blank");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("remo.ai.timeout must be positive");
        }
        if (maxTokens < 1 || maxRunsPerAccountPerHour < 1 || maxRunsPerDay < 1) {
            throw new IllegalArgumentException("remo.ai limits must be at least 1");
        }
    }
}
