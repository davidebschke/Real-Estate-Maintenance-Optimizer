package com.remo.realestatemaintainceoptimizer.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Counts attempts per key within a sliding time window, e.g. failed logins per client address, tracking at most
 * {@value #MAX_TRACKED_KEYS} keys by evicting the least recently used one.
 */
public class SlidingWindowRateLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attemptsByKey = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public SlidingWindowRateLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /**
     * Returns whether the given key already used up every attempt allowed within the current window.
     */
    public synchronized boolean isExhausted(String key) {
        Deque<Instant> attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return false;
        }
        removeExpired(attempts);
        if (attempts.isEmpty()) {
            attemptsByKey.remove(key);
            return false;
        }
        return attempts.size() >= maxAttempts;
    }

    /**
     * Records one attempt for the given key.
     */
    public synchronized void recordAttempt(String key) {
        attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(clock.instant());
    }

    /**
     * Atomically records one attempt for the given key unless it is already exhausted, returning whether the attempt
     * was allowed, so concurrent callers can never together exceed the limit.
     */
    public synchronized boolean tryAcquire(String key) {
        if (isExhausted(key)) {
            return false;
        }
        recordAttempt(key);
        return true;
    }

    /**
     * Gives back the most recently recorded attempt of the given key, e.g. one reserved by {@link #tryAcquire} for an operation that then succeeded or failed for an unrelated reason.
     */
    public synchronized void releaseLatest(String key) {
        Deque<Instant> attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return;
        }
        attempts.pollLast();
        if (attempts.isEmpty()) {
            attemptsByKey.remove(key);
        }
    }

    /**
     * Forgets every recorded attempt of the given key.
     */
    public synchronized void reset(String key) {
        attemptsByKey.remove(key);
    }

    /**
     * Returns how many keys currently have attempts recorded.
     */
    public synchronized int trackedKeyCount() {
        return attemptsByKey.size();
    }

    private void removeExpired(Deque<Instant> attempts) {
        Instant windowStart = clock.instant().minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(windowStart)) {
            attempts.removeFirst();
        }
    }
}
