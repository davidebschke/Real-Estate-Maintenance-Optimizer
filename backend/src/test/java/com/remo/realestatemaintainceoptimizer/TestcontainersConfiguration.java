package com.remo.realestatemaintainceoptimizer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Provides one throwaway PostgreSQL container, matching the production major version, shared as the datasource of every test context importing it.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGRES_CONTAINER =
            new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    /**
     * Returns the shared container without letting a closing context stop it, since Testcontainers removes it when the test JVM exits.
     */
    @Bean(destroyMethod = "")
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES_CONTAINER;
    }
}
