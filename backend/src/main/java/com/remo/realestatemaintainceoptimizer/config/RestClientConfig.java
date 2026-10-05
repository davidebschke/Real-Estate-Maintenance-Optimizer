package com.remo.realestatemaintainceoptimizer.config;

import java.time.Duration;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Provides the {@link RestClient.Builder} used by services that call external HTTP APIs, a fresh instance per injection so one service's base URL and default headers (e.g. an API key) never leak into another service's client.
 */
@Configuration
public class RestClientConfig {

    private static final Duration EXTERNAL_CALL_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(EXTERNAL_CALL_TIMEOUT);
        requestFactory.setReadTimeout(EXTERNAL_CALL_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
