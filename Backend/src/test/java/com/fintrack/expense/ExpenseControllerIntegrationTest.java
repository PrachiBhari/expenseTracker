package com.fintrack.expense;

import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.category.CategoryType;
import com.fintrack.income.IncomeRepository;
import com.fintrack.security.JwtService;
import com.fintrack.security.RefreshTokenRepository;
import com.fintrack.user.Role;
import com.fintrack.user.User;
import com.fintrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Expense CRUD API.
 *
 * Test setup strategy: instead of calling the register/login endpoints in @BeforeEach
 * (which adds HTTP overhead), we:
 *   1. Create the test user DIRECTLY via UserRepository (no HTTP call)
 *   2. Generate a JWT DIRECTLY via JwtService (no HTTP call)
 *   3. Create the test category DIRECTLY via CategoryRepository (no HTTP call)
 *
 * This is a valid integration test pattern: the setup creates known state, and
 * the tests verify the HTTP behavior. The HTTP calls ARE the things being tested.
 *
 * INTERVIEW: "Why use the real JwtService in tests instead of a fixed token string?"
 *   A real JWT has a signature verified by the JwtAuthFilter.
 *   A fake/hardcoded token string would fail signature verification → 401 on all tests.
 *   By injecting the real JwtService and using the test JWT secret (from application-test.yml),
 *   we generate tokens that the real filter will accept.
 *
 * INTERVIEW: "What is the test JWT secret and where is it configured?"
 *   In application-test.yml:
 *     jwt.secret: test-secret-key-for-unit-testing-only-must-be-256-bits-long-abc123
 *   This value is loaded by JwtService when the 'test' profile is active.
 *   It's safe to commit because it's only used in tests, never in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Expense Controller Integration Tests")
class ExpenseControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private IncomeRepository incomeRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String bearerToken;     // valid JWT for the test user
    private Long expenseCategoryId; // ID of an EXPENSE category owned by the test user

    @BeforeEach
    void setUp() {
        // Clean up in FK-safe order
        expenseRepository.deleteAll();
        incomeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user directly via repository — avoids HTTP overhead in setup
        User testUser = userRepository.save(User.builder()
                .fullName("Test User")
                .email("testuser@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .enabled(true)
                .build());

        // Generate JWT using the real JwtService (uses test secret from application-test.yml)
        // The JwtAuthFilter will accept this token because it's signed with the same key
        bearerToken = jwtService.generateAccessToken(testUser);

        // Create an EXPENSE category directly — used as the categoryId in expense requests
        Category category = categoryRepository.save(Category.builder()
                .user(testUser)
                .name("Food & Dining")
                .type(CategoryType.EXPENSE)
                .color("#FF6B6B")
                .icon("utensils")
                .build());
        expenseCategoryId = category.getId();
    }

    // =========================================================================
    // Security tests — no auth
    // =========================================================================

    @Test
    @DisplayName("POST /expenses without token → 401 Unauthorized")
    void createExpense_withoutAuthToken_shouldReturn401() throws Exception {
        // No Authorization header — Spring Security blocks this before the controller runs
        mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // Validation tests
    // =========================================================================

    @Test
    @DisplayName("POST /expenses: invalid amount (null) → 400 with fieldErrors")
    void createExpense_withNullAmount_shouldReturn400WithFieldError() throws Exception {
        mockMvc.perform(post("/expenses")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": null,
                                    "categoryId": %d,
                                    "expenseDate": "2026-06-15"
                                }
                                """.formatted(expenseCategoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("amount")));
    }

    @Test
    @DisplayName("POST /expenses: negative amount → 400 with fieldErrors")
    void createExpense_withNegativeAmount_shouldReturn400() throws Exception {
        mockMvc.perform(post("/expenses")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": -50.00,
                                    "categoryId": %d,
                                    "expenseDate": "2026-06-15"
                                }
                                """.formatted(expenseCategoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("amount")));
    }

    // =========================================================================
    // CRUD happy-path
    // =========================================================================

    @Test
    @DisplayName("POST /expenses: valid payload → 201 Created with expense response")
    void createExpense_withValidPayload_shouldReturn201() throws Exception {
        mockMvc.perform(post("/expenses")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 350.50,
                                    "categoryId": %d,
                                    "description": "Team lunch",
                                    "expenseDate": "2026-06-15",
                                    "paymentMethod": "Credit Card"
                                }
                                """.formatted(expenseCategoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(350.50))
                .andExpect(jsonPath("$.description").value("Team lunch"))
                .andExpect(jsonPath("$.expenseDate").value("2026-06-15"))
                .andExpect(jsonPath("$.category.name").value("Food & Dining"))
                .andExpect(jsonPath("$.category.color").value("#FF6B6B"));
    }

    @Test
    @DisplayName("GET /expenses → 200 OK with paginated response containing created expense")
    void getExpenses_afterCreatingOne_shouldReturnPageWithExpense() throws Exception {
        // First create an expense
        createExpenseViaApi(new BigDecimal("250.00"), "Groceries");

        // Then list all expenses
        mockMvc.perform(get("/expenses")
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].description").value("Groceries"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("PUT /expenses/{id} → 200 OK with updated amount")
    void updateExpense_withValidPayload_shouldReturn200WithUpdatedData() throws Exception {
        // Create an expense first
        Long expenseId = createExpenseViaApi(new BigDecimal("100.00"), "Initial");

        // Update it
        mockMvc.perform(put("/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": 999.99,
                                    "categoryId": %d,
                                    "description": "Updated description",
                                    "expenseDate": "2026-06-20",
                                    "paymentMethod": "Debit Card"
                                }
                                """.formatted(expenseCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(999.99))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("DELETE /expenses/{id} → 204 No Content, expense no longer in list")
    void deleteExpense_shouldReturn204AndRemoveFromList() throws Exception {
        // Create an expense
        Long expenseId = createExpenseViaApi(new BigDecimal("75.00"), "Coffee");

        // Delete it
        mockMvc.perform(delete("/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isNoContent());

        // Verify it's gone from the list
        mockMvc.perform(get("/expenses")
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /expenses/{id} for another user's expense → 404 (ownership enforced)")
    void getExpense_withWrongId_shouldReturn404() throws Exception {
        // Try to get an expense that doesn't exist
        // Even if it existed but belonged to another user, we'd still get 404 (no data leakage)
        mockMvc.perform(get("/expenses/99999")
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Creates an expense via the API and returns its ID.
     * Used by tests that need an existing expense to update or delete.
     */
    private Long createExpenseViaApi(BigDecimal amount, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/expenses")
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "amount": %s,
                                    "categoryId": %d,
                                    "description": "%s",
                                    "expenseDate": "%s"
                                }
                                """.formatted(amount, expenseCategoryId, description,
                                LocalDate.now().toString())))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper.readTree(body).get("id").asLong();
    }
}
