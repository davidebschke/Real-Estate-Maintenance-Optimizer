package com.remo.realestatemaintainceoptimizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Verifies counting per key, the sliding expiry of old attempts, resetting and releasing, atomicity under concurrency, and the bounded number of tracked keys.
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
    void releaseLatestGivesBackOnlyTheMostRecentAttempt() {
        limiter.recordAttempt("key");
        limiter.recordAttempt("key");
        limiter.recordAttempt("key");

        limiter.releaseLatest("key");

        assertThat(limiter.isExhausted("key")).isFalse();
        assertThat(limiter.tryAcquire("key")).isTrue();
        assertThat(limiter.isExhausted("key")).isTrue();
    }

    @Test
    void releasingTheLastAttemptForgetsTheKeyAndReleasingAnUnknownKeyDoesNothing() {
        limiter.recordAttempt("key");

        limiter.releaseLatest("key");
        limiter.releaseLatest("unknown");

        assertThat(limiter.trackedKeyCount()).isZero();
    }

    @Test
    void neverTracksMoreThanTheMaximumNumberOfKeysAndEvictsTheLeastRecentlyUsedOne() {
        limiter.recordAttempt("key-0");
        for (int index = 1; index < SlidingWindowRateLimiter.MAX_TRACKED_KEYS; index++) {
            limiter.recordAttempt("key-" + index);
        }
        limiter.recordAttempt("key-0");

        limiter.recordAttempt("one-key-too-many");

        assertThat(limiter.trackedKeyCount()).isEqualTo(SlidingWindowRateLimiter.MAX_TRACKED_KEYS);
        limiter.recordAttempt("key-0");
        assertThat(limiter.isExhausted("key-0")).isTrue();
        limiter.recordAttempt("key-1");
        limiter.recordAttempt("key-1");
        assertThat(limiter.isExhausted("key-1")).isFalse();
    }

    @Test
    void concurrentTryAcquireCallsNeverTogetherExceedTheLimit() throws InterruptedException {
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            for (int attempt = 0; attempt < 100; attempt++) {
                executor.submit(() -> {
                    start.await();
                    if (limiter.tryAcquire("key")) {
                        allowed.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(allowed.get()).isEqualTo(3);
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
