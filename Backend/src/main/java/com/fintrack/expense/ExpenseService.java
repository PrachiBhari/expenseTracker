package com.fintrack.expense;

import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.category.CategoryType;
import com.fintrack.common.dto.PageResponse;
import com.fintrack.common.exception.ResourceNotFoundException;
import com.fintrack.expense.dto.ExpenseRequest;
import com.fintrack.expense.dto.ExpenseResponse;
import com.fintrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Business logic for Expense CRUD operations.
 *
 * KEY PATTERNS:
 *
 * 1. Ownership is ALWAYS enforced at the query level.
 *    findByIdAndUserId(id, userId) → SQL: WHERE id=? AND user_id=?
 *    Even if an attacker guesses an expense ID, they get 404 (not 403).
 *
 * 2. Category validation on create/update:
 *    - Category must exist AND belong to this user (ownership check)
 *    - Category type must be EXPENSE (not INCOME)
 *    This prevents cross-contamination: an income category can't classify expenses.
 *
 * 3. Specifications for filtering:
 *    Dynamic filters are built as Specification objects and composed with .and().
 *    The base spec is always belongsToUser(userId) — it can never be removed.
 *    Additional filter specs are only added when the corresponding param is non-null.
 *
 * 4. @Transactional(readOnly=true) on reads:
 *    Hibernate won't track entity state changes (dirty checking) in read-only mode.
 *    Slight performance improvement, and signals intent to future readers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;

    // =========================================================================
    // READ — List with filtering + pagination
    // =========================================================================

    /**
     * Returns a page of expenses for the user, with optional filters applied.
     *
     * @param userId     current authenticated user (mandatory, security scope)
     * @param from       filter: expense_date >= from  (optional)
     * @param to         filter: expense_date <= to    (optional)
     * @param categoryId filter: category_id = ?       (optional)
     * @param minAmount  filter: amount >= minAmount   (optional)
     * @param maxAmount  filter: amount <= maxAmount   (optional)
     * @param q          filter: description ILIKE %q% (optional)
     * @param pageable   page number, size, sort direction (from request params)
     */
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpenses(
            Long userId,
            LocalDate from, LocalDate to,
            Long categoryId,
            BigDecimal minAmount, BigDecimal maxAmount,
            String q,
            Pageable pageable) {

        // Build the composed Specification.
        // belongsToUser() is MANDATORY — it's the security constraint.
        // All other specs return null when their param is null → ignored by JPA.
        Specification<Expense> spec = Specification
                .where(ExpenseSpecification.belongsToUser(userId))
                .and(ExpenseSpecification.fromDate(from))
                .and(ExpenseSpecification.toDate(to))
                .and(ExpenseSpecification.hasCategory(categoryId))
                .and(ExpenseSpecification.amountBetween(minAmount, maxAmount))
                .and(ExpenseSpecification.descriptionContains(q));

        // findAll(spec, pageable): Spring Data generates the SQL with WHERE + LIMIT + OFFSET
        Page<ExpenseResponse> page = expenseRepository
                .findAll(spec, pageable)
                .map(expenseMapper::toResponse);

        return PageResponse.from(page);
    }

    // =========================================================================
    // READ — Single
    // =========================================================================

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id, Long userId) {
        Expense expense = findOwnedExpense(id, userId);
        return expenseMapper.toResponse(expense);
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        Category category = resolveCategory(request.getCategoryId(), userId);

        Expense expense = expenseMapper.toEntity(request);
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setCategory(category);

        Expense saved = expenseRepository.save(expense);
        log.info("Expense created: id={}, amount={}, userId={}", saved.getId(), saved.getAmount(), userId);
        return expenseMapper.toResponse(saved);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long userId) {
        Expense expense = findOwnedExpense(id, userId);
        Category category = resolveCategory(request.getCategoryId(), userId);

        // updateEntity mutates only: amount, description, expenseDate, paymentMethod
        expenseMapper.updateEntity(request, expense);
        // category is set separately (mapper ignores it since request has categoryId, not Category)
        expense.setCategory(category);

        // No explicit save() needed — JPA detects dirty entity and issues UPDATE on commit
        log.info("Expense updated: id={}, userId={}", id, userId);
        return expenseMapper.toResponse(expense);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Transactional
    public void deleteExpense(Long id, Long userId) {
        Expense expense = findOwnedExpense(id, userId);
        expenseRepository.delete(expense);
        log.info("Expense deleted: id={}, userId={}", id, userId);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Loads an expense that belongs to the user.
     * Returns 404 whether the expense doesn't exist OR belongs to another user.
     */
    private Expense findOwnedExpense(Long id, Long userId) {
        return expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    /**
     * Validates and loads a category for use with an expense.
     *
     * Two security checks:
     * 1. Ownership: findByIdAndUserId → 404 if category belongs to another user
     * 2. Type check: category.type must be EXPENSE
     *    Prevents using an INCOME category to classify an expense — which would
     *    corrupt the analytics (income category would appear in expense charts).
     */
    private Category resolveCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        if (category.getType() != CategoryType.EXPENSE) {
            throw new IllegalArgumentException(
                    "Category '" + category.getName() + "' is an INCOME category. " +
                    "Expenses must use an EXPENSE category.");
        }
        return category;
    }
}
