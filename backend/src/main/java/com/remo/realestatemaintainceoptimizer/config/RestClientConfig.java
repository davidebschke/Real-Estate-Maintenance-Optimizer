package com.remo.realestatemaintainceoptimizer.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Provides the {@link RestClient.Builder} used by services that call external HTTP APIs.
 */
@Configuration
public class RestClientConfig {

    private static final Duration EXTERNAL_CALL_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(EXTERNAL_CALL_TIMEOUT);
        requestFactory.setReadTimeout(EXTERNAL_CALL_TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
