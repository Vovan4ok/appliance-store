package com.vovan4ok.appliance.store.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = "test-only-insecure-jwt-secret-do-not-use-in-prod";

    @Test
    void constructor_nullSecret_failsFast() {
        assertThatThrownBy(() -> new JwtUtils(null, 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void constructor_blankSecret_failsFast() {
        assertThatThrownBy(() -> new JwtUtils("", 1000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_secretShorterThan32Bytes_failsFast() {
        assertThatThrownBy(() -> new JwtUtils("a".repeat(31), 1000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void constructor_secretOf32Bytes_isAccepted() {
        assertThat(new JwtUtils("a".repeat(32), 1000).getExpirationMs()).isEqualTo(1000);
    }

    @Test
    void generateToken_roundTripsEmailAndRole() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);

        String token = jwtUtils.generateToken("user@test.com", "ROLE_EMPLOYEE");

        assertThat(jwtUtils.isValid(token)).isTrue();
        assertThat(jwtUtils.extractEmail(token)).isEqualTo("user@test.com");
        assertThat(jwtUtils.extractRole(token)).isEqualTo("ROLE_EMPLOYEE");
    }

    @Test
    void isValid_tokenSignedWithAnotherSecret_isRejected() {
        String foreign = new JwtUtils("another-secret-that-is-also-32-bytes-long!!", 60_000)
                .generateToken("user@test.com", "ROLE_EMPLOYEE");

        assertThat(new JwtUtils(SECRET, 60_000).isValid(foreign)).isFalse();
    }

    @Test
    void generateToken_givesEveryTokenAUniqueId() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);

        String first = jwtUtils.extractId(jwtUtils.generateToken("user@test.com", "ROLE_CLIENT"));
        String second = jwtUtils.extractId(jwtUtils.generateToken("user@test.com", "ROLE_CLIENT"));

        assertThat(first).isNotBlank().isNotEqualTo(second);
    }

    @Test
    void remainingLifetime_isTimeUntilExpiry() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);

        Duration remaining = jwtUtils.remainingLifetime(jwtUtils.generateToken("user@test.com", "ROLE_CLIENT"));

        assertThat(remaining).isBetween(Duration.ofSeconds(58), Duration.ofSeconds(60));
    }

    @Test
    void isValid_expiredToken_isRejected() {
        JwtUtils jwtUtils = new JwtUtils(SECRET, -1000);

        assertThat(jwtUtils.isValid(jwtUtils.generateToken("user@test.com", "ROLE_CLIENT"))).isFalse();
    }
}
