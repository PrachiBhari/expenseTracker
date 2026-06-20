package com.fintrack.user.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response body for POST /auth/register, /auth/login, and /auth/refresh.
 *
 * Contains everything the frontend needs to start making authenticated requests:
 *   accessToken   → short-lived JWT (15 min) — sent as Bearer on every API call
 *   tokenType     → always "Bearer" — tells the frontend how to use the token
 *   refreshToken  → long-lived random UUID (7 days) — sent only to /auth/refresh
 *   user          → user profile for the frontend to display (name, email)
 *
 * Token storage decision (documented trade-off from TRD §7.3):
 *   Both tokens stored in localStorage for MVP simplicity.
 *   More secure alternative: httpOnly cookie for refresh token (prevents XSS reading it),
 *   but adds CSRF considerations. Documented as a future hardening step.
 *
 * Example JSON response:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tokenType": "Bearer",
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
 *   "user": { "id": 1, "fullName": "Aisha K", "email": "aisha@example.com", "role": "USER" }
 * }
 */
@Getter
@Builder
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private String refreshToken;
    private UserResponse user;
}
