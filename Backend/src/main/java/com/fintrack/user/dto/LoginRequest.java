package com.fintrack.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /auth/login.
 *
 * Intentionally minimal — only email and password.
 * No @Size on password here because:
 *   - We don't want to reveal password constraints on login (that's a registration concern).
 *   - A wrong password of any length should give the same generic error.
 */
@Getter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
