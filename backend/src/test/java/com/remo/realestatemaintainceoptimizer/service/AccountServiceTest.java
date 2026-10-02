package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.AccountNotFoundException;
import com.remo.realestatemaintainceoptimizer.exception.DemoAccountRestrictedException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidCurrentPasswordException;
import com.remo.realestatemaintainceoptimizer.exception.InvalidNewPasswordException;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.exception.UsernameAlreadyTakenException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import com.remo.realestatemaintainceoptimizer.security.JwtService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies changing username, password and appointment buffer of regular accounts and their rejection for demo accounts against a real PostgreSQL database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountServiceTest {

    private static final String PASSWORD = "correct horse battery staple";
    private static final String NEW_PASSWORD = "another long passphrase";

    @Autowired
    private AccountService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User account;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        account = TestAccounts.saveRegularAccount(userRepository, passwordEncoder.encode(PASSWORD));
    }

    @Test
    void changingTheUsernameTrimsAndLowercasesIt() {
        User renamed = service.changeUsername(account.id(), "  Fresh-Name ");

        assertThat(renamed.username()).isEqualTo("fresh-name");
        assertThat(userRepository.findByUsername("fresh-name")).isPresent();
    }

    @Test
    void changingTheUsernameToTheOwnOneIsANoOp() {
        assertThat(service.changeUsername(account.id(), account.username()).username()).isEqualTo(account.username());
    }

    @Test
    void changingTheUsernameToOneOfAnotherAccountIsRejected() {
        User other = TestAccounts.saveRegularAccount(userRepository);

        assertThatThrownBy(() -> service.changeUsername(account.id(), other.username().toUpperCase()))
                .isInstanceOf(UsernameAlreadyTakenException.class);
    }

    @Test
    void changingThePasswordReplacesTheHashAndRevokesEarlierSessions() {
        Instant before = Instant.now();

        User changed = service.changePassword(account.id(), PASSWORD, NEW_PASSWORD);

        assertThat(passwordEncoder.matches(NEW_PASSWORD, changed.passwordHash())).isTrue();
        assertThat(changed.acceptsSessionStartedAt(JwtService.revocationInstant(before).minusSeconds(1))).isFalse();
        assertThat(changed.acceptsSessionStartedAt(Instant.now().plusSeconds(1))).isTrue();
    }

    @Test
    void changingThePasswordWithAWrongCurrentPasswordIsRejected() {
        assertThatThrownBy(() -> service.changePassword(account.id(), "wrong password", NEW_PASSWORD))
                .isInstanceOf(InvalidCurrentPasswordException.class);
    }

    @Test
    void changingThePasswordOfAnAccountWithoutPasswordIsRejected() {
        User withoutPassword = TestAccounts.saveRegularAccount(userRepository);

        assertThatThrownBy(() -> service.changePassword(withoutPassword.id(), "anything", NEW_PASSWORD))
                .isInstanceOf(InvalidCurrentPasswordException.class);
    }

    @Test
    void aNewPasswordViolatingThePolicyIsRejectedWithItsReason() {
        assertThatThrownBy(() -> service.changePassword(account.id(), PASSWORD, "short"))
                .isInstanceOfSatisfying(InvalidNewPasswordException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidNewPasswordException.REASON_TOO_SHORT));
        assertThatThrownBy(() -> service.changePassword(account.id(), PASSWORD, "ä".repeat(40)))
                .isInstanceOfSatisfying(InvalidNewPasswordException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidNewPasswordException.REASON_TOO_LONG));
        assertThatThrownBy(() -> service.changePassword(account.id(), PASSWORD, PASSWORD))
                .isInstanceOfSatisfying(InvalidNewPasswordException.class,
                        exception -> assertThat(exception.reasonCode()).isEqualTo(InvalidNewPasswordException.REASON_UNCHANGED));
    }

    @Test
    void aCorrectCurrentPasswordResetsTheCountOfFailedAttempts() {
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> service.changePassword(account.id(), "wrong password", NEW_PASSWORD))
                    .isInstanceOf(InvalidCurrentPasswordException.class);
        }
        service.changePassword(account.id(), PASSWORD, NEW_PASSWORD);

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.changePassword(account.id(), "wrong password", PASSWORD))
                    .isInstanceOf(InvalidCurrentPasswordException.class);
        }
    }

    @Test
    void tooManyWrongCurrentPasswordsAreRateLimited() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.changePassword(account.id(), "wrong password", NEW_PASSWORD))
                    .isInstanceOf(InvalidCurrentPasswordException.class);
        }

        assertThatThrownBy(() -> service.changePassword(account.id(), PASSWORD, NEW_PASSWORD))
                .isInstanceOfSatisfying(RateLimitExceededException.class, exception -> assertThat(exception.reasonCode())
                        .isEqualTo(RateLimitExceededException.REASON_TOO_MANY_PASSWORD_CHANGE_ATTEMPTS));
    }

    @Test
    void changingTheAppointmentBufferPersistsIt() {
        service.changeAppointmentBufferMinutes(account.id(), 30);

        assertThat(userRepository.findById(account.id()).orElseThrow().appointmentBufferMinutes()).isEqualTo(30);
    }

    @Test
    void demoAccountsAreRejectedForEveryChange() {
        User demo = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3);

        assertThatThrownBy(() -> service.changeUsername(demo.id(), "free-name")).isInstanceOf(DemoAccountRestrictedException.class);
        assertThatThrownBy(() -> service.changePassword(demo.id(), PASSWORD, NEW_PASSWORD))
                .isInstanceOf(DemoAccountRestrictedException.class);
        assertThatThrownBy(() -> service.changeAppointmentBufferMinutes(demo.id(), 30))
                .isInstanceOf(DemoAccountRestrictedException.class);
    }

    @Test
    void anUnknownAccountIsRejected() {
        assertThatThrownBy(() -> service.changeAppointmentBufferMinutes("unknown-id", 30))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
