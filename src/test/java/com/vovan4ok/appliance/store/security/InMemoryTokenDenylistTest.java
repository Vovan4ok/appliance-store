package com.vovan4ok.appliance.store.security;

import com.vovan4ok.appliance.store.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenDenylistTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-04T10:00:00Z"));
    private final InMemoryTokenDenylist denylist = new InMemoryTokenDenylist(clock);

    @Test
    void revokedToken_isRevokedUntilItWouldHaveExpired() {
        denylist.revoke("jti-1", Duration.ofMinutes(10));

        assertThat(denylist.isRevoked("jti-1")).isTrue();
        clock.advance(Duration.ofMinutes(9));
        assertThat(denylist.isRevoked("jti-1")).isTrue();
        clock.advance(Duration.ofMinutes(1));
        assertThat(denylist.isRevoked("jti-1")).isFalse();
    }

    @Test
    void otherTokens_areNotAffected() {
        denylist.revoke("jti-1", Duration.ofMinutes(10));

        assertThat(denylist.isRevoked("jti-2")).isFalse();
    }

    @Test
    void nullIdOrNonPositiveLifetime_isIgnored() {
        denylist.revoke(null, Duration.ofMinutes(10));
        denylist.revoke("expired", Duration.ZERO);

        assertThat(denylist.isRevoked(null)).isFalse();
        assertThat(denylist.isRevoked("expired")).isFalse();
    }
}
