package com.vovan4ok.appliance.store.service;

import com.vovan4ok.appliance.store.exception.RateLimitExceededException;

import java.time.Clock;

/**
 * Per-user limit on AI calls: a fixed one-minute window ({@code app.ai.rate-limit.per-minute}).
 */
public interface AiRateLimiter {

    long WINDOW_SECONDS = 60;

    /**
     * Counts one AI request for the user.
     *
     * @throws RateLimitExceededException when the user is over the limit for the current window
     */
    void acquire(String user);

    static long currentWindow(Clock clock) {
        return clock.instant().getEpochSecond() / WINDOW_SECONDS;
    }

    static long secondsUntilNextWindow(Clock clock) {
        return WINDOW_SECONDS - clock.instant().getEpochSecond() % WINDOW_SECONDS;
    }
}
