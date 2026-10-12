package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

/**
 * Verifies that {@code remo.payments.*} binds with its defaults (due on the 5th, Claude Haiku 5.5, 5 MB statements) and that an invalid due day, model or limit is rejected at startup.
 */
class PaymentPropertiesTest {

    private static PaymentProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties)).bindOrCreate("remo.payments", PaymentProperties.class);
    }

    @Test
    void usesTheDocumentedDefaults() {
        PaymentProperties properties = bind(Map.of());

        assertThat(properties.dueDay()).isEqualTo(5);
        assertThat(properties.scanModel()).isEqualTo("claude-haiku-5-5");
        assertThat(properties.scanMaxTokens()).isEqualTo(8192);
        assertThat(properties.maxStatementSize()).isEqualTo(DataSize.ofMegabytes(5));
        assertThat(properties.maxScansPerAccountPerHour()).isEqualTo(10);
        assertThat(properties.maxScansPerDay()).isEqualTo(200);
        assertThat(properties.zone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void bindsTheConfiguredValues() {
        PaymentProperties properties = bind(Map.of(
                "remo.payments.due-day", "10",
                "remo.payments.scan-model", "claude-sonnet-5-5",
                "remo.payments.max-statement-size", "2MB",
                "remo.payments.max-scans-per-account-per-hour", "3"));

        assertThat(properties.dueDay()).isEqualTo(10);
        assertThat(properties.scanModel()).isEqualTo("claude-sonnet-5-5");
        assertThat(properties.maxStatementSize()).isEqualTo(DataSize.ofMegabytes(2));
        assertThat(properties.maxScansPerAccountPerHour()).isEqualTo(3);
    }

    @Test
    void rejectsADueDayOutsideTheDaysEveryMonthHas() {
        assertThatThrownBy(() -> bind(Map.of("remo.payments.due-day", "0")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bind(Map.of("remo.payments.due-day", "29")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABlankModelAndNonPositiveLimits() {
        assertThatThrownBy(() -> bind(Map.of("remo.payments.scan-model", " ")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bind(Map.of("remo.payments.scan-max-tokens", "0")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bind(Map.of("remo.payments.max-scans-per-day", "0")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bind(Map.of("remo.payments.max-statement-size", "0B")))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }
}
