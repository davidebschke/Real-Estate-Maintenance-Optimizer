package com.remo.realestatemaintainceoptimizer.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Allows the configured frontend origin to call the backend REST API across origins, including the session and CSRF cookies.
 */
@Configuration
@EnableConfigurationProperties(FrontendProperties.class)
public class CorsConfig {

    private final FrontendProperties frontendProperties;

    public CorsConfig(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
    }

    /**
     * Provides the CORS rules applied by the Spring Security filter chain before any authentication check.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendProperties.baseUrl()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
