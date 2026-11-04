package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
import com.vovan4ok.appliance.store.service.AiRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single-instance fallback used when Redis is disabled (dev/test).
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryAiRateLimiter implements AiRateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limitPerMinute;
    private final Clock clock;

    @Autowired
    public InMemoryAiRateLimiter(@Value("${app.ai.rate-limit.per-minute}") int limitPerMinute) {
        this(limitPerMinute, Clock.systemUTC());
    }

    InMemoryAiRateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    @Override
    public void acquire(String user) {
        long current = AiRateLimiter.currentWindow(clock);
        Window window = windows.compute(user,
                (key, old) -> old == null || old.id != current ? new Window(current) : old);
        if (window.count.incrementAndGet() > limitPerMinute) {
            throw new RateLimitExceededException(AiRateLimiter.secondsUntilNextWindow(clock));
        }
    }

    private static final class Window {
        private final long id;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long id) {
            this.id = id;
        }
    }
}
