package com.remo.realestatemaintainceoptimizer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Verifies username normalization, expiry, and the per-resource creation limits of an account.
 */
class UserTest {

    private static final Instant NOW = Instant.parse("2026-09-29T10:00:00Z");

    @Test
    void storesTheUsernameTrimmedAndInLowerCase() {
        User user = User.regular("id", "  DEbschke ", null, "David Ebschke", NOW);

        assertThat(user.username()).isEqualTo("debschke");
    }

    @Test
    void aRegularAccountNeverExpiresAndHasNoCreationLimits() {
        User user = User.regular("id", "debschke", null, "David Ebschke", NOW);

        assertThat(user.isExpiredAt(NOW.plusSeconds(1_000_000))).isFalse();
        assertThat(user.remainingPropertyCreations()).isNull();
        assertThat(user.remainingAppointmentCreations()).isNull();
        assertThat(user.remainingTenantCreations()).isNull();
        for (int index = 0; index < 10; index++) {
            user.consumePropertyCreation();
            user.consumeAppointmentCreation();
        }
    }

    @Test
    void anAccountWithAnExpiryIsExpiredFromThatInstantOn() {
        User user = new User("id", "demo-1", null, "Demo", true, NOW, NOW.plusSeconds(60), 3, 3);

        assertThat(user.isExpiredAt(NOW.plusSeconds(59))).isFalse();
        assertThat(user.isExpiredAt(NOW.plusSeconds(60))).isTrue();
    }

    @Test
    void consumesPropertyCreationsUntilTheLimitIsExhausted() {
        User user = new User("id", "demo-1", null, "Demo", true, NOW, NOW.plusSeconds(60), 1, 5);

        user.consumePropertyCreation();

        assertThat(user.remainingPropertyCreations()).isZero();
        assertThatThrownBy(user::consumePropertyCreation)
                .isInstanceOf(CreationQuotaExceededException.class)
                .extracting("resource").isEqualTo(CreationQuotaExceededException.RESOURCE_PROPERTY);
        assertThat(user.remainingAppointmentCreations()).isEqualTo(5);
    }

    @Test
    void consumesTenantCreationsUntilTheLimitIsExhausted() {
        User user = new User("id", "demo-1", null, "Demo", true, NOW, NOW.plusSeconds(60), 5, 5, 1);

        user.consumeTenantCreation();

        assertThat(user.remainingTenantCreations()).isZero();
        assertThatThrownBy(user::consumeTenantCreation)
                .isInstanceOf(CreationQuotaExceededException.class)
                .extracting("resource").isEqualTo(CreationQuotaExceededException.RESOURCE_TENANT);
        assertThat(user.remainingPropertyCreations()).isEqualTo(5);
    }

    @Test
    void consumesAppointmentCreationsUntilTheLimitIsExhausted() {
        User user = new User("id", "demo-1", null, "Demo", true, NOW, NOW.plusSeconds(60), 5, 1);

        user.consumeAppointmentCreation();

        assertThat(user.remainingAppointmentCreations()).isZero();
        assertThatThrownBy(user::consumeAppointmentCreation)
                .isInstanceOf(CreationQuotaExceededException.class)
                .extracting("resource").isEqualTo(CreationQuotaExceededException.RESOURCE_APPOINTMENT);
    }

    @Test
    void acceptsEverySessionUntilSessionsAreRevokedAndThenOnlyLaterOnes() {
        User user = User.regular("id", "debschke", null, "David Ebschke", NOW);
        assertThat(user.acceptsSessionStartedAt(NOW.minusSeconds(3600))).isTrue();

        user.invalidateSessionsStartedBefore(NOW);

        assertThat(user.acceptsSessionStartedAt(NOW.minusMillis(1))).isFalse();
        assertThat(user.acceptsSessionStartedAt(NOW)).isTrue();
        assertThat(user.acceptsSessionStartedAt(NOW.plusSeconds(1))).isTrue();
    }

    @Test
    void replacesThePasswordHash() {
        User user = User.regular("id", "debschke", null, "David Ebschke", NOW);

        user.changePasswordHash("new-hash");

        assertThat(user.passwordHash()).isEqualTo("new-hash");
    }

    @Test
    void changingTheUsernameStoresItTrimmedAndInLowerCase() {
        User user = User.regular("id", "debschke", null, "David Ebschke", NOW);

        user.changeUsername("  New-NAME ");

        assertThat(user.username()).isEqualTo("new-name");
    }

    @Test
    void anAccountHasNoOwnAppointmentBufferUntilItSetsOne() {
        User user = User.regular("id", "debschke", null, "David Ebschke", NOW);
        assertThat(user.appointmentBufferMinutes()).isNull();

        user.changeAppointmentBufferMinutes(30);

        assertThat(user.appointmentBufferMinutes()).isEqualTo(30);
    }
}
