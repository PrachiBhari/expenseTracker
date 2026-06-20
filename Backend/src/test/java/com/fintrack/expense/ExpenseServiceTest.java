package com.fintrack.expense;

import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.category.CategoryType;
import com.fintrack.common.exception.ResourceNotFoundException;
import com.fintrack.expense.dto.ExpenseRequest;
import com.fintrack.expense.dto.ExpenseResponse;
import com.fintrack.user.User;
import com.fintrack.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseService.
 *
 * Key security behaviour tested:
 *   - Category ownership: a category must belong to the same user as the expense
 *   - Category type: only EXPENSE categories are valid for expenses (not INCOME)
 *   - Expense ownership: findByIdAndUserId prevents cross-user access
 *
 * INTERVIEW: "Why do we need a category type check in the service?
 *             Isn't the category form in the UI enough?"
 *   Defense-in-depth: never trust the client. A malicious API consumer could
 *   send any categoryId. Without the type check, an INCOME category could appear
 *   in expense charts — corrupting the user's analytics data. Always validate at
 *   the API boundary regardless of what the UI does.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExpenseMapper expenseMapper;

    @InjectMocks private ExpenseService expenseService;

    private static final Long USER_ID = 1L;
    private static final Long EXPENSE_ID = 100L;
    private static final Long CATEGORY_ID = 10L;

    private User testUser;
    private Category expenseCategory;
    private Category incomeCategory;
    private Expense testExpense;
    private ExpenseResponse testResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(USER_ID)
                .email("user@example.com")
                .build();

        expenseCategory = Category.builder()
                .id(CATEGORY_ID)
                .user(testUser)
                .name("Food")
                .type(CategoryType.EXPENSE)  // correct type
                .build();

        incomeCategory = Category.builder()
                .id(99L)
                .user(testUser)
                .name("Salary")
                .type(CategoryType.INCOME)  // wrong type for expenses
                .build();

        testExpense = Expense.builder()
                .id(EXPENSE_ID)
                .user(testUser)
                .category(expenseCategory)
                .amount(new BigDecimal("500.00"))
                .description("Lunch")
                .expenseDate(LocalDate.of(2026, 6, 15))
                .build();

        testResponse = ExpenseResponse.builder()
                .id(EXPENSE_ID)
                .amount(new BigDecimal("500.00"))
                .description("Lunch")
                .expenseDate(LocalDate.of(2026, 6, 15))
                .createdAt(Instant.now())
                .build();
    }

    // =========================================================================
    // createExpense()
    // =========================================================================

    @Test
    @DisplayName("createExpense: valid request with owned EXPENSE category → saves and returns response")
    void createExpense_withValidRequest_shouldSaveAndReturnResponse() {
        // Arrange
        ExpenseRequest request = buildExpenseRequest(new BigDecimal("500.00"), CATEGORY_ID, "Lunch");

        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_ID))
                .thenReturn(Optional.of(expenseCategory));
        when(expenseMapper.toEntity(request)).thenReturn(testExpense);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(testUser);
        when(expenseRepository.save(testExpense)).thenReturn(testExpense);
        when(expenseMapper.toResponse(testExpense)).thenReturn(testResponse);

        // Act
        ExpenseResponse response = expenseService.createExpense(request, USER_ID);

        // Assert
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
        assertThat(response.getDescription()).isEqualTo("Lunch");

        verify(expenseRepository).save(testExpense);
    }

    @Test
    @DisplayName("createExpense: INCOME category used on expense → throws IllegalArgumentException")
    void createExpense_withIncomeCategoryType_shouldThrowIllegalArgumentException() {
        // Arrange — client sends an INCOME category for an expense (cross-contamination attack)
        ExpenseRequest request = buildExpenseRequest(new BigDecimal("500.00"), 99L, "Salary");

        when(categoryRepository.findByIdAndUserId(99L, USER_ID))
                .thenReturn(Optional.of(incomeCategory));  // returns INCOME category

        // Act + Assert
        assertThatThrownBy(() -> expenseService.createExpense(request, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INCOME");

        // Expense must NOT be saved
        verify(expenseRepository, never()).save(any());
    }

    @Test
    @DisplayName("createExpense: non-existent or unowned category → throws ResourceNotFoundException")
    void createExpense_withNonExistentCategory_shouldThrowResourceNotFoundException() {
        // Arrange
        ExpenseRequest request = buildExpenseRequest(new BigDecimal("200.00"), 999L, "Unknown");

        when(categoryRepository.findByIdAndUserId(999L, USER_ID))
                .thenReturn(Optional.empty());  // 404 — doesn't exist or belongs to another user

        // Act + Assert
        assertThatThrownBy(() -> expenseService.createExpense(request, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).save(any());
    }

    // =========================================================================
    // getExpenseById()
    // =========================================================================

    @Test
    @DisplayName("getExpenseById: owned expense → returns response")
    void getExpenseById_whenOwned_shouldReturnExpenseResponse() {
        // Arrange
        when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_ID))
                .thenReturn(Optional.of(testExpense));
        when(expenseMapper.toResponse(testExpense)).thenReturn(testResponse);

        // Act
        ExpenseResponse response = expenseService.getExpenseById(EXPENSE_ID, USER_ID);

        // Assert
        assertThat(response.getId()).isEqualTo(EXPENSE_ID);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("getExpenseById: not owned or doesn't exist → throws 404 (no data leakage)")
    void getExpenseById_whenNotOwned_shouldThrowResourceNotFoundException() {
        // findByIdAndUserId returns empty whether the expense doesn't exist OR belongs to another user.
        // The 404 response is the same in both cases — attacker can't determine if the ID exists.
        when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getExpenseById(EXPENSE_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // deleteExpense()
    // =========================================================================

    @Test
    @DisplayName("deleteExpense: owned expense → calls repository.delete")
    void deleteExpense_whenOwned_shouldDelete() {
        // Arrange
        when(expenseRepository.findByIdAndUserId(EXPENSE_ID, USER_ID))
                .thenReturn(Optional.of(testExpense));

        // Act
        expenseService.deleteExpense(EXPENSE_ID, USER_ID);

        // Assert
        // verify() checks that the specified method was called exactly once with the given argument
        verify(expenseRepository).delete(testExpense);
    }

    // =========================================================================
    // Test data builders
    // =========================================================================

    private ExpenseRequest buildExpenseRequest(BigDecimal amount, Long categoryId, String description) {
        ExpenseRequest request = new ExpenseRequest();
        ReflectionTestUtils.setField(request, "amount", amount);
        ReflectionTestUtils.setField(request, "categoryId", categoryId);
        ReflectionTestUtils.setField(request, "description", description);
        ReflectionTestUtils.setField(request, "expenseDate", LocalDate.of(2026, 6, 15));
        ReflectionTestUtils.setField(request, "paymentMethod", "Cash");
        return request;
    }
}
