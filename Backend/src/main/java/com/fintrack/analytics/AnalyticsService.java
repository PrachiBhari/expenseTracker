package com.fintrack.analytics;

import com.fintrack.analytics.dto.CategoryBreakdownItem;
import com.fintrack.analytics.dto.SummaryResponse;
import com.fintrack.analytics.dto.TrendPoint;
import com.fintrack.category.CategoryType;
import com.fintrack.expense.ExpenseRepository;
import com.fintrack.income.IncomeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business logic for all analytics endpoints.
 *
 * Three responsibilities:
 *
 * 1. SUMMARY — total income, total expense, net balance for a date range.
 *    Two aggregate queries (one for income, one for expense), one subtraction.
 *
 * 2. CATEGORY BREAKDOWN — how much was spent (or earned) per category.
 *    One aggregate query, GROUP BY category, ordered by total DESC.
 *    Returns the donut chart data.
 *
 * 3. TRENDS — month-by-month income vs expense for the last N months.
 *    Two aggregate queries (one per transaction type), merged in Java.
 *    Always returns a CONTIGUOUS list — months with no transactions get 0.
 *
 * All methods are @Transactional(readOnly = true).
 * readOnly = true tells Hibernate to skip dirty checking, which speeds up reads.
 * It also lets the database driver use read replicas when available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    // =========================================================================
    // Summary
    // =========================================================================

    /**
     * Returns headline totals for a date range.
     *
     * Default period (when from/to are null): the current calendar month.
     * Example: called on 2026-06-15 → from=2026-06-01, to=2026-06-30.
     *
     * Interview: "How does netBalance handle new users with no transactions?"
     *   SUM returns NULL from the DB when there are no rows.
     *   We use Optional.orElse(BigDecimal.ZERO) to convert null → 0.
     *   netBalance will be 0.00 — a safe, sensible default.
     */
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(Long userId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo   = (to   != null) ? to   : LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        BigDecimal totalIncome = incomeRepository
                .sumAmountByUserAndDateRange(userId, effectiveFrom, effectiveTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalExpense = expenseRepository
                .sumAmountByUserAndDateRange(userId, effectiveFrom, effectiveTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        log.debug("Summary for userId={}: income={}, expense={}, net={}", userId, totalIncome, totalExpense, netBalance);

        return SummaryResponse.builder()
                .from(effectiveFrom)
                .to(effectiveTo)
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .build();
    }

    // =========================================================================
    // Category Breakdown
    // =========================================================================

    /**
     * Returns how much was spent (or earned) per category in a date range.
     *
     * type=EXPENSE → breakdown of spending (default, most common)
     * type=INCOME  → breakdown of income sources by category
     *
     * The query uses INNER JOIN, so:
     *   - Uncategorized income records are excluded (income.category can be null).
     *   - Expenses always have a category (NOT NULL FK), so none are excluded.
     *
     * Interview: "Why ORDER BY SUM(amount) DESC?"
     *   The frontend pie/donut chart renders slices in order.
     *   Largest slice first = most important category visually prominent.
     *   This saves a client-side sort.
     */
    @Transactional(readOnly = true)
    public List<CategoryBreakdownItem> getCategoryBreakdown(
            Long userId, CategoryType type, LocalDate from, LocalDate to) {

        LocalDate effectiveFrom = (from != null) ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo   = (to   != null) ? to   : LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        List<Object[]> rows = (type == CategoryType.INCOME)
                ? incomeRepository.findIncomeCategoryBreakdown(userId, effectiveFrom, effectiveTo)
                : expenseRepository.findExpenseCategoryBreakdown(userId, effectiveFrom, effectiveTo);

        return rows.stream()
                .map(this::mapToCategoryBreakdownItem)
                .toList();
    }

    // =========================================================================
    // Trends
    // =========================================================================

    /**
     * Returns month-by-month income and expense for the last N months.
     *
     * Algorithm:
     *   1. Calculate startDate = first day of (now - months).
     *      E.g. months=6, today=2026-06-15 → startDate=2026-01-01
     *
     *   2. Query DB for monthly expense totals (only months with data are returned).
     *   3. Query DB for monthly income totals (same).
     *
     *   4. Build a map: "YYYY-MM" → BigDecimal for each type.
     *
     *   5. Walk through every month from startDate to today.
     *      For each, look up the value in the map (or use ZERO if not present).
     *      This guarantees a CONTIGUOUS list with no gaps.
     *
     * Interview: "What does contiguous mean and why does it matter?"
     *   It means every month in the range appears in the response, even if the
     *   user had no transactions that month. Chart libraries (Chart.js, Recharts)
     *   need a data point per x-axis label. Missing points cause rendering bugs.
     *   We solve this server-side so the frontend doesn't have to.
     *
     * Interview: "Why a Map<String, BigDecimal> keyed by 'YYYY-MM'?"
     *   Strings are safe keys: "2026-06".compareTo("2026-01") works correctly
     *   because the format is lexicographically sortable (year first, then month).
     *   Using a composite Integer key (year*100+month) is an alternative but less
     *   readable.
     */
    @Transactional(readOnly = true)
    public List<TrendPoint> getTrends(Long userId, int months) {
        LocalDate startDate = LocalDate.now()
                .minusMonths(months)
                .withDayOfMonth(1);

        // Query — only months that have at least one transaction are returned
        List<Object[]> expenseRows = expenseRepository.findMonthlyExpenseTotals(userId, startDate);
        List<Object[]> incomeRows  = incomeRepository.findMonthlyIncomeTotals(userId, startDate);

        // Build lookup maps: "YYYY-MM" → total
        Map<String, BigDecimal> expenseByMonth = toMonthMap(expenseRows);
        Map<String, BigDecimal> incomeByMonth  = toMonthMap(incomeRows);

        // Walk every month from startDate to today, filling zeros for empty months
        List<TrendPoint> trends = new ArrayList<>();
        LocalDate cursor = startDate;
        LocalDate today  = LocalDate.now();

        while (!cursor.isAfter(today)) {
            String monthKey = cursor.format(MONTH_FORMATTER);   // "2026-01"
            trends.add(TrendPoint.builder()
                    .month(monthKey)
                    .income(incomeByMonth.getOrDefault(monthKey, BigDecimal.ZERO))
                    .expense(expenseByMonth.getOrDefault(monthKey, BigDecimal.ZERO))
                    .build());
            cursor = cursor.plusMonths(1);
        }

        return trends;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Maps a raw Object[] row from the category-breakdown native/JPQL query
     * to a CategoryBreakdownItem DTO.
     *
     * Row layout: [categoryId, name, color, icon, total]
     *
     * Interview: "Why cast to (Long) rather than using row[0].toString()?"
     *   JPQL returns id as Long (BIGINT → Long).
     *   For native queries, PostgreSQL BIGINT → Long via JDBC.
     *   Casting to the known type fails fast and visibly if something changes,
     *   rather than silently returning a wrong value.
     */
    private CategoryBreakdownItem mapToCategoryBreakdownItem(Object[] row) {
        return CategoryBreakdownItem.builder()
                .categoryId((Long) row[0])
                .name((String) row[1])
                .color((String) row[2])
                .icon((String) row[3])
                .total((BigDecimal) row[4])
                .build();
    }

    /**
     * Converts native-query rows (year, month, total) into a "YYYY-MM" → total map.
     *
     * The native query returns year/month as Integer (because of the ::integer cast).
     * Using ((Number) row[...]).intValue() is defensive: it works whether the JDBC
     * driver gives us Integer, Long, or BigInteger — all implement Number.
     *
     * Interview: "What if the DB returns a Double for EXTRACT?"
     *   Without ::integer in the SQL, PostgreSQL EXTRACT returns double precision.
     *   JDBC maps double precision to Double, which also implements Number.
     *   The ::integer cast forces PostgreSQL to return an integer type,
     *   but the Number cast is a belt-and-suspenders safety measure.
     */
    private Map<String, BigDecimal> toMonthMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal total = (BigDecimal) row[2];
            String key = String.format("%04d-%02d", year, month);
            map.put(key, total);
        }
        return map;
    }
}
