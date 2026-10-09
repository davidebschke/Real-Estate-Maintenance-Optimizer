package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Verifies that {@code remo.ai.*} binds with its defaults (switched off, Claude Haiku 4.5) and that an enabled AI without API key or with an invalid model, timeout or limit is rejected at startup.
 */
class AiPropertiesTest {

    private static AiProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties)).bindOrCreate("remo.ai", AiProperties.class);
    }

    @Test
    void theAiIsDisabledByDefaultAndUsesClaudeHaiku() {
        AiProperties properties = bind(Map.of());

        assertThat(properties.enabled()).isFalse();
        assertThat(properties.model()).isEqualTo("claude-haiku-4-5");
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.maxTokens()).isEqualTo(4096);
        assertThat(properties.maxRunsPerAccountPerHour()).isEqualTo(5);
        assertThat(properties.maxRunsPerDay()).isEqualTo(200);
    }

    @Test
    void bindsTheConfiguredValues() {
        AiProperties properties = bind(Map.of(
                "remo.ai.enabled", "true",
                "remo.ai.api-key", "secret",
                "remo.ai.model", "claude-sonnet-5-5",
                "remo.ai.timeout", "30s",
                "remo.ai.max-runs-per-account-per-hour", "2"));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.apiKey()).isEqualTo("secret");
        assertThat(properties.model()).isEqualTo("claude-sonnet-5-5");
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.maxRunsPerAccountPerHour()).isEqualTo(2);
    }

    @Test
    void rejectsAnEnabledAiWithoutAnApiKey() {
        assertThatThrownBy(() -> new AiProperties(true, null, "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiProperties(true, " ", "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankModelANonPositiveTimeoutOrLimit() {
        assertThatThrownBy(() -> new AiProperties(false, null, " ", Duration.ofSeconds(60), 4096, 5, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiProperties(false, null, "claude-haiku-4-5", Duration.ZERO, 4096, 5, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiProperties(false, null, "claude-haiku-4-5", Duration.ofSeconds(60), 0, 5, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiProperties(false, null, "claude-haiku-4-5", Duration.ofSeconds(60), 4096, 5, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
