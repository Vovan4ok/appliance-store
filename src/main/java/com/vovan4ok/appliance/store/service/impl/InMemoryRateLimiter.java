package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
import com.vovan4ok.appliance.store.service.RateLimiter;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single-instance fallback used when Redis is disabled (dev/test).
 */
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limitPerMinute;
    private final Clock clock;

    public InMemoryRateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    @Override
    public void acquire(String key) {
        long current = RateLimiter.currentWindow(clock);
        Window window = windows.compute(key,
                (k, old) -> old == null || old.id != current ? new Window(current) : old);
        // Keys from past minutes are dropped, so a public endpoint hit from many IPs can't grow the map forever
        windows.values().removeIf(w -> w.id < current);
        if (window.count.incrementAndGet() > limitPerMinute) {
            throw new RateLimitExceededException(RateLimiter.secondsUntilNextWindow(clock));
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
