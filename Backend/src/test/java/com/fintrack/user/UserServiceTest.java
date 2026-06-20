package com.fintrack.user;

import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.common.exception.DuplicateResourceException;
import com.fintrack.security.JwtService;
import com.fintrack.security.RefreshToken;
import com.fintrack.security.RefreshTokenRepository;
import com.fintrack.user.dto.AuthResponse;
import com.fintrack.user.dto.LoginRequest;
import com.fintrack.user.dto.RefreshTokenRequest;
import com.fintrack.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService — the authentication business logic layer.
 *
 * Pattern: Arrange → Act → Assert
 *
 * WHY unit tests instead of integration tests here?
 * Unit tests are:
 *   - Fast (no Spring context, no DB)
 *   - Focused (test one class in isolation)
 *   - Precise (verify exactly which methods were called and with what arguments)
 *   - Deterministic (no side effects between tests)
 *
 * We mock every dependency so each test covers ONE thing:
 * the logic inside UserService itself.
 *
 * INTERVIEW: "What is the difference between @Mock and @Spy?"
 *   @Mock creates a completely fake object — all methods return null/0/false by default.
 *   @Spy wraps a real object — real methods run unless you stub specific ones.
 *   Use @Mock for external dependencies. Use @Spy when you want real behaviour
 *   for most methods but override specific ones.
 */
@ExtendWith(MockitoExtension.class)   // JUnit 5 Mockito integration (no @RunWith needed)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    // @Mock creates a fake implementation of each interface/class.
    // Mockito intercepts every call and returns a configured stub or default value.
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    // ── System Under Test ─────────────────────────────────────────────────────
    // @InjectMocks creates UserService using constructor injection with the @Mocks above.
    // Mockito looks for a constructor that matches the mock types.
    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {
        // @Value("${jwt.refresh-token-expiration}") is NOT injected by @InjectMocks.
        // Spring's @Value injection only happens inside a Spring context.
        // In pure Mockito tests (no Spring), we set @Value fields manually via reflection.
        ReflectionTestUtils.setField(userService, "refreshTokenExpiration", 604800000L);
    }

    // =========================================================================
    // register()
    // =========================================================================

    @Test
    @DisplayName("register: new email → saves user, seeds 14 default categories, returns tokens")
    void register_withNewEmail_shouldSaveUserSeedCategoriesAndReturnTokens() {
        // Arrange — set up all stubs BEFORE calling the method under test
        RegisterRequest request = buildRegisterRequest("Alice Kumar", "alice@example.com", "password123");

        User savedUser = buildUser(1L, "Alice Kumar", "alice@example.com");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$bcrypt-hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser)).thenReturn("mock-access-token");
        // thenAnswer with getArgument(0) returns the RefreshToken that was passed to save()
        // This lets us inspect the token value that the service set on it.
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuthResponse response = userService.register(request);

        // Assert — verify the response fields
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isNotBlank();  // UUID assigned by service
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        // Verify exactly 14 default categories were seeded
        // ArgumentCaptor captures the actual argument passed to saveAll()
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> captor =
                (ArgumentCaptor<List<Category>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(14);
    }

    @Test
    @DisplayName("register: duplicate email → throws DuplicateResourceException, never saves user")
    void register_withExistingEmail_shouldThrowDuplicateResourceException() {
        // Arrange
        RegisterRequest request = buildRegisterRequest("Bob", "existing@example.com", "password");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@example.com");

        // Verify no user was persisted and no categories were seeded
        verify(userRepository, never()).save(any());
        verify(categoryRepository, never()).saveAll(any());
    }

    // =========================================================================
    // login()
    // =========================================================================

    @Test
    @DisplayName("login: correct credentials → returns tokens with user profile")
    void login_withCorrectCredentials_shouldReturnAuthResponse() {
        // Arrange
        LoginRequest request = buildLoginRequest("alice@example.com", "password123");
        User user = buildUser(1L, "Alice", "alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuthResponse response = userService.login(request);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("login: unknown email → throws BadCredentialsException (generic message)")
    void login_withUnknownEmail_shouldThrowBadCredentialsException() {
        // Arrange
        LoginRequest request = buildLoginRequest("ghost@example.com", "any-password");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // Password check must NEVER run if the user doesn't exist
        // This ensures the same code path for missing user vs wrong password (no enumeration)
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login: wrong password → throws BadCredentialsException, never issues token")
    void login_withWrongPassword_shouldThrowBadCredentialsException() {
        // Arrange
        LoginRequest request = buildLoginRequest("alice@example.com", "wrong-password");
        User user = buildUser(1L, "Alice", "alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // Token must NEVER be issued on failed login
        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // =========================================================================
    // refreshToken()
    // =========================================================================

    @Test
    @DisplayName("refreshToken: valid token → deletes old, saves new (rotation), returns new tokens")
    void refreshToken_withValidToken_shouldRotateAndReturnNewAuthResponse() {
        // Arrange
        User user = buildUser(1L, "Alice", "alice@example.com");
        RefreshToken existingToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("old-refresh-token")
                .expiresAt(Instant.now().plusSeconds(3600))  // 1 hour in future — not expired
                .createdAt(Instant.now())
                .build();

        RefreshTokenRequest request = buildRefreshTokenRequest("old-refresh-token");

        when(refreshTokenRepository.findByToken("old-refresh-token"))
                .thenReturn(Optional.of(existingToken));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        AuthResponse response = userService.refreshToken(request);

        // Assert
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");

        // ROTATION: verify the old token was deleted
        verify(refreshTokenRepository).delete(existingToken);
        // Verify a new token was saved
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refreshToken: expired token → deletes it and throws BadCredentialsException")
    void refreshToken_withExpiredToken_shouldCleanUpAndThrow() {
        // Arrange
        User user = buildUser(1L, "Alice", "alice@example.com");
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .token("expired-token")
                .expiresAt(Instant.now().minusSeconds(3600))  // 1 hour in the PAST — expired
                .createdAt(Instant.now().minusSeconds(7200))
                .build();

        RefreshTokenRequest request = buildRefreshTokenRequest("expired-token");
        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        // Act + Assert
        assertThatThrownBy(() -> userService.refreshToken(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");

        // Expired tokens must be cleaned up from DB even though they're invalid
        verify(refreshTokenRepository).delete(expiredToken);
        // Must NOT issue a new token
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("refreshToken: unknown token → throws BadCredentialsException")
    void refreshToken_withUnknownToken_shouldThrowBadCredentialsException() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest("nonexistent");
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.refreshToken(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // =========================================================================
    // Test data builders
    // =========================================================================

    /**
     * Creates a User with a known ID. Builder works because User has @Builder.
     * 'hashed-password' is what we configure passwordEncoder.matches() to match against.
     */
    private User buildUser(Long id, String fullName, String email) {
        return User.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .passwordHash("hashed-password")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    /**
     * Creates DTOs via ReflectionTestUtils because RegisterRequest has @NoArgsConstructor
     * but no setters (only @Getter). ReflectionTestUtils bypasses access control and sets
     * private fields directly — safe in tests, never do this in production code.
     */
    private RegisterRequest buildRegisterRequest(String fullName, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        ReflectionTestUtils.setField(request, "fullName", fullName);
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private LoginRequest buildLoginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private RefreshTokenRequest buildRefreshTokenRequest(String token) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        ReflectionTestUtils.setField(request, "refreshToken", token);
        return request;
    }
}
