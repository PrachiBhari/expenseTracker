package com.fintrack.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /auth/refresh.
 *
 * The client sends the refresh token it received during login/register.
 * The server validates it, deletes it (token rotation), and returns
 * a new access token + new refresh token.
 */
@Getter
@NoArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
