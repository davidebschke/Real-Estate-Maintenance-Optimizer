package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies that {@code remo.auth.*} properties bind from application.yml, with the test-only secret and without an initial password.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthPropertiesTest {

    @Autowired
    private AuthProperties authProperties;

    @Test
    void bindsTheSessionAndCookieSettings() {
        assertThat(authProperties.sessionDuration()).isEqualTo(Duration.ofHours(8));
        assertThat(authProperties.cookieSecure()).isTrue();
        assertThat(authProperties.jwtSecret()).isEqualTo("test-only-jwt-secret-that-is-long-enough-for-hs256");
        assertThat(authProperties.initialUserPassword()).isEmpty();
    }

    @Test
    void bindsTheRateLimits() {
        assertThat(authProperties.maxFailedLoginsPerUsername()).isEqualTo(5);
        assertThat(authProperties.maxFailedLoginsPerClient()).isEqualTo(20);
        assertThat(authProperties.failedLoginWindow()).isEqualTo(Duration.ofMinutes(15));
        assertThat(authProperties.maxDemoAccountsPerClient()).isEqualTo(10);
        assertThat(authProperties.demoAccountCreationWindow()).isEqualTo(Duration.ofHours(1));
        assertThat(authProperties.maxActiveDemoAccounts()).isEqualTo(100);
    }
}
