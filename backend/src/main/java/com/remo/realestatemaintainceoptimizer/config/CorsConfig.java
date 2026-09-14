package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the configured frontend origin to call the backend REST API across origins.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String frontendBaseUrl;

    public CorsConfig(@Value("${remo.frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendBaseUrl)
                .allowedMethods("GET");
    }
}
