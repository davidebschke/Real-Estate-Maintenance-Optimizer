package com.remo.realestatemaintainceoptimizer.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Binds the rent payment check (due day, statement scan model and limits) under the {@code remo.payments} prefix.
 */
@ConfigurationProperties(prefix = "remo.payments")
public record PaymentProperties(
        @DefaultValue("5") int dueDay,
        @DefaultValue("claude-haiku-5-5") String scanModel,
        @DefaultValue("8192") int scanMaxTokens,
        @DefaultValue("5MB") DataSize maxStatementSize,
        @DefaultValue("10") int maxScansPerAccountPerHour,
        @DefaultValue("200") int maxScansPerDay,
        @DefaultValue("Europe/Berlin") ZoneId zone) {

    public static final int FIRST_VALID_DUE_DAY = 1;
    public static final int LAST_VALID_DUE_DAY = 28;
    private static final int MIN_LIMIT = 1;

    public PaymentProperties {
        if (dueDay < FIRST_VALID_DUE_DAY || dueDay > LAST_VALID_DUE_DAY) {
            throw new IllegalArgumentException("remo.payments.due-day must lie between " + FIRST_VALID_DUE_DAY + " and " + LAST_VALID_DUE_DAY);
        }
        if (scanModel == null || scanModel.isBlank()) {
            throw new IllegalArgumentException("remo.payments.scan-model must not be blank");
        }
        if (scanMaxTokens < MIN_LIMIT || maxScansPerAccountPerHour < MIN_LIMIT || maxScansPerDay < MIN_LIMIT) {
            throw new IllegalArgumentException("remo.payments limits must be at least 1");
        }
        if (maxStatementSize.toBytes() < MIN_LIMIT) {
            throw new IllegalArgumentException("remo.payments.max-statement-size must be positive");
        }
    }
}
