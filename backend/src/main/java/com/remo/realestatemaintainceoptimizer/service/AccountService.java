package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.AuthProperties;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.DemoAccountRestrictedException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCurrentPasswordException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidNewPasswordException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.UsernameAlreadyTakenException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import com.remo.realestatemaintainceoptimizer.security.SlidingWindowRateLimiter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes the profile settings of regular accounts, i.e. username, password and appointment buffer; demo accounts are rejected.
 */
@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlidingWindowRateLimiter passwordChangeAttemptsPerAccount;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordChangeAttemptsPerAccount = new SlidingWindowRateLimiter(
                authProperties.maxFailedLoginsPerUsernameAndClient(), authProperties.failedLoginWindow(), Clock.systemUTC());
    }

    /**
     * Renames the given regular account, rejecting a username that already belongs to another account; the running session stays valid since it identifies the account by id.
     */
    public User changeUsername(String userId, String newUsername) {
        User account = loadRegularAccount(userId);
        String normalizedUsername = User.normalizeUsername(newUsername);
        if (normalizedUsername.equals(account.username())) {
            return account;
        }
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new UsernameAlreadyTakenException(normalizedUsername);
        }
        account.changeUsername(normalizedUsername);
        return account;
    }

    /**
     * Replaces the password of the given regular account after verifying the current one and revokes every session started until now, so only a freshly issued session stays valid;
     * a wrong current password is counted against a per-account limit, so it cannot be guessed through this operation.
     */
    public User changePassword(String userId, String currentPassword, String newPassword) {
        User account = loadRegularAccount(userId);
        if (!passwordChangeAttemptsPerAccount.tryAcquire(userId)) {
            throw new RateLimitExceededException(RateLimitExceededException.REASON_TOO_MANY_PASSWORD_CHANGE_ATTEMPTS);
        }
        String storedHash = account.passwordHash();
        if (storedHash == null || !passwordEncoder.matches(currentPassword, storedHash)) {
            throw new InvalidCurrentPasswordException();
        }
        passwordChangeAttemptsPerAccount.reset(userId);
        requireAcceptableNewPassword(currentPassword, newPassword);

        account.changePasswordHash(passwordEncoder.encode(newPassword));
        account.invalidateSessionsStartedBefore(JwtService.revocationInstant(Instant.now()));
        return account;
    }

    /**
     * Sets the minimum gap in minutes kept between two appointments of the given regular account.
     */
    public User changeAppointmentBufferMinutes(String userId, int appointmentBufferMinutes) {
        User account = loadRegularAccount(userId);
        account.changeAppointmentBufferMinutes(appointmentBufferMinutes);
        return account;
    }

    private User loadRegularAccount(String userId) {
        User account = userRepository.findById(userId).orElseThrow(() -> new AccountNotFoundException(userId));
        if (account.demoAccount()) {
            throw new DemoAccountRestrictedException();
        }
        return account;
    }

    private static void requireAcceptableNewPassword(String currentPassword, String newPassword) {
        if (newPassword.length() < InitialAccountPasswordService.MIN_PASSWORD_LENGTH) {
            throw new InvalidNewPasswordException(InvalidNewPasswordException.REASON_TOO_SHORT);
        }
        if (newPassword.getBytes(StandardCharsets.UTF_8).length > InitialAccountPasswordService.MAX_PASSWORD_BYTES) {
            throw new InvalidNewPasswordException(InvalidNewPasswordException.REASON_TOO_LONG);
        }
        if (newPassword.equals(currentPassword)) {
            throw new InvalidNewPasswordException(InvalidNewPasswordException.REASON_UNCHANGED);
        }
    }
}
