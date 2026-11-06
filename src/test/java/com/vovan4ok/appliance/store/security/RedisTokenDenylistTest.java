package com.vovan4ok.appliance.store.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Runs against a real Redis in Docker (skipped when Docker is not available).
 */
@DataRedisTest
@Testcontainers(disabledWithoutDocker = true)
class RedisTokenDenylistTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.10-alpine"))
            .withExposedPorts(6379);

    @Autowired
    StringRedisTemplate template;

    RedisTokenDenylist denylist;

    @BeforeEach
    void setUp() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
        denylist = new RedisTokenDenylist(template);
    }

    @Test
    void revokedToken_isStoredWithTtlOfItsRemainingLifetime() {
        denylist.revoke("jti-1", Duration.ofMinutes(10));

        assertThat(denylist.isRevoked("jti-1")).isTrue();
        Long ttl = template.getExpire(RedisTokenDenylist.KEY_PREFIX + "jti-1");
        assertThat(ttl).isBetween(590L, 600L);
    }

    @Test
    void unknownToken_isNotRevoked() {
        assertThat(denylist.isRevoked("never-revoked")).isFalse();
    }

    @Test
    void expiredEntry_disappearsByItself() throws InterruptedException {
        denylist.revoke("short-lived", Duration.ofSeconds(1));
        assertThat(denylist.isRevoked("short-lived")).isTrue();

        Thread.sleep(1500);

        assertThat(denylist.isRevoked("short-lived")).isFalse();
    }

    @Test
    void redisUnavailable_failsOpenInsteadOfThrowing() {
        LettuceConnectionFactory deadFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 1));
        deadFactory.afterPropertiesSet();
        RedisTokenDenylist unavailable = new RedisTokenDenylist(new StringRedisTemplate(deadFactory));

        assertThatCode(() -> unavailable.revoke("jti-1", Duration.ofMinutes(1))).doesNotThrowAnyException();
        assertThat(unavailable.isRevoked("jti-1")).isFalse();
        deadFactory.destroy();
    }
}
