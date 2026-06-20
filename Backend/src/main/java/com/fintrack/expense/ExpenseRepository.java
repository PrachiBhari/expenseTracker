package com.fintrack.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Expense entity.
 *
 * Extends TWO interfaces:
 *
 * 1. JpaRepository<Expense, Long>
 *    → Standard CRUD: save, findById, findAll, delete, count, etc.
 *
 * 2. JpaSpecificationExecutor<Expense>
 *    → Enables Specification-based dynamic queries.
 *      Used for the filter endpoint: GET /expenses?from=&to=&categoryId=&q=
 *      Specifications are composable predicates — each filter is a separate
 *      Specification, combined with and(). No giant JPQL if-else chains.
 *
 * Interview: "What is a JPA Specification?"
 *   A Specification is a Predicate factory — a lambda that receives a
 *   CriteriaBuilder and returns a WHERE clause fragment. Multiple specs
 *   are combined with Specification.where(s1).and(s2).and(s3).
 *   This is cleaner and more testable than building dynamic JPQL strings.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>,
                                           JpaSpecificationExecutor<Expense> {

    /**
     * Fetch a single expense — includes ownership check in the query.
     * Returns empty if: (1) expense doesn't exist, or (2) belongs to another user.
     * Service returns 404 in both cases (no information leakage).
     */
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    /**
     * Check if a category has any expenses before allowing deletion.
     * Used in CategoryService: if existsByCategoryId(id) → throw CategoryInUseException.
     */
    boolean existsByCategoryId(Long categoryId);

    // =========================================================================
    // Analytics queries
    // =========================================================================

    /**
     * Total expense amount for a user in a date range.
     * Returns Optional because SUM returns NULL when no rows match (no expenses yet).
     * Service calls .orElse(BigDecimal.ZERO) to handle that.
     */
    @Query("SELECT SUM(e.amount) FROM Expense e " +
           "WHERE e.user.id = :userId AND e.expenseDate BETWEEN :from AND :to")
    Optional<BigDecimal> sumAmountByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Expense totals grouped by category, ordered by total DESC.
     *
     * Returns Object[] rows where:
     *   [0] Long       — category id
     *   [1] String     — category name
     *   [2] String     — category color (nullable)
     *   [3] String     — category icon (nullable)
     *   [4] BigDecimal — SUM(amount)
     *
     * INNER JOIN means expenses with no category are excluded.
     * That is correct — the breakdown only shows categorised spend.
     */
    @Query("SELECT c.id, c.name, c.color, c.icon, SUM(e.amount) " +
           "FROM Expense e JOIN e.category c " +
           "WHERE e.user.id = :userId AND e.expenseDate BETWEEN :from AND :to " +
           "GROUP BY c.id, c.name, c.color, c.icon " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> findExpenseCategoryBreakdown(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Monthly expense totals for trend charts.
     * Native SQL used because EXTRACT on LocalDate columns is most reliable
     * via native PostgreSQL syntax with an explicit integer cast.
     *
     * Returns Object[] rows where:
     *   [0] Integer    — year  (e.g. 2026)
     *   [1] Integer    — month (1–12)
     *   [2] BigDecimal — SUM(amount)
     *
     * Only months with at least one expense are returned — the service
     * fills in BigDecimal.ZERO for months with no data.
     */
    @Query(value =
           "SELECT EXTRACT(YEAR FROM expense_date)::integer, " +
           "       EXTRACT(MONTH FROM expense_date)::integer, " +
           "       SUM(amount) " +
           "FROM expenses " +
           "WHERE user_id = :userId AND expense_date >= :from " +
           "GROUP BY EXTRACT(YEAR FROM expense_date), EXTRACT(MONTH FROM expense_date) " +
           "ORDER BY EXTRACT(YEAR FROM expense_date), EXTRACT(MONTH FROM expense_date)",
           nativeQuery = true)
    List<Object[]> findMonthlyExpenseTotals(
            @Param("userId") Long userId,
            @Param("from") LocalDate from);
}
