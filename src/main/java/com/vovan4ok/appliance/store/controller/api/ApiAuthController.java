package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.dto.api.ErrorResponse;
import com.vovan4ok.appliance.store.model.dto.api.LoginRequest;
import com.vovan4ok.appliance.store.model.dto.api.TokenResponse;
import com.vovan4ok.appliance.store.security.JwtUtils;
import com.vovan4ok.appliance.store.security.TokenDenylist;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Obtain a JWT token")
public class ApiAuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final TokenDenylist tokenDenylist;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login and receive a Bearer token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            String role = auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority())
                    .orElse("ROLE_CLIENT");

            String token = jwtUtils.generateToken(request.getEmail(), role);
            return ResponseEntity.ok(new TokenResponse(token, jwtUtils.getExpirationMs()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse(401, "Unauthorized", "Invalid email or password", "/api/v1/auth/login"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current Bearer token (it stops working immediately)")
    public ResponseEntity<?> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, "Bad Request",
                    "Send the token to revoke as 'Authorization: Bearer <token>'", "/api/v1/auth/logout"));
        }
        // Only reachable with a valid, non-revoked token: /api/v1/** requires authentication
        String token = authorization.substring(BEARER_PREFIX.length());
        tokenDenylist.revoke(jwtUtils.extractId(token), jwtUtils.remainingLifetime(token));
        return ResponseEntity.noContent().build();
    }
}