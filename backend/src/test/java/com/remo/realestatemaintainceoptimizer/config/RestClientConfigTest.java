package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Verifies the shared {@link RestClient.Builder} bounds an unresponsive external call in time instead of hanging indefinitely.
 */
class RestClientConfigTest {

    @Test
    void boundsAnUnresponsiveServerCallInsteadOfHangingIndefinitely() throws IOException {
        try (ServerSocket unresponsiveServer = new ServerSocket(0)) {
            RestClient restClient = new RestClientConfig()
                    .restClientBuilder()
                    .baseUrl("http://localhost:" + unresponsiveServer.getLocalPort())
                    .build();

            assertTimeoutPreemptively(
                    Duration.ofSeconds(10),
                    () -> assertThatExceptionOfType(ResourceAccessException.class)
                            .isThrownBy(() -> restClient.get().retrieve().toBodilessEntity()));
        }
    }

    @Test
    void handsEveryInjectionItsOwnBuilderSoOneServicesHeadersNeverLeakIntoAnother() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RestClientConfig.class)) {
            RestClient.Builder first = context.getBean(RestClient.Builder.class);
            RestClient.Builder second = context.getBean(RestClient.Builder.class);

            assertThat(first).isNotSameAs(second);
        }
    }
}
