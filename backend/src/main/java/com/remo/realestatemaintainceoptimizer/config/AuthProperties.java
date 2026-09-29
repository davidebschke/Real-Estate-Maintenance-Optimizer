package com.remo.realestatemaintainceoptimizer.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds authentication-related configuration (session JWT, cookie, initial account, rate limits) under the {@code remo.auth} prefix.
 */
@ConfigurationProperties(prefix = "remo.auth")
public record AuthProperties(
        String jwtSecret,
        @DefaultValue("8h") Duration sessionDuration,
        @DefaultValue("true") boolean cookieSecure,
        String initialUserPassword,
        @DefaultValue("5") int maxFailedLoginsPerUsernameAndClient,
        @DefaultValue("20") int maxFailedLoginsPerClient,
        @DefaultValue("15m") Duration failedLoginWindow,
        @DefaultValue("10") int maxDemoAccountsPerClient,
        @DefaultValue("1h") Duration demoAccountCreationWindow,
        @DefaultValue("100") int maxActiveDemoAccounts) {
}
