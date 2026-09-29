package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCredentialsException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies login credentials with brute-force protection, looks up the logged-in account and ends sessions.
 */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlidingWindowRateLimiter failedLoginsPerUsernameAndClient;
    private final SlidingWindowRateLimiter failedLoginsPerClient;
    private final String unmatchableHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.failedLoginsPerUsernameAndClient = new SlidingWindowRateLimiter(
                authProperties.maxFailedLoginsPerUsernameAndClient(), authProperties.failedLoginWindow(), Clock.systemUTC());
        this.failedLoginsPerClient = new SlidingWindowRateLimiter(
                authProperties.maxFailedLoginsPerClient(), authProperties.failedLoginWindow(), Clock.systemUTC());
        this.unmatchableHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Returns the account matching the given credentials, taking the same time whether or not the username exists.
     * Every attempt is reserved against the limits before the password is checked, so parallel guesses cannot slip
     * through; the per-username limit is keyed by client address too, so nobody can lock another client out of an account.
     */
    @Transactional(readOnly = true)
    public User authenticate(String username, String password, String clientAddress) {
        String normalizedUsername = User.normalizeUsername(username);
        String usernameAtClient = normalizedUsername + "@" + clientAddress;
        boolean usernameAttemptAllowed = failedLoginsPerUsernameAndClient.tryAcquire(usernameAtClient);
        boolean clientAttemptAllowed = failedLoginsPerClient.tryAcquire(clientAddress);
        if (!usernameAttemptAllowed || !clientAttemptAllowed) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_LOGIN_ATTEMPTS);
        }

        Optional<User> account = userRepository.findByUsername(normalizedUsername)
                .filter(candidate -> !candidate.isExpiredAt(Instant.now()));
        String storedHash = account.map(User::passwordHash).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(password, storedHash != null ? storedHash : unmatchableHash);

        if (storedHash == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        failedLoginsPerUsernameAndClient.reset(usernameAtClient);
        failedLoginsPerClient.releaseLatest(clientAddress);
        return account.get();
    }

    /**
     * Returns the account with the given id.
     */
    @Transactional(readOnly = true)
    public User getAccount(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new AccountNotFoundException(userId));
    }

    /**
     * Ends the sessions of the given account: a demo account is deleted together with all its data, a regular account
     * has every session started until now revoked, so an already issued token can no longer be used.
     */
    public void logout(String userId) {
        userRepository.findById(userId).ifPresent(account -> {
            if (account.demoAccount()) {
                userRepository.delete(account);
            } else {
                account.invalidateSessionsStartedBefore(JwtService.revocationInstant(Instant.now()));
            }
        });
    }
}
