package com.vovan4ok.appliance.store.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many AI requests - try again in " + retryAfterSeconds + " s");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
