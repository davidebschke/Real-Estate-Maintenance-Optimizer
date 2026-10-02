package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} background jobs such as the expired demo account cleanup and binds the appointment scheduling properties.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppointmentSchedulingProperties.class)
public class SchedulingConfig {
}
