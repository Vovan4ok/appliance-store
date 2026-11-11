package com.vovan4ok.appliance.store.service;

/**
 * Global cap on LLM calls for the AI re-ranking of recommendations
 * ({@code app.recommendations.ai.max-calls-per-minute}, one shared key): the endpoint is public, so this caps
 * the cost no matter how many visitors or instances there are.
 */
@FunctionalInterface
public interface RecommendationAiBudget extends RateLimiter {
}
