package com.fintrack.user;

import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.category.CategoryType;
import com.fintrack.common.exception.DuplicateResourceException;
import com.fintrack.common.exception.ResourceNotFoundException;
import com.fintrack.security.JwtService;
import com.fintrack.security.RefreshToken;
import com.fintrack.security.RefreshTokenRepository;
import com.fintrack.user.dto.AuthResponse;
import com.fintrack.user.dto.LoginRequest;
import com.fintrack.user.dto.RegisterRequest;
import com.fintrack.user.dto.RefreshTokenRequest;
import com.fintrack.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for User authentication and profile operations.
 *
 * Handles all authentication business logic:
 *   register()      — validate, hash password, save user, seed categories, issue tokens
 *   login()         — verify credentials, issue tokens
 *   refreshToken()  — validate refresh token, rotate it, issue new access token
 *   logout()        — revoke refresh token
 *   getCurrentUser()— return profile for /users/me
 *
 * @Transactional on register() ensures atomicity:
 *   If category seeding fails after the user is saved, the entire registration
 *   rolls back — no partial state in the DB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // =========================================================================
    // Registration
    // =========================================================================

    /**
     * Registers a new user account.
     *
     * Steps:
     *   1. Check email uniqueness
     *   2. Hash the password with BCrypt
     *   3. Save the User entity
     *   4. Seed default categories for this new user
     *   5. Generate JWT access token
     *   6. Create and persist a refresh token
     *   7. Return AuthResponse with both tokens + user profile
     *
     * @Transactional: all DB operations succeed together or roll back together.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Step 1: check uniqueness before saving
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email '" + request.getEmail() + "' already exists.");
        }

        // Step 2 + 3: build and save the user with hashed password
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: id={}, email={}", user.getId(), user.getEmail());

        // Step 4: seed default categories so the user can start immediately
        seedDefaultCategories(user);

        // Steps 5-7: generate and return tokens
        return issueAuthResponse(user);
    }

    // =========================================================================
    // Login
    // =========================================================================

    /**
     * Authenticates a user with email + password.
     *
     * Manual credential verification instead of AuthenticationManager.authenticate():
     *   - Same behavior as DaoAuthenticationProvider internally
     *   - Simpler: no need to expose AuthenticationManager as a bean
     *   - Same generic error message regardless of failure reason (no enumeration)
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        log.info("User logged in: id={}, email={}", user.getId(), user.getEmail());
        return issueAuthResponse(user);
    }

    // =========================================================================
    // Token Refresh
    // =========================================================================

    /**
     * Exchanges a valid refresh token for a new access token + new refresh token.
     *
     * TOKEN ROTATION: every refresh call:
     *   1. Validates the incoming refresh token
     *   2. Deletes it from the DB (it can never be used again)
     *   3. Issues a brand-new refresh token
     *
     * Why rotation?
     * If someone steals a refresh token, they can use it once.
     * After the legitimate user refreshes, the stolen token is invalidated.
     * The attacker's next use fails → security incident detected.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken existingToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Check expiry
        if (existingToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(existingToken); // clean up expired token
            throw new BadCredentialsException("Refresh token has expired. Please log in again.");
        }

        User user = existingToken.getUser();

        // Rotate: delete old token before issuing new one
        refreshTokenRepository.delete(existingToken);

        log.info("Token refreshed for user: id={}", user.getId());
        return issueAuthResponse(user);
    }

    // =========================================================================
    // Logout
    // =========================================================================

    /**
     * Revokes the refresh token — effectively ending the session.
     * The access token is still valid until it expires (15 min) but since it's
     * short-lived, the risk window is small. The frontend should clear it immediately.
     *
     * Silent success even if token is not found (already revoked or never existed).
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.deleteByToken(request.getRefreshToken());
        log.info("Refresh token revoked on logout");
    }

    // =========================================================================
    // Profile
    // =========================================================================

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserResponse.from(user);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Generates JWT + creates refresh token + builds AuthResponse.
     * Extracted as a helper because register(), login(), and refreshToken()
     * all need to issue the same response.
     */
    private AuthResponse issueAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createAndSaveRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(UserResponse.from(user))
                .build();
    }

    /**
     * Creates a new RefreshToken entity with a random UUID value and persists it.
     *
     * UUID.randomUUID() → "550e8400-e29b-41d4-a716-446655440000"
     * Cryptographically random, unpredictable, unique (collision probability is negligible).
     * The UNIQUE DB constraint is a safety net in case of collision.
     */
    private RefreshToken createAndSaveRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .createdAt(Instant.now())
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Seeds a sensible set of default categories for a new user.
     *
     * WHY seed at registration time?
     * PRD US-4.3: "As a new user, I want a starter set of sensible default categories
     * so that I can begin immediately." Without defaults, new users must create
     * categories before they can log any transaction — friction kills early engagement.
     *
     * These run inside the same @Transactional as register() — if saving a
     * category fails, the whole registration rolls back.
     */
    private void seedDefaultCategories(User user) {
        List<Category> defaults = List.of(
            // EXPENSE categories
            buildCategory(user, "Food & Dining",    CategoryType.EXPENSE, "#FF6B6B", "utensils"),
            buildCategory(user, "Transportation",   CategoryType.EXPENSE, "#4ECDC4", "car"),
            buildCategory(user, "Housing & Rent",   CategoryType.EXPENSE, "#45B7D1", "home"),
            buildCategory(user, "Shopping",         CategoryType.EXPENSE, "#FFA07A", "shopping-bag"),
            buildCategory(user, "Entertainment",    CategoryType.EXPENSE, "#98D8C8", "film"),
            buildCategory(user, "Healthcare",       CategoryType.EXPENSE, "#DDA0DD", "heart"),
            buildCategory(user, "Education",        CategoryType.EXPENSE, "#F0E68C", "book"),
            buildCategory(user, "Utilities",        CategoryType.EXPENSE, "#B0C4DE", "zap"),
            buildCategory(user, "Personal Care",    CategoryType.EXPENSE, "#FFB6C1", "user"),
            buildCategory(user, "Other Expenses",   CategoryType.EXPENSE, "#D3D3D3", "more-horizontal"),
            // INCOME categories
            buildCategory(user, "Salary",           CategoryType.INCOME,  "#90EE90", "briefcase"),
            buildCategory(user, "Freelance",        CategoryType.INCOME,  "#87CEEB", "laptop"),
            buildCategory(user, "Investment",       CategoryType.INCOME,  "#F0E68C", "trending-up"),
            buildCategory(user, "Other Income",     CategoryType.INCOME,  "#D3D3D3", "plus-circle")
        );

        categoryRepository.saveAll(defaults);
        log.debug("Seeded {} default categories for user id={}", defaults.size(), user.getId());
    }

    private Category buildCategory(User user, String name, CategoryType type,
                                   String color, String icon) {
        return Category.builder()
                .user(user)
                .name(name)
                .type(type)
                .color(color)
                .icon(icon)
                .build();
    }
}
