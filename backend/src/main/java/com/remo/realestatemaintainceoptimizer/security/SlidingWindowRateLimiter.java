package com.remo.realestatemaintainceoptimizer.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Counts attempts per key within a sliding time window, e.g. failed logins per username or demo accounts per client address.
 */
public class SlidingWindowRateLimiter {

    static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> attemptsByKey = new HashMap<>();

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
        if (attemptsByKey.size() >= MAX_TRACKED_KEYS) {
            evictExpired();
        }
        attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(clock.instant());
    }

    /**
     * Records one attempt for the given key unless it is already exhausted, returning whether the attempt was allowed.
     */
    public synchronized boolean tryAcquire(String key) {
        if (isExhausted(key)) {
            return false;
        }
        recordAttempt(key);
        return true;
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

    private void evictExpired() {
        attemptsByKey.values().forEach(this::removeExpired);
        attemptsByKey.values().removeIf(Deque::isEmpty);
    }

    private void removeExpired(Deque<Instant> attempts) {
        Instant windowStart = clock.instant().minus(window);
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(windowStart)) {
            attempts.removeFirst();
        }
    }
}
