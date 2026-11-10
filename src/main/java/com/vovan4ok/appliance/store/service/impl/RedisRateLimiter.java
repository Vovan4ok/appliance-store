package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
import com.vovan4ok.appliance.store.service.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;

/**
 * Shared across all app instances: one Redis counter per scope, key and minute (INCR is atomic).
 */
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    static final String KEY_PREFIX = "rate:";

    private final StringRedisTemplate redis;
    private final String scope;
    private final int limitPerMinute;
    private final Clock clock;

    /**
     * @param scope the use case ("ai", "recommendations"...): the same key gets a separate counter per use case
     */
    public RedisRateLimiter(StringRedisTemplate redis, String scope, int limitPerMinute, Clock clock) {
        this.redis = redis;
        this.scope = scope;
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    @Override
    public void acquire(String key) {
        String redisKey = KEY_PREFIX + scope + ":" + key + ":" + RateLimiter.currentWindow(clock);
        Long count;
        try {
            count = redis.opsForValue().increment(redisKey);
            // The key outlives its window slightly, then Redis deletes it
            redis.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS + 10));
        } catch (DataAccessException e) {
            // Fail open: a Redis outage must not block users (health shows Redis DOWN)
            log.warn("Rate limit {} skipped - Redis unavailable: {}", scope, e.getMessage());
            return;
        }
        if (count != null && count > limitPerMinute) {
            throw new RateLimitExceededException(RateLimiter.secondsUntilNextWindow(clock));
        }
    }
}
