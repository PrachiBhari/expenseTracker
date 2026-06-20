package com.fintrack.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /auth/register.
 *
 * Bean Validation annotations define what "valid" means for each field.
 * @Valid on the controller parameter triggers these checks before the method runs.
 * If any check fails, MethodArgumentNotValidException is thrown →
 * GlobalExceptionHandler returns a 400 with the list of field errors.
 *
 * Why separate DTO instead of using User entity directly?
 *   1. The entity has fields we don't want the client to set (id, role, enabled, createdAt).
 *   2. The password in the request is PLAINTEXT — the entity stores the hash.
 *   3. DTOs create a stable API contract independent of the DB schema.
 */
@Getter
@NoArgsConstructor
public class RegisterRequest {

    @Size(max = 120, message = "Full name must not exceed 120 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    /**
     * @NotBlank: not null and not just whitespace
     * @Size min=8: minimum password length per PRD AC3 (US-1.1)
     * Max 100: prevents DoS via BCrypt (BCrypt is intentionally slow;
     * hashing a 1MB password string would take forever)
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
