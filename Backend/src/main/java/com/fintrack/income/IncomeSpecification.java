package com.fintrack.income;

import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Specifications for dynamic income filtering.
 *
 * Identical pattern to ExpenseSpecification — the field names differ
 * (incomeDate vs expenseDate) but the logic is the same.
 *
 * This deliberate mirroring is a feature, not duplication:
 *   - Expense and Income are separate domain concepts
 *   - Their filter specs are independently testable
 *   - A change to expense filtering doesn't accidentally break income filtering
 *
 * Interview: "Why not make one generic TransactionSpecification?"
 *   Because Expense and Income are different JPA entities with different types.
 *   Specification<Expense> and Specification<Income> are not interchangeable.
 *   Generics could work but add complexity without real benefit at this scale.
 */
public final class IncomeSpecification {

    private IncomeSpecification() {}

    public static Specification<Income> belongsToUser(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Income> fromDate(LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("incomeDate"), from);
    }

    public static Specification<Income> toDate(LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("incomeDate"), to);
    }

    public static Specification<Income> hasCategory(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Income> descriptionContains(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return null;
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("description")), pattern);
        };
    }

    public static Specification<Income> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null) return cb.between(root.get("amount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("amount"), min);
            if (max != null) return cb.lessThanOrEqualTo(root.get("amount"), max);
            return null;
        };
    }
}
