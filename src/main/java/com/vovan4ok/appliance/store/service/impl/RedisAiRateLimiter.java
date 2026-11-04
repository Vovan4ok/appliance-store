package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
import com.vovan4ok.appliance.store.service.AiRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/**
 * Shared across all app instances: one Redis counter per user and minute (INCR is atomic).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisAiRateLimiter implements AiRateLimiter {

    static final String KEY_PREFIX = "ai:rate:";

    private final StringRedisTemplate redis;
    private final int limitPerMinute;
    private final Clock clock;

    @Autowired
    public RedisAiRateLimiter(StringRedisTemplate redis,
                              @Value("${app.ai.rate-limit.per-minute}") int limitPerMinute) {
        this(redis, limitPerMinute, Clock.systemUTC());
    }

    RedisAiRateLimiter(StringRedisTemplate redis, int limitPerMinute, Clock clock) {
        this.redis = redis;
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    @Override
    public void acquire(String user) {
        String key = KEY_PREFIX + user + ":" + AiRateLimiter.currentWindow(clock);
        Long count;
        try {
            count = redis.opsForValue().increment(key);
            // The key outlives its window slightly, then Redis deletes it
            redis.expire(key, Duration.ofSeconds(WINDOW_SECONDS + 10));
        } catch (DataAccessException e) {
            // Fail open: a Redis outage must not block employees (health shows Redis DOWN)
            log.warn("AI rate limit skipped - Redis unavailable: {}", e.getMessage());
            return;
        }
        if (count != null && count > limitPerMinute) {
            throw new RateLimitExceededException(AiRateLimiter.secondsUntilNextWindow(clock));
        }
    }
}
