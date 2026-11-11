package com.vovan4ok.appliance.store.service;

/**
 * Per-employee limit on AI calls ({@code app.ai.rate-limit.per-minute}); the key is the user's email.
 * Its own type, so it can be injected (and mocked) separately from the other limiters.
 */
@FunctionalInterface
public interface AiRateLimiter extends RateLimiter {
}
