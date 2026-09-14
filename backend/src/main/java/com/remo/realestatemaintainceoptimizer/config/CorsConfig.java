package com.remo.realestatemaintainceoptimizer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the configured frontend origin to call the backend REST API across origins.
 */
@Configuration
@EnableConfigurationProperties(FrontendProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private final FrontendProperties frontendProperties;

    public CorsConfig(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendProperties.baseUrl())
                .allowedMethods("GET");
    }
}
