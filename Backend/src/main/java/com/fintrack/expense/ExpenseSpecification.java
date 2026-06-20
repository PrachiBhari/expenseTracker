package com.fintrack.expense;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Specifications for dynamic expense filtering.
 *
 * WHAT IS A SPECIFICATION?
 * A Specification is a functional interface:
 *   Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb)
 *
 * Each static method here returns one filter condition (predicate).
 * In the service, we COMPOSE them:
 *   Specification.where(belongsToUser(userId))
 *     .and(fromDate(from))
 *     .and(toDate(to))
 *     .and(...)
 *
 * KEY BEHAVIOUR: returning null means "no filter applied" for that parameter.
 * Spring Data handles null predicates gracefully in .and() / .where().
 * This is what makes Specifications composable without if/else chains.
 *
 * ALTERNATIVE PATTERN (avoided here):
 *   One giant @Query with dynamic JPQL: "SELECT e FROM Expense e WHERE
 *   (:userId IS NULL OR e.user.id = :userId) AND (:from IS NULL OR e.expenseDate >= :from) ..."
 *   This works but is a single string, hard to test, and grows unreadable.
 *   Specifications are type-safe, individually testable, and reusable.
 *
 * Static utility class — no @Component needed (not a Spring bean).
 */
public final class ExpenseSpecification {

    private ExpenseSpecification() {}

    /**
     * MANDATORY filter — always applied.
     * Every query is scoped to the current user's data.
     * Without this, one user could see another's expenses.
     *
     * root.get("user").get("id") → expense.user_id column (via JPA navigation)
     */
    public static Specification<Expense> belongsToUser(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }

    /**
     * Filter: expense_date >= from
     * Returns null if 'from' is not provided → filter not applied.
     */
    public static Specification<Expense> fromDate(LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
    }

    /**
     * Filter: expense_date <= to
     * Returns null if 'to' is not provided → filter not applied.
     */
    public static Specification<Expense> toDate(LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("expenseDate"), to);
    }

    /**
     * Filter: category_id = categoryId
     * Returns null if categoryId is not provided.
     */
    public static Specification<Expense> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    /**
     * Filter: description ILIKE '%q%' (case-insensitive text search)
     *
     * cb.lower() converts both sides to lowercase → case-insensitive matching.
     * Equivalent SQL: WHERE LOWER(description) LIKE LOWER('%groceries%')
     *
     * Returns null if q is null or blank → filter not applied.
     *
     * Note: LIKE with leading % cannot use an index efficiently.
     * For MVP scale this is fine. At larger scale: full-text search (PostgreSQL tsvector).
     */
    public static Specification<Expense> descriptionContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("description")), pattern);
        };
    }

    /**
     * Filter: amount between min and max (inclusive).
     * Handles all four cases: both, only min, only max, neither.
     */
    public static Specification<Expense> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("amount"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("amount"), min);
            }
            if (max != null) {
                return cb.lessThanOrEqualTo(root.get("amount"), max);
            }
            return null;
        };
    }
}
