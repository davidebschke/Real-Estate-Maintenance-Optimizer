package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCredentialsException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies password verification, brute-force limits per username and client and per client (also under parallel attempts), account lookup and logout against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthServiceTest {

    private static final String PASSWORD = "correct horse battery staple";

    @Autowired
    private AuthService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User account;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        account = TestAccounts.saveRegularAccount(userRepository, passwordEncoder.encode(PASSWORD));
    }

    @Test
    void returnsTheAccountForMatchingCredentials() {
        User authenticated = service.authenticate(account.username(), PASSWORD, TestAccounts.uniqueClientAddress());

        assertThat(authenticated.id()).isEqualTo(account.id());
    }

    @Test
    void matchesTheUsernameCaseInsensitivelyAndIgnoresSurroundingWhitespace() {
        String enteredUsername = "  " + account.username().toUpperCase() + " ";

        User authenticated = service.authenticate(enteredUsername, PASSWORD, TestAccounts.uniqueClientAddress());

        assertThat(authenticated.id()).isEqualTo(account.id());
    }

    @Test
    void rejectsAWrongPassword() {
        assertThatThrownBy(() -> service.authenticate(account.username(), "wrong password", TestAccounts.uniqueClientAddress()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAnUnknownUsernameTheSameWayAsAWrongPassword() {
        assertThatThrownBy(() -> service.authenticate("nobody", PASSWORD, TestAccounts.uniqueClientAddress()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAnAccountWithoutPasswordEvenForAnEmptyPassword() {
        User withoutPassword = TestAccounts.saveRegularAccount(userRepository);

        assertThatThrownBy(() -> service.authenticate(withoutPassword.username(), "", TestAccounts.uniqueClientAddress()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsAnExpiredAccountEvenWithTheCorrectPassword() {
        User expired = userRepository.save(new User(
                "expired-user", "expired-user", passwordEncoder.encode(PASSWORD), "Expired", false,
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(60), null, null));

        assertThatThrownBy(() -> service.authenticate(expired.username(), PASSWORD, TestAccounts.uniqueClientAddress()))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void locksAUsernameForAClientAfterTooManyFailedAttemptsEvenForTheCorrectPassword() {
        String clientAddress = TestAccounts.uniqueClientAddress();
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.authenticate(account.username(), "wrong", clientAddress))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> service.authenticate(account.username(), PASSWORD, clientAddress))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("reasonCode").isEqualTo(RateLimitExceededException.REASON_TOO_MANY_LOGIN_ATTEMPTS);
    }

    @Test
    void anAttackerLockingAUsernameFromTheirClientCannotLockOutTheOwnerOnAnotherClient() {
        String attackerAddress = TestAccounts.uniqueClientAddress();
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.authenticate(account.username(), "wrong", attackerAddress))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThat(service.authenticate(account.username(), PASSWORD, TestAccounts.uniqueClientAddress()).id())
                .isEqualTo(account.id());
    }

    @Test
    void parallelWrongPasswordsCannotExceedTheLimitWhileTheirHashesAreStillBeingChecked() throws InterruptedException {
        String clientAddress = TestAccounts.uniqueClientAddress();
        int parallelAttempts = 15;
        AtomicInteger checkedPasswords = new AtomicInteger();
        AtomicInteger rateLimitedAttempts = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(parallelAttempts)) {
            for (int attempt = 0; attempt < parallelAttempts; attempt++) {
                executor.submit(() -> {
                    start.await();
                    try {
                        service.authenticate(account.username(), "wrong", clientAddress);
                    } catch (InvalidCredentialsException exception) {
                        checkedPasswords.incrementAndGet();
                    } catch (RateLimitExceededException exception) {
                        rateLimitedAttempts.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(checkedPasswords.get()).isEqualTo(5);
        assertThat(rateLimitedAttempts.get()).isEqualTo(parallelAttempts - 5);
    }

    @Test
    void blocksAClientAfterTooManyFailedAttemptsAcrossDifferentUsernames() {
        String clientAddress = TestAccounts.uniqueClientAddress();
        for (int attempt = 0; attempt < 20; attempt++) {
            String username = "unknown-" + attempt;
            assertThatThrownBy(() -> service.authenticate(username, "wrong", clientAddress))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> service.authenticate(account.username(), PASSWORD, clientAddress))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(service.authenticate(account.username(), PASSWORD, TestAccounts.uniqueClientAddress()).id())
                .isEqualTo(account.id());
    }

    @Test
    void aSuccessfulLoginResetsTheFailedAttemptsOfItsUsername() {
        String clientAddress = TestAccounts.uniqueClientAddress();
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.authenticate(account.username(), "wrong", clientAddress))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        service.authenticate(account.username(), PASSWORD, clientAddress);

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.authenticate(account.username(), "wrong", clientAddress))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
        assertThat(service.authenticate(account.username(), PASSWORD, clientAddress).id()).isEqualTo(account.id());
    }

    @Test
    void aSuccessfulLoginDoesNotCountAgainstTheLimitOfItsClient() {
        String clientAddress = TestAccounts.uniqueClientAddress();
        for (int attempt = 0; attempt < 19; attempt++) {
            String username = "unknown-" + attempt;
            assertThatThrownBy(() -> service.authenticate(username, "wrong", clientAddress))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        service.authenticate(account.username(), PASSWORD, clientAddress);

        assertThatThrownBy(() -> service.authenticate("unknown-last", "wrong", clientAddress))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.authenticate(account.username(), PASSWORD, clientAddress))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void returnsTheAccountWithTheGivenId() {
        assertThat(service.getAccount(account.id()).username()).isEqualTo(account.username());
    }

    @Test
    void throwsWhenTheAccountNoLongerExists() {
        assertThatThrownBy(() -> service.getAccount("deleted-account")).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void loggingOutADemoAccountDeletesItWithAllItsData() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3);
        propertyRepository.save(new Property("demo-property", demo.id(), "Demo-Objekt", "Demostr. 1", "pi-building"));

        service.logout(demo.id());

        assertThat(userRepository.findById(demo.id())).isEmpty();
        assertThat(propertyRepository.findById("demo-property")).isEmpty();
    }

    @Test
    void loggingOutARegularAccountKeepsItAndItsDataButRevokesItsEarlierSessions() {
        propertyRepository.save(new Property("property-1", account.id(), "Objekt", "Str. 1", "pi-building"));
        Instant sessionStartedBeforeLogout = Instant.now().minusSeconds(1);

        service.logout(account.id());

        User reloaded = userRepository.findById(account.id()).orElseThrow();
        assertThat(propertyRepository.findById("property-1")).isPresent();
        assertThat(reloaded.acceptsSessionStartedAt(sessionStartedBeforeLogout)).isFalse();
        assertThat(reloaded.acceptsSessionStartedAt(Instant.now().plusSeconds(1))).isTrue();
    }

    @Test
    void loggingOutAnAlreadyDeletedAccountDoesNothing() {
        service.logout("deleted-account");

        assertThat(userRepository.count()).isEqualTo(1);
    }
}
