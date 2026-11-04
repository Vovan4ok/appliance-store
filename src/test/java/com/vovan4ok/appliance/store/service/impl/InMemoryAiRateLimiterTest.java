package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.MutableClock;
import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
import com.vovan4ok.appliance.store.service.AiRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAiRateLimiterTest {

    // 20 seconds into a minute window
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-04T10:00:20Z"));
    private final AiRateLimiter limiter = new InMemoryAiRateLimiter(3, clock);

    @Test
    void requestsUpToTheLimit_areAllowed() {
        assertThatCode(() -> {
            limiter.acquire("phobos@gmail.com");
            limiter.acquire("phobos@gmail.com");
            limiter.acquire("phobos@gmail.com");
        }).doesNotThrowAnyException();
    }

    @Test
    void requestOverTheLimit_isRejectedWithSecondsUntilTheNextWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.acquire("phobos@gmail.com");
        }

        assertThatThrownBy(() -> limiter.acquire("phobos@gmail.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("retryAfterSeconds").isEqualTo(40L);
    }

    @Test
    void limit_isPerUser() {
        for (int i = 0; i < 3; i++) {
            limiter.acquire("phobos@gmail.com");
        }

        assertThatCode(() -> limiter.acquire("moon@gmail.com")).doesNotThrowAnyException();
    }

    @Test
    void nextWindow_resetsTheCounter() {
        for (int i = 0; i < 3; i++) {
            limiter.acquire("phobos@gmail.com");
        }
        clock.advance(Duration.ofSeconds(40));

        assertThatCode(() -> limiter.acquire("phobos@gmail.com")).doesNotThrowAnyException();
    }
}
