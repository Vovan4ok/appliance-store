package com.vovan4ok.appliance.store.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisTokenDenylist implements TokenDenylist {

    static final String KEY_PREFIX = "jwt:denylist:";

    private final StringRedisTemplate redis;

    @Override
    public void revoke(String tokenId, Duration timeToLive) {
        if (tokenId == null || timeToLive.isZero() || timeToLive.isNegative()) {
            return;
        }
        try {
            // TTL = remaining token lifetime, so Redis cleans the entry up by itself
            redis.opsForValue().set(KEY_PREFIX + tokenId, "1", timeToLive);
        } catch (DataAccessException e) {
            log.error("Could not revoke JWT id={} - Redis unavailable: {}", tokenId, e.getMessage());
        }
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + tokenId));
        } catch (DataAccessException e) {
            // Fail open: a Redis outage must not log every user out (health shows Redis DOWN)
            log.warn("JWT deny-list check skipped - Redis unavailable: {}", e.getMessage());
            return false;
        }
    }
}
