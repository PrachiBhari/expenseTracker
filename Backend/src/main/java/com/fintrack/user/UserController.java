package com.fintrack.user;

import com.fintrack.security.SecurityUtils;
import com.fintrack.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile endpoints — PROTECTED (requires valid JWT).
 *
 * Full path: GET /api/v1/users/me
 *
 * SecurityRequirement("bearerAuth") → shows the lock icon in Swagger UI,
 * indicating this endpoint requires the Bearer token.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile operations")
public class UserController {

    private final UserService userService;

    /**
     * Returns the current authenticated user's profile.
     *
     * SecurityUtils.getCurrentUserId() reads from the SecurityContext,
     * which was populated by JwtAuthFilter for this request.
     * No userId in the URL path — users can only see their own profile.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getCurrentUser(userId));
    }
}
