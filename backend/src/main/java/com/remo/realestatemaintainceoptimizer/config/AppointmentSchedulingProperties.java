package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds appointment scheduling configuration, i.e. the minimum gap kept between two appointments, under the {@code remo.appointments} prefix.
 */
@ConfigurationProperties(prefix = "remo.appointments")
public record AppointmentSchedulingProperties(@DefaultValue("5") int bufferMinutes) {

    public static final int MAX_BUFFER_MINUTES = 24 * 60;

    public AppointmentSchedulingProperties {
        if (bufferMinutes < 0 || bufferMinutes > MAX_BUFFER_MINUTES) {
            throw new IllegalArgumentException(
                    "remo.appointments.buffer-minutes must be between 0 and " + MAX_BUFFER_MINUTES);
        }
    }

    /**
     * Returns the buffer an account's own setting resolves to, falling back to the configured default when the account has none.
     */
    public int bufferMinutesFor(Integer accountBufferMinutes) {
        return accountBufferMinutes != null ? accountBufferMinutes : bufferMinutes;
    }
}
