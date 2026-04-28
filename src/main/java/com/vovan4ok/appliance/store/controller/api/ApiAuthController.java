package com.vovan4ok.appliance.store.controller.api;

import com.vovan4ok.appliance.store.model.dto.api.ErrorResponse;
import com.vovan4ok.appliance.store.model.dto.api.LoginRequest;
import com.vovan4ok.appliance.store.model.dto.api.TokenResponse;
import com.vovan4ok.appliance.store.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Obtain a JWT token")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

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
            return ResponseEntity.ok(new TokenResponse(token, 86400000L));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse(401, "Unauthorized", "Invalid email or password", "/api/v1/auth/login"));
        }
    }
}