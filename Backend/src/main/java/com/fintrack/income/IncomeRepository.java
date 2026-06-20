package com.fintrack.income;

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
 * Repository for Income entity.
 *
 * Identical structure to ExpenseRepository.
 * JpaSpecificationExecutor enables the same dynamic filtering pattern
 * (date range, category, amount range, text search) as expenses.
 *
 * The pattern is consistent: same endpoint shape, same filter params,
 * just a different resource type. This makes the API predictable.
 */
@Repository
public interface IncomeRepository extends JpaRepository<Income, Long>,
                                          JpaSpecificationExecutor<Income> {

    /**
     * Fetch a single income record with ownership check.
     * Returns empty if income doesn't exist or belongs to another user.
     */
    Optional<Income> findByIdAndUserId(Long id, Long userId);

    /**
     * Check if a category has any income records before allowing deletion.
     * Used alongside ExpenseRepository.existsByCategoryId() in CategoryService.
     */
    boolean existsByCategoryId(Long categoryId);

    // =========================================================================
    // Analytics queries
    // =========================================================================

    /**
     * Total income amount for a user in a date range.
     * Returns Optional because SUM returns NULL when no rows match.
     */
    @Query("SELECT SUM(i.amount) FROM Income i " +
           "WHERE i.user.id = :userId AND i.incomeDate BETWEEN :from AND :to")
    Optional<BigDecimal> sumAmountByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Income totals grouped by category, ordered by total DESC.
     * INNER JOIN excludes uncategorized income (no category → not in breakdown).
     *
     * Returns Object[] rows where:
     *   [0] Long       — category id
     *   [1] String     — category name
     *   [2] String     — category color (nullable)
     *   [3] String     — category icon (nullable)
     *   [4] BigDecimal — SUM(amount)
     */
    @Query("SELECT c.id, c.name, c.color, c.icon, SUM(i.amount) " +
           "FROM Income i JOIN i.category c " +
           "WHERE i.user.id = :userId AND i.incomeDate BETWEEN :from AND :to " +
           "GROUP BY c.id, c.name, c.color, c.icon " +
           "ORDER BY SUM(i.amount) DESC")
    List<Object[]> findIncomeCategoryBreakdown(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /**
     * Monthly income totals for trend charts.
     *
     * Returns Object[] rows where:
     *   [0] Integer    — year
     *   [1] Integer    — month (1–12)
     *   [2] BigDecimal — SUM(amount)
     *
     * Only months with at least one income record are returned.
     */
    @Query(value =
           "SELECT EXTRACT(YEAR FROM income_date)::integer, " +
           "       EXTRACT(MONTH FROM income_date)::integer, " +
           "       SUM(amount) " +
           "FROM incomes " +
           "WHERE user_id = :userId AND income_date >= :from " +
           "GROUP BY EXTRACT(YEAR FROM income_date), EXTRACT(MONTH FROM income_date) " +
           "ORDER BY EXTRACT(YEAR FROM income_date), EXTRACT(MONTH FROM income_date)",
           nativeQuery = true)
    List<Object[]> findMonthlyIncomeTotals(
            @Param("userId") Long userId,
            @Param("from") LocalDate from);
}
