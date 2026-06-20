package com.fintrack.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Category entity.
 *
 * All query methods are scoped to a userId — a user can NEVER see another
 * user's categories, even if they guess the category ID.
 *
 * The pattern  findBy{Field}And{Field}  generates:
 *   SELECT * FROM categories WHERE id = ? AND user_id = ?
 * This is the ownership check — returns empty if the id belongs to another user.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * List all categories for a user, sorted alphabetically.
     * Used for populating the category dropdown in the transaction form.
     */
    List<Category> findByUserIdOrderByNameAsc(Long userId);

    /**
     * List categories filtered by type (INCOME or EXPENSE).
     * Used when the frontend requests: GET /categories?type=EXPENSE
     */
    List<Category> findByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);

    /**
     * Fetch a single category, scoped to the user (ownership check).
     * Returns empty if the category doesn't exist OR belongs to another user.
     * The service returns 404 in both cases — we don't reveal whether
     * the ID exists (avoids data enumeration).
     */
    Optional<Category> findByIdAndUserId(Long id, Long userId);

    /**
     * Check for duplicate category name + type per user before creating.
     * Used in CategoryService to give a clear 409 Conflict error.
     */
    boolean existsByNameAndTypeAndUserId(String name, CategoryType type, Long userId);

    /**
     * Check for duplicates on update — excludes the current category itself.
     * Prevents false conflict when updating a category with the same name.
     */
    boolean existsByNameAndTypeAndUserIdAndIdNot(String name, CategoryType type, Long userId, Long id);
}
