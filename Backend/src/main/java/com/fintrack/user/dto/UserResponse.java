package com.fintrack.user.dto;

import com.fintrack.user.Role;
import com.fintrack.user.User;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for user profile data.
 * Returned from GET /users/me and embedded inside AuthResponse.
 *
 * DELIBERATELY excludes:
 *   - passwordHash   → never send password data to clients
 *   - enabled        → internal admin concern
 *   - createdAt      → not needed in MVP profile view
 *
 * Static factory method from(User) — converts entity to DTO in one line.
 * This is a simple alternative to MapStruct for a 4-field DTO.
 */
@Getter
@Builder
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
