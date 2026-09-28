package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies that {@code remo.frontend.*} properties bind correctly from application.yml.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FrontendPropertiesTest {

    @Autowired
    private FrontendProperties frontendProperties;

    @Test
    void bindsTheConfiguredFrontendBaseUrl() {
        assertThat(frontendProperties.baseUrl()).isEqualTo("http://localhost:5173");
    }
}
