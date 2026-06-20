package com.fintrack.income;

import com.fintrack.category.Category;
import com.fintrack.category.CategoryRepository;
import com.fintrack.category.CategoryType;
import com.fintrack.common.dto.PageResponse;
import com.fintrack.common.exception.ResourceNotFoundException;
import com.fintrack.income.dto.IncomeRequest;
import com.fintrack.income.dto.IncomeResponse;
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
 * Business logic for Income CRUD operations.
 *
 * Mirrors ExpenseService with one key difference:
 * Category is OPTIONAL for income.
 *   - null categoryId → income.category = null (uncategorized)
 *   - non-null categoryId → validated for ownership AND INCOME type
 *
 * The type check is the inverse of expenses:
 *   Expense.resolveCategory → must be CategoryType.EXPENSE
 *   Income.resolveCategory  → must be CategoryType.INCOME
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final IncomeMapper incomeMapper;

    @Transactional(readOnly = true)
    public PageResponse<IncomeResponse> getIncomes(
            Long userId,
            LocalDate from, LocalDate to,
            Long categoryId,
            BigDecimal minAmount, BigDecimal maxAmount,
            String q,
            Pageable pageable) {

        Specification<Income> spec = Specification
                .where(IncomeSpecification.belongsToUser(userId))
                .and(IncomeSpecification.fromDate(from))
                .and(IncomeSpecification.toDate(to))
                .and(IncomeSpecification.hasCategory(categoryId))
                .and(IncomeSpecification.amountBetween(minAmount, maxAmount))
                .and(IncomeSpecification.descriptionContains(q));

        Page<IncomeResponse> page = incomeRepository
                .findAll(spec, pageable)
                .map(incomeMapper::toResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public IncomeResponse getIncomeById(Long id, Long userId) {
        return incomeMapper.toResponse(findOwnedIncome(id, userId));
    }

    @Transactional
    public IncomeResponse createIncome(IncomeRequest request, Long userId) {
        Category category = resolveCategory(request.getCategoryId(), userId);

        Income income = incomeMapper.toEntity(request);
        income.setUser(userRepository.getReferenceById(userId));
        income.setCategory(category);  // may be null — that's valid for income

        Income saved = incomeRepository.save(income);
        log.info("Income created: id={}, amount={}, userId={}", saved.getId(), saved.getAmount(), userId);
        return incomeMapper.toResponse(saved);
    }

    @Transactional
    public IncomeResponse updateIncome(Long id, IncomeRequest request, Long userId) {
        Income income = findOwnedIncome(id, userId);
        Category category = resolveCategory(request.getCategoryId(), userId);

        incomeMapper.updateEntity(request, income);
        income.setCategory(category);

        log.info("Income updated: id={}, userId={}", id, userId);
        return incomeMapper.toResponse(income);
    }

    @Transactional
    public void deleteIncome(Long id, Long userId) {
        Income income = findOwnedIncome(id, userId);
        incomeRepository.delete(income);
        log.info("Income deleted: id={}, userId={}", id, userId);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private Income findOwnedIncome(Long id, Long userId) {
        return incomeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Income", id));
    }

    /**
     * Resolves an optional category for income.
     * Returns null if categoryId is null — uncategorized income is allowed.
     * If categoryId is provided, validates ownership and INCOME type.
     */
    private Category resolveCategory(Long categoryId, Long userId) {
        if (categoryId == null) {
            return null;  // uncategorized income is valid
        }

        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));

        if (category.getType() != CategoryType.INCOME) {
            throw new IllegalArgumentException(
                    "Category '" + category.getName() + "' is an EXPENSE category. " +
                    "Income records must use an INCOME category.");
        }
        return category;
    }
}
