package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives the productive account created by the database migration its password from configuration, so the password never appears in the repository.
 */
@Service
@Transactional
public class InitialAccountPasswordService {

    static final String INITIAL_USERNAME = "debschke";
    static final int MIN_PASSWORD_LENGTH = 12;

    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAccountPasswordService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public InitialAccountPasswordService(
            UserRepository userRepository, PasswordEncoder passwordEncoder, AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    /**
     * Applies the configured initial password to the productive account once the application has started.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applyConfiguredInitialPassword() {
        applyInitialPassword(INITIAL_USERNAME, authProperties.initialUserPassword());
    }

    /**
     * Sets the given password on the given account only while it has none yet and the password is long enough,
     * returning whether it was set; an already set password is never overwritten.
     */
    public boolean applyInitialPassword(String username, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            LOGGER.warn("Initial password for account '{}' ignored: it must have at least {} characters",
                    username, MIN_PASSWORD_LENGTH);
            return false;
        }

        User account = userRepository.findByUsername(User.normalizeUsername(username)).orElse(null);
        if (account == null) {
            LOGGER.warn("Initial password ignored: account '{}' does not exist", username);
            return false;
        }
        if (account.passwordHash() != null) {
            return false;
        }

        account.changePasswordHash(passwordEncoder.encode(rawPassword));
        LOGGER.info("Initial password set for account '{}'", username);
        return true;
    }
}
