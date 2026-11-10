package com.vovan4ok.appliance.store.service;

/**
 * Per-IP limit on the public recommendations endpoint ({@code app.recommendations.rate-limit.per-minute}):
 * it needs no login, so without a limit anyone could scrape the catalog in a loop.
 */
@FunctionalInterface
public interface RecommendationRateLimiter extends RateLimiter {
}
