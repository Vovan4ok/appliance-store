package com.vovan4ok.appliance.store.model.dto.api;

import lombok.Getter;

@Getter
public class TokenResponse {

    private final String token;
    private final String type = "Bearer";
    private final long expiresIn;

    public TokenResponse(String token, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}