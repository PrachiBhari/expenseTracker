package com.fintrack.user;

import com.fintrack.category.CategoryRepository;
import com.fintrack.expense.ExpenseRepository;
import com.fintrack.income.IncomeRepository;
import com.fintrack.security.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the authentication flow.
 *
 * WHAT MAKES THIS AN INTEGRATION TEST?
 * It tests the full request-response cycle:
 *   HTTP Request → Spring Security Filter → Controller → Service → Repository → H2 DB
 *                                                                             ↓
 *   HTTP Response ← Jackson Serialization ← Controller ← Service ← Repository
 *
 * Nothing is mocked — every layer runs for real.
 *
 * DATABASE: H2 in-memory with MODE=PostgreSQL (configured in application-test.yml).
 *   Flyway is disabled. Hibernate creates tables from JPA entities (ddl-auto=create-drop).
 *   Tables exist for the lifetime of the test class; we clean between tests.
 *
 * INTERVIEW: "@SpringBootTest vs @WebMvcTest — when do you use each?"
 *   @WebMvcTest: loads ONLY the web layer (controllers + filters + security).
 *     Services/repositories are @MockBean — fast but tests only controller logic.
 *     Use when: you want to test request mapping, validation, or security rules in isolation.
 *
 *   @SpringBootTest: loads the FULL application context (all beans, real repositories).
 *     Use when: you want to test end-to-end behavior including DB interactions.
 *     Slower to start, but verifies the whole chain from HTTP to DB.
 *
 * INTERVIEW: "@AutoConfigureMockMvc — what does it do?"
 *   Without it, you'd need to build MockMvc manually with mockMvcBuilders.webAppContextSetup().
 *   @AutoConfigureMockMvc registers MockMvc as a bean you can @Autowire directly.
 *   It automatically applies Spring Security configuration to MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")   // activates application-test.yml → H2 database
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    // Injected to clean up DB between tests
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private IncomeRepository incomeRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    // Shared test credentials
    private static final String TEST_EMAIL    = "testuser@example.com";
    private static final String TEST_PASSWORD = "securepassword123";
    private static final String TEST_NAME     = "Test User";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up in FK-safe order before each test:
        //   child records first (expenses, incomes, refresh_tokens)
        //   then categories, then users (parent)
        expenseRepository.deleteAll();
        incomeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Pre-register a test user so login tests have a user to authenticate against
        registerUser(TEST_NAME, TEST_EMAIL, TEST_PASSWORD);
    }

    // =========================================================================
    // POST /auth/register
    // =========================================================================

    @Test
    @DisplayName("register: valid payload → 201 Created with access token, refresh token, user profile")
    void register_withValidPayload_shouldReturn201WithAuthResponse() throws Exception {
        // Use a DIFFERENT email than setUp's user so there's no conflict
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "Alice Kumar",
                                    "email": "alice@example.com",
                                    "password": "mypassword123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.fullName").value("Alice Kumar"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                // Verify passwords are NEVER exposed in any response
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("register: duplicate email → 409 Conflict with error message")
    void register_withDuplicateEmail_shouldReturn409() throws Exception {
        // TEST_EMAIL was already registered in setUp() — registering it again should conflict
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "Another User",
                                    "email": "%s",
                                    "password": "anotherpassword"
                                }
                                """.formatted(TEST_EMAIL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").containsString(TEST_EMAIL));
    }

    @Test
    @DisplayName("register: missing email and short password → 400 with fieldErrors list")
    void register_withMissingFields_shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "Bob",
                                    "email": "",
                                    "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                // Both email and password violations should be reported at once
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        hasItems("email", "password")));
    }

    // =========================================================================
    // POST /auth/login
    // =========================================================================

    @Test
    @DisplayName("login: correct credentials → 200 OK with tokens")
    void login_withValidCredentials_shouldReturn200WithTokens() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """.formatted(TEST_EMAIL, TEST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL));
    }

    @Test
    @DisplayName("login: wrong password → 401 Unauthorized with generic message (no user enumeration)")
    void login_withWrongPassword_shouldReturn401WithGenericMessage() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "completely-wrong-password"
                                }
                                """.formatted(TEST_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                // Generic message — should NOT say "wrong password" or "password incorrect"
                // as that would confirm the email exists (user enumeration vulnerability)
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // =========================================================================
    // GET /users/me — protected endpoint
    // =========================================================================

    @Test
    @DisplayName("GET /users/me without token → 401 Unauthorized from AuthenticationEntryPoint")
    void getProfile_withoutBearerToken_shouldReturn401() throws Exception {
        // No Authorization header → Spring Security blocks at filter level (before controller)
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /users/me with valid token → 200 OK with user profile")
    void getProfile_withValidToken_shouldReturn200() throws Exception {
        // First, login to get a valid token
        String accessToken = loginAndGetToken(TEST_EMAIL, TEST_PASSWORD);

        // Then, use it to call the protected endpoint
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.fullName").value(TEST_NAME));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Registers a user via the API — same path as a real client.
     * Used in @BeforeEach to set up a known user for login/profile tests.
     */
    private void registerUser(String fullName, String email, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "%s",
                                    "email": "%s",
                                    "password": "%s"
                                }
                                """.formatted(fullName, email, password)))
                .andExpect(status().isCreated());
    }

    /**
     * Logs in and extracts the access token from the JSON response.
     * Used by tests that need an authenticated request.
     *
     * We parse the response body as a tree and extract the scalar value.
     * This avoids deserializing into AuthResponse (which has @Builder, no @NoArgsConstructor,
     * making Jackson deserialization tricky without extra config).
     */
    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Use Jackson's readTree to navigate JSON without a full POJO
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        return mapper.readTree(body).get("accessToken").asText();
    }
}
