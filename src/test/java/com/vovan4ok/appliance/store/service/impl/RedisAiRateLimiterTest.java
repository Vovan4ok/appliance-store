package com.vovan4ok.appliance.store.service.impl;

import com.vovan4ok.appliance.store.MutableClock;
import com.vovan4ok.appliance.store.exception.RateLimitExceededException;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against a real Redis in Docker (skipped when Docker is not available).
 */
@DataRedisTest
@Testcontainers(disabledWithoutDocker = true)
class RedisAiRateLimiterTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.10-alpine"))
            .withExposedPorts(6379);

    @Autowired
    StringRedisTemplate template;

    // 20 seconds into a minute window
    MutableClock clock = new MutableClock(Instant.parse("2026-11-04T10:00:20Z"));
    RedisAiRateLimiter limiter;

    @BeforeEach
    void setUp() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
        limiter = new RedisAiRateLimiter(template, 3, clock);
    }

    @Test
    void overTheLimit_isRejectedWithSecondsUntilTheNextWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.acquire("phobos@gmail.com");
        }

        assertThatThrownBy(() -> limiter.acquire("phobos@gmail.com"))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("retryAfterSeconds").isEqualTo(40L);
    }

    @Test
    void counter_isSharedThroughRedis_soAllAppInstancesSeeTheSameLimit() {
        RedisAiRateLimiter otherInstance = new RedisAiRateLimiter(template, 3, clock);
        limiter.acquire("phobos@gmail.com");
        otherInstance.acquire("phobos@gmail.com");
        limiter.acquire("phobos@gmail.com");

        assertThatThrownBy(() -> otherInstance.acquire("phobos@gmail.com"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void limit_isPerUser_andResetsInTheNextWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.acquire("phobos@gmail.com");
        }
        assertThatCode(() -> limiter.acquire("moon@gmail.com")).doesNotThrowAnyException();

        clock.advance(Duration.ofSeconds(40));
        assertThatCode(() -> limiter.acquire("phobos@gmail.com")).doesNotThrowAnyException();
    }

    @Test
    void windowKey_getsATtl_soRedisCleansItUp() {
        limiter.acquire("phobos@gmail.com");

        String key = template.keys(RedisAiRateLimiter.KEY_PREFIX + "*").iterator().next();
        assertThat(template.getExpire(key)).isBetween(60L, 70L);
    }

    @Test
    void redisUnavailable_failsOpenInsteadOfBlocking() {
        LettuceConnectionFactory deadFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("localhost", 1));
        deadFactory.afterPropertiesSet();
        RedisAiRateLimiter unavailable = new RedisAiRateLimiter(new StringRedisTemplate(deadFactory), 1, clock);

        assertThatCode(() -> {
            unavailable.acquire("phobos@gmail.com");
            unavailable.acquire("phobos@gmail.com");
        }).doesNotThrowAnyException();
        deadFactory.destroy();
    }
}
