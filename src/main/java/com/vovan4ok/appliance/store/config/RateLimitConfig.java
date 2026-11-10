package com.vovan4ok.appliance.store.config;

import com.vovan4ok.appliance.store.service.AiRateLimiter;
import com.vovan4ok.appliance.store.service.RateLimiter;
import com.vovan4ok.appliance.store.service.RecommendationRateLimiter;
import com.vovan4ok.appliance.store.service.impl.InMemoryRateLimiter;
import com.vovan4ok.appliance.store.service.impl.RedisRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

/**
 * One rate limiter per use case, all with the same fixed-window logic: in Redis when it is enabled (shared by
 * every app instance), otherwise in memory (dev/test).
 */
@Configuration
public class RateLimitConfig {

    /** Creates a limiter for one use case ("scope") with its own per-minute limit. */
    @FunctionalInterface
    public interface RateLimiterFactory {
        RateLimiter create(String scope, int limitPerMinute);
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public RateLimiterFactory redisRateLimiterFactory(StringRedisTemplate redis) {
        return (scope, limit) -> new RedisRateLimiter(redis, scope, limit, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
    public RateLimiterFactory inMemoryRateLimiterFactory() {
        return (scope, limit) -> new InMemoryRateLimiter(limit, Clock.systemUTC());
    }

    @Bean
    public AiRateLimiter aiRateLimiter(RateLimiterFactory factory,
                                       @Value("${app.ai.rate-limit.per-minute}") int perMinute) {
        return factory.create("ai", perMinute)::acquire;
    }

    @Bean
    public RecommendationRateLimiter recommendationRateLimiter(
            RateLimiterFactory factory, @Value("${app.recommendations.rate-limit.per-minute}") int perMinute) {
        return factory.create("recommendations", perMinute)::acquire;
    }
}
