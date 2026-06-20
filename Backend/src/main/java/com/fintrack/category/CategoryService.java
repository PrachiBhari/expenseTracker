package com.fintrack.category;

import com.fintrack.category.dto.CategoryRequest;
import com.fintrack.category.dto.CategoryResponse;
import com.fintrack.common.exception.CategoryInUseException;
import com.fintrack.common.exception.DuplicateResourceException;
import com.fintrack.common.exception.ResourceNotFoundException;
import com.fintrack.expense.ExpenseRepository;
import com.fintrack.income.IncomeRepository;
import com.fintrack.user.User;
import com.fintrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for Category CRUD operations.
 *
 * Key responsibilities:
 *   1. Ownership enforcement — every operation is scoped to userId
 *   2. Uniqueness check — (name + type + userId) must be unique
 *   3. Deletion safety — block delete if category has any transactions
 *   4. Mapping — entity ↔ DTO conversion via CategoryMapper
 *
 * All public methods are @Transactional:
 *   - readOnly = true for GETs (performance: no dirty checking, read-only DB connection)
 *   - default (read-write) for write operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    // =========================================================================
    // READ
    // =========================================================================

    /**
     * List all categories for the current user.
     * Optionally filter by type (INCOME or EXPENSE).
     * Sorted alphabetically by name.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long userId, CategoryType type) {
        List<Category> categories = (type != null)
                ? categoryRepository.findByUserIdAndTypeOrderByNameAsc(userId, type)
                : categoryRepository.findByUserIdOrderByNameAsc(userId);

        return categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    /**
     * Get a single category by ID.
     * Returns 404 if not found OR if it belongs to another user.
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id, Long userId) {
        Category category = findOwnedCategory(id, userId);
        return categoryMapper.toResponse(category);
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, Long userId) {
        validateNameUniqueness(request.getName(), request.getType(), userId, null);

        Category category = categoryMapper.toEntity(request);
        // getReferenceById() returns a Hibernate proxy — no SELECT query issued.
        // When JPA saves the category, it uses userId as the FK value.
        category.setUser(userRepository.getReferenceById(userId));

        Category saved = categoryRepository.save(category);
        log.info("Category created: id={}, name={}, userId={}", saved.getId(), saved.getName(), userId);
        return categoryMapper.toResponse(saved);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, Long userId) {
        Category category = findOwnedCategory(id, userId);
        validateNameUniqueness(request.getName(), request.getType(), userId, id);

        // updateEntity mutates 'category' in-place: name, type, color, icon
        // JPA detects the dirty fields and issues UPDATE on transaction commit
        categoryMapper.updateEntity(request, category);

        log.info("Category updated: id={}, userId={}", id, userId);
        return categoryMapper.toResponse(category);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Deletes a category — BLOCKED if any expense or income references it.
     *
     * Why check at service layer and not rely on the DB FK constraint?
     * The DB RESTRICT constraint would throw a DataIntegrityViolationException
     * with a cryptic message. We check first and throw CategoryInUseException
     * with a clear, human-readable message that the frontend can display.
     */
    @Transactional
    public void deleteCategory(Long id, Long userId) {
        Category category = findOwnedCategory(id, userId);

        boolean usedInExpenses = expenseRepository.existsByCategoryId(id);
        boolean usedInIncomes  = incomeRepository.existsByCategoryId(id);

        if (usedInExpenses || usedInIncomes) {
            throw new CategoryInUseException(category.getName());
        }

        categoryRepository.delete(category);
        log.info("Category deleted: id={}, name={}, userId={}", id, category.getName(), userId);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Finds a category that belongs to the user, or throws 404.
     * Used in every write and single-read operation.
     * The query includes userId in the WHERE clause — SQL-level ownership check.
     */
    private Category findOwnedCategory(Long id, Long userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    /**
     * Validates category name + type uniqueness for this user.
     * On update, excludes the category being updated (excludeId) to allow
     * saving a category with the same name it already has.
     *
     * Example: User updates "Food" EXPENSE to just change its color.
     *   Without excludeId: "Food + EXPENSE already exists" → false conflict
     *   With    excludeId: "Food + EXPENSE exists but it's the same record" → OK
     */
    private void validateNameUniqueness(String name, CategoryType type, Long userId, Long excludeId) {
        boolean duplicate;

        if (excludeId == null) {
            duplicate = categoryRepository.existsByNameAndTypeAndUserId(name, type, userId);
        } else {
            duplicate = categoryRepository.existsByNameAndTypeAndUserIdAndIdNot(name, type, userId, excludeId);
        }

        if (duplicate) {
            throw new DuplicateResourceException(
                    "A '" + type + "' category named '" + name + "' already exists.");
        }
    }
}
