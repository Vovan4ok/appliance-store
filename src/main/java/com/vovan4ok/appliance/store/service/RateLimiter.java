package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.exception.RateLimitExceededException;

import java.time.Clock;

/**
 * Fixed one-minute window: at most N calls per key (a user, an IP address...) and minute.
 * Instances are created per use case by {@link com.vovan4ok.appliance.store.config.RateLimitConfig}.
 */
@FunctionalInterface
public interface RateLimiter {

    long WINDOW_SECONDS = 60;

    /**
     * Counts one call for the key.
     *
     * @throws RateLimitExceededException when the key is over the limit for the current window
     */
    void acquire(String key);

    static long currentWindow(Clock clock) {
        return clock.instant().getEpochSecond() / WINDOW_SECONDS;
    }

    static long secondsUntilNextWindow(Clock clock) {
        return WINDOW_SECONDS - clock.instant().getEpochSecond() % WINDOW_SECONDS;
    }
}
