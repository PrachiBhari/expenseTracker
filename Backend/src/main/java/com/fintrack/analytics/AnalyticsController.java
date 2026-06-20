package com.fintrack.analytics;

import com.fintrack.analytics.dto.CategoryBreakdownItem;
import com.fintrack.analytics.dto.SummaryResponse;
import com.fintrack.analytics.dto.TrendPoint;
import com.fintrack.category.CategoryType;
import com.fintrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Analytics endpoints.
 *
 * All endpoints are read-only (GET) — analytics never modify data.
 * All endpoints are PROTECTED — only authenticated users can see their own data.
 *
 * Full paths (context-path = /api/v1):
 *   GET /api/v1/analytics/summary        → headline totals for a date range
 *   GET /api/v1/analytics/by-category    → spending breakdown per category
 *   GET /api/v1/analytics/trends         → month-by-month income vs expense
 *
 * Interview: "Why is this a separate controller instead of a method on /expenses?"
 *   Analytics is a cross-cutting concern — it aggregates BOTH incomes and expenses.
 *   Putting it on /expenses or /incomes would be semantically wrong.
 *   Separate controller = separate responsibility = cleaner SRP.
 *
 * Interview: "What's the difference between @RequestParam(required=false) and defaultValue?"
 *   required=false → parameter is optional; Java receives null when absent.
 *     Use this when the service needs to know "was a value provided?" (e.g. to default to current month).
 *   defaultValue → Spring always provides a value; Java never sees null.
 *     Use this when the default is a fixed constant (e.g. type=EXPENSE, months=6).
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Dashboard summary, category breakdown, and trend data")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /analytics/summary?from=2026-06-01&to=2026-06-30
     *
     * Returns total income, total expense, and net balance for a period.
     * If from/to are omitted, defaults to the current calendar month.
     *
     * Response: { from, to, totalIncome, totalExpense, netBalance }
     * Status: 200 OK always (0 values for new users, never 404)
     */
    @GetMapping("/summary")
    @Operation(summary = "Get income/expense summary for a date range",
               description = "Defaults to the current month when from/to are not provided.")
    public ResponseEntity<SummaryResponse> getSummary(

            @Parameter(description = "Start date (inclusive), e.g. 2026-06-01. Defaults to first day of current month.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive), e.g. 2026-06-30. Defaults to last day of current month.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getSummary(userId, from, to));
    }

    /**
     * GET /analytics/by-category?type=EXPENSE&from=2026-06-01&to=2026-06-30
     *
     * Returns spend (or income) totals grouped by category, sorted by total DESC.
     * Drives the donut/pie chart on the dashboard.
     *
     * type=EXPENSE (default) → most common dashboard view
     * type=INCOME            → income source breakdown
     *
     * If from/to omitted, defaults to current month.
     *
     * Response: [ { categoryId, name, color, icon, total }, ... ]
     * Status: 200 OK always (empty list if no data)
     */
    @GetMapping("/by-category")
    @Operation(summary = "Get spending or income breakdown by category",
               description = "Sorted by total descending. Defaults to EXPENSE type and current month.")
    public ResponseEntity<List<CategoryBreakdownItem>> getCategoryBreakdown(

            @Parameter(description = "EXPENSE (default) or INCOME")
            @RequestParam(required = false, defaultValue = "EXPENSE") CategoryType type,

            @Parameter(description = "Start date (inclusive). Defaults to first day of current month.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive). Defaults to last day of current month.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(userId, type, from, to));
    }

    /**
     * GET /analytics/trends?months=6
     *
     * Returns one TrendPoint per calendar month for the last N months.
     * Each point has both income and expense totals for that month.
     * Drives the line or bar chart showing income vs expense over time.
     *
     * months=6  → last 6 months (default)
     * months=12 → last 12 months (year view)
     *
     * The list is ALWAYS contiguous — months with no transactions get 0 values.
     * List is sorted chronologically (oldest first) — chart libraries expect this.
     *
     * Response: [ { month: "2026-01", income: 0, expense: 0 }, ..., { month: "2026-06", ... } ]
     * Status: 200 OK always
     */
    @GetMapping("/trends")
    @Operation(summary = "Get monthly income vs expense trend",
               description = "Returns one data point per month for the last N months. " +
                             "Months with no transactions are included with 0 values.")
    public ResponseEntity<List<TrendPoint>> getTrends(

            @Parameter(description = "Number of months to look back (default 6, max 24)")
            @RequestParam(required = false, defaultValue = "6") int months) {

        // Guard against absurdly large ranges that would query years of data
        int clampedMonths = Math.min(Math.max(months, 1), 24);

        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getTrends(userId, clampedMonths));
    }
}
