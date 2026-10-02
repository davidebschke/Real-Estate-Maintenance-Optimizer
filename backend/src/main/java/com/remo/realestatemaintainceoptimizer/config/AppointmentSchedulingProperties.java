package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds appointment scheduling configuration, i.e. the minimum gap kept between two appointments, under the {@code remo.appointments} prefix.
 */
@ConfigurationProperties(prefix = "remo.appointments")
public record AppointmentSchedulingProperties(@DefaultValue("15") int bufferMinutes) {

    public AppointmentSchedulingProperties {
        if (bufferMinutes < 0) {
            throw new IllegalArgumentException("remo.appointments.buffer-minutes must not be negative");
        }
    }
}
