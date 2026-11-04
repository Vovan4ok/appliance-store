package com.vovan4ok.appliance.store.security;

import java.time.Duration;

/**
 * Revoked JWT ids (jti). A token stays on the list only until it would have expired anyway.
 */
public interface TokenDenylist {

    void revoke(String tokenId, Duration timeToLive);

    boolean isRevoked(String tokenId);
}
