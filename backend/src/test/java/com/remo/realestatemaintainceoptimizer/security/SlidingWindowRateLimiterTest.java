package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Verifies counting per key, the sliding expiry of old attempts, resetting, and the bounded number of tracked keys.
 */
class SlidingWindowRateLimiterTest {

    private final AdjustableClock clock = new AdjustableClock(Instant.parse("2026-09-29T10:00:00Z"));
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, Duration.ofMinutes(15), clock);

    @Test
    void isExhaustedOnlyOnceTheMaximumNumberOfAttemptsWasRecorded() {
        limiter.recordAttempt("key");
        limiter.recordAttempt("key");
        assertThat(limiter.isExhausted("key")).isFalse();

        limiter.recordAttempt("key");
        assertThat(limiter.isExhausted("key")).isTrue();
    }

    @Test
    void countsEveryKeySeparately() {
        limiter.recordAttempt("a");
        limiter.recordAttempt("a");
        limiter.recordAttempt("a");

        assertThat(limiter.isExhausted("a")).isTrue();
        assertThat(limiter.isExhausted("b")).isFalse();
    }

    @Test
    void forgetsAttemptsOnceTheyLeaveTheWindow() {
        limiter.recordAttempt("key");
        limiter.recordAttempt("key");
        clock.advance(Duration.ofMinutes(10));
        limiter.recordAttempt("key");
        assertThat(limiter.isExhausted("key")).isTrue();

        clock.advance(Duration.ofMinutes(6));

        assertThat(limiter.isExhausted("key")).isFalse();
    }

    @Test
    void tryAcquireAllowsAttemptsUntilExhaustedAndThenRefuses() {
        assertThat(limiter.tryAcquire("key")).isTrue();
        assertThat(limiter.tryAcquire("key")).isTrue();
        assertThat(limiter.tryAcquire("key")).isTrue();

        assertThat(limiter.tryAcquire("key")).isFalse();
    }

    @Test
    void resetForgetsEveryAttemptOfTheKey() {
        limiter.recordAttempt("key");
        limiter.recordAttempt("key");
        limiter.recordAttempt("key");

        limiter.reset("key");

        assertThat(limiter.isExhausted("key")).isFalse();
    }

    @Test
    void evictsExpiredKeysOnceTooManyKeysAreTracked() {
        for (int index = 0; index < SlidingWindowRateLimiter.MAX_TRACKED_KEYS; index++) {
            limiter.recordAttempt("key-" + index);
        }
        clock.advance(Duration.ofMinutes(16));

        limiter.recordAttempt("fresh-key");

        assertThat(limiter.trackedKeyCount()).isEqualTo(1);
    }

    private static final class AdjustableClock extends Clock {

        private Instant now;

        private AdjustableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
