package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} background jobs such as the expired demo account cleanup.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
