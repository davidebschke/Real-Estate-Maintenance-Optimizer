package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies that the productive account receives its configured initial password exactly once, only as a BCrypt hash and only when it is long enough.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InitialAccountPasswordServiceTest {

    private static final String PASSWORD = "a-sufficiently-long-password";

    @Autowired
    private InitialAccountPasswordService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetInitialAccount() {
        userRepository.deleteAllInBatch();
        userRepository.save(User.regular(
                "initial-account", InitialAccountPasswordService.INITIAL_USERNAME, null, "David Ebschke", Instant.now()));
    }

    @Test
    void setsTheConfiguredPasswordAsABcryptHash() {
        boolean applied = service.applyInitialPassword(InitialAccountPasswordService.INITIAL_USERNAME, PASSWORD);

        String storedHash = loadInitialAccount().passwordHash();
        assertThat(applied).isTrue();
        assertThat(storedHash).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, storedHash)).isTrue();
    }

    @Test
    void neverOverwritesAnAlreadySetPassword() {
        service.applyInitialPassword(InitialAccountPasswordService.INITIAL_USERNAME, PASSWORD);

        boolean appliedAgain = service.applyInitialPassword(
                InitialAccountPasswordService.INITIAL_USERNAME, "another-long-enough-password");

        assertThat(appliedAgain).isFalse();
        assertThat(passwordEncoder.matches(PASSWORD, loadInitialAccount().passwordHash())).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "too-short"})
    void ignoresAMissingBlankOrTooShortPassword(String rawPassword) {
        boolean applied = service.applyInitialPassword(InitialAccountPasswordService.INITIAL_USERNAME, rawPassword);

        assertThat(applied).isFalse();
        assertThat(loadInitialAccount().passwordHash()).isNull();
    }

    @Test
    void ignoresAPasswordLongerThanBcryptSupportsInsteadOfFailingTheStartup() {
        String tooLongPassword = "ä".repeat(37);

        boolean applied = service.applyInitialPassword(InitialAccountPasswordService.INITIAL_USERNAME, tooLongPassword);

        assertThat(applied).isFalse();
        assertThat(loadInitialAccount().passwordHash()).isNull();
    }

    @Test
    void acceptsAPasswordOfExactlyTheMaximumBcryptLength() {
        String maximumLengthPassword = "a".repeat(InitialAccountPasswordService.MAX_PASSWORD_BYTES);

        assertThat(service.applyInitialPassword(InitialAccountPasswordService.INITIAL_USERNAME, maximumLengthPassword))
                .isTrue();
        assertThat(passwordEncoder.matches(maximumLengthPassword, loadInitialAccount().passwordHash())).isTrue();
    }

    @Test
    void ignoresAnUnknownAccount() {
        assertThat(service.applyInitialPassword("unknown", PASSWORD)).isFalse();
    }

    @Test
    void theConfiguredPasswordIsEmptyInTestsSoStartupLeavesTheAccountWithoutPassword() {
        service.applyConfiguredInitialPassword();

        assertThat(loadInitialAccount().passwordHash()).isNull();
    }

    private User loadInitialAccount() {
        return userRepository.findByUsername(InitialAccountPasswordService.INITIAL_USERNAME).orElseThrow();
    }
}
