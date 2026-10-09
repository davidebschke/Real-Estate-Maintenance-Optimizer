package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.security.UsernamePolicy;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates password-less, session-only demo accounts pre-filled with example data and removes them once expired.
 */
@Service
@Transactional
public class DemoAccountService {

    static final String DEMO_DISPLAY_NAME = "Demo";
    static final int DEMO_PROPERTY_CREATIONS = 3;
    static final int DEMO_APPOINTMENT_CREATIONS = 3;
    static final int DEMO_TENANT_CREATIONS = 10;

    static final long DEMO_ACCOUNT_CREATION_LOCK_KEY = 0x72656D6F64656D6FL;

    private static final int USERNAME_RANDOM_BYTES = 8;

    private final UserRepository userRepository;
    private final DemoDataSeeder demoDataSeeder;
    private final AuthProperties authProperties;
    private final SlidingWindowRateLimiter creationsPerClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public DemoAccountService(
            UserRepository userRepository, DemoDataSeeder demoDataSeeder, AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.demoDataSeeder = demoDataSeeder;
        this.authProperties = authProperties;
        this.creationsPerClient = new SlidingWindowRateLimiter(
                authProperties.maxDemoAccountsPerClient(), authProperties.demoAccountCreationWindow(), Clock.systemUTC());
    }

    /**
     * Creates a demo account that expires after one session and may create only a few more properties,
     * appointments and tenants, rejecting it when too many demo accounts exist or this client created too many recently; creations
     * are serialized by a database lock so concurrent requests cannot exceed the global limit together.
     */
    public User createDemoAccount(String clientAddress) {
        userRepository.acquireTransactionLock(DEMO_ACCOUNT_CREATION_LOCK_KEY);
        if (userRepository.countByDemoAccountTrue() >= authProperties.maxActiveDemoAccounts()) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_DEMO_CAPACITY_REACHED);
        }
        if (!creationsPerClient.tryAcquire(clientAddress)) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_DEMO_ACCOUNTS);
        }

        try {
            Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
            User account = userRepository.save(new User(
                    UUID.randomUUID().toString(),
                    UsernamePolicy.DEMO_PREFIX + randomSuffix(),
                    null,
                    DEMO_DISPLAY_NAME,
                    true,
                    createdAt,
                    createdAt.plus(authProperties.sessionDuration()),
                    DEMO_PROPERTY_CREATIONS,
                    DEMO_APPOINTMENT_CREATIONS,
                    DEMO_TENANT_CREATIONS));
            demoDataSeeder.seed(account.id(), LocalDate.now());
            return account;
        } catch (RuntimeException exception) {
            creationsPerClient.releaseLatest(clientAddress);
            throw exception;
        }
    }

    /**
     * Deletes every expired demo account together with all its data, returning how many were removed.
     */
    public int deleteExpiredDemoAccounts() {
        return userRepository.deleteDemoAccountsExpiredAt(Instant.now());
    }

    private String randomSuffix() {
        byte[] randomBytes = new byte[USERNAME_RANDOM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}
