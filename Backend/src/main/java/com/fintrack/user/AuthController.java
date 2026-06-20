package com.fintrack.user;

import com.fintrack.user.dto.AuthResponse;
import com.fintrack.user.dto.LoginRequest;
import com.fintrack.user.dto.RefreshTokenRequest;
import com.fintrack.user.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints — all PUBLIC (no JWT required).
 *
 * Full paths (with context-path /api/v1):
 *   POST /api/v1/auth/register  → create account + return tokens
 *   POST /api/v1/auth/login     → verify credentials + return tokens
 *   POST /api/v1/auth/refresh   → rotate refresh token + return new tokens
 *   POST /api/v1/auth/logout    → revoke refresh token
 *
 * Controller responsibilities (ONLY these — no business logic here):
 *   1. Map HTTP methods and paths to service methods
 *   2. Trigger @Valid bean validation on request bodies
 *   3. Return the correct HTTP status code
 *
 * @RestController = @Controller + @ResponseBody (all methods return JSON).
 * @RequestMapping sets the URL prefix for all methods in this class.
 * @Tag is a Swagger annotation — groups these endpoints in Swagger UI.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, and logout")
public class AuthController {

    private final UserService userService;

    /**
     * Register a new account.
     * Returns 201 Created (not 200 OK) — a new resource was created.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with email and password.
     * Returns 200 OK — not creating a new resource, just authenticating.
     */
    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Exchange a refresh token for a new access token + new refresh token.
     * The old refresh token is invalidated (token rotation).
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = userService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout — revoke the refresh token.
     * Returns 204 No Content — operation succeeded, nothing to return.
     * The client should clear both tokens from localStorage on receipt of 204.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
