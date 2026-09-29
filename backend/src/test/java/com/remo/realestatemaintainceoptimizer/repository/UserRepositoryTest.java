package com.remo.realestatemaintainceoptimizer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies account persistence, username lookup and uniqueness, demo account counting, and the cascading deletion of expired demo accounts.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAllInBatch();
    }

    @Test
    void persistsAndReloadsEveryAccountField() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        repository.save(new User("user-1", "Debschke", "hash", "David Ebschke", true, now, now.plusSeconds(60), 3, 2));
        flushAndClear();

        User reloaded = repository.findById("user-1").orElseThrow();

        assertThat(reloaded.username()).isEqualTo("debschke");
        assertThat(reloaded.passwordHash()).isEqualTo("hash");
        assertThat(reloaded.displayName()).isEqualTo("David Ebschke");
        assertThat(reloaded.demoAccount()).isTrue();
        assertThat(reloaded.createdAt()).isEqualTo(now);
        assertThat(reloaded.expiresAt()).isEqualTo(now.plusSeconds(60));
        assertThat(reloaded.remainingPropertyCreations()).isEqualTo(3);
        assertThat(reloaded.remainingAppointmentCreations()).isEqualTo(2);
    }

    @Test
    void findsAnAccountByItsNormalizedUsername() {
        User account = TestAccounts.saveRegularAccount(repository);
        flushAndClear();

        assertThat(repository.findByUsername(account.username())).isPresent();
        assertThat(repository.findByUsername("unknown")).isEmpty();
    }

    @Test
    void rejectsASecondAccountWithTheSameUsernameInADifferentCase() {
        Instant now = Instant.now();
        repository.saveAndFlush(User.regular("user-1", "debschke", null, "David Ebschke", now));

        assertThatThrownBy(() -> repository.saveAndFlush(User.regular("user-2", "DEBSCHKE", null, "Someone Else", now)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countsOnlyDemoAccounts() {
        TestAccounts.saveRegularAccount(repository);
        TestAccounts.saveDemoAccount(repository, Instant.now().plusSeconds(60), 3, 3);
        TestAccounts.saveDemoAccount(repository, Instant.now().plusSeconds(60), 3, 3);

        assertThat(repository.countByDemoAccountTrue()).isEqualTo(2);
    }

    @Test
    void deletesOnlyExpiredDemoAccountsTogetherWithTheirData() {
        Instant now = Instant.now();
        User expiredDemo = TestAccounts.saveDemoAccount(repository, now.minusSeconds(1), 3, 3);
        User activeDemo = TestAccounts.saveDemoAccount(repository, now.plusSeconds(3600), 3, 3);
        User regular = TestAccounts.saveRegularAccount(repository);
        Property expiredProperty = propertyRepository.save(
                new Property("property-1", expiredDemo.id(), "Wohnanlage Sonnenhof", "Aachener Str. 512", "pi-building"));
        LocalDateTime start = LocalDateTime.of(2026, 8, 11, 13, 0);
        appointmentRepository.save(new Appointment(
                "appointment-1", null, "Kellerreinigung", expiredProperty, "", start, start.plusHours(1),
                false, false, null, List.of(), List.of(), null));
        flushAndClear();

        int deletedCount = repository.deleteDemoAccountsExpiredAt(now);
        flushAndClear();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(repository.findById(expiredDemo.id())).isEmpty();
        assertThat(repository.findById(activeDemo.id())).isPresent();
        assertThat(repository.findById(regular.id())).isPresent();
        assertThat(propertyRepository.findById("property-1")).isEmpty();
        assertThat(appointmentRepository.findById("appointment-1")).isEmpty();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
