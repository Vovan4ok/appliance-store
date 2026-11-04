package com.vovan4ok.appliance.store.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring's cache abstraction. The backing store comes from spring.cache.type:
 * Redis in prod (shared, with a TTL), an in-memory map in dev/test.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String AI_DESCRIPTIONS = "aiDescriptions";
}
