package com.vovan4ok.appliance.store.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-instance fallback used when Redis is disabled (dev/test). Entries are lost on restart.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTokenDenylist implements TokenDenylist {

    private final Map<String, Instant> revokedUntil = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryTokenDenylist() {
        this(Clock.systemUTC());
    }

    InMemoryTokenDenylist(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void revoke(String tokenId, Duration timeToLive) {
        if (tokenId == null || timeToLive.isZero() || timeToLive.isNegative()) {
            return;
        }
        Instant now = clock.instant();
        revokedUntil.values().removeIf(expiry -> !expiry.isAfter(now));
        revokedUntil.put(tokenId, now.plus(timeToLive));
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null) {
            return false;
        }
        Instant expiry = revokedUntil.get(tokenId);
        if (expiry == null) {
            return false;
        }
        if (!expiry.isAfter(clock.instant())) {
            revokedUntil.remove(tokenId);
            return false;
        }
        return true;
    }
}
