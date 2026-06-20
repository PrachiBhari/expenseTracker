package com.fintrack.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * One data point on the income-vs-expense line/bar chart.
 *
 * Returned by GET /analytics/trends?months=6
 * Each point represents a single calendar month.
 *
 * month format: "YYYY-MM" (e.g. "2026-06")
 *   - Lexicographic sort is the same as chronological sort — no parsing needed.
 *   - Easy to display: the frontend can split on "-" to get year and month.
 *
 * If the user had no transactions in a month, income=0 and expense=0.
 * The service always generates a contiguous series with no gaps,
 * so the chart never has missing data points.
 *
 * Interview: "Why return 0 instead of omitting months with no data?"
 *   Charts need a continuous x-axis. If month 3 is missing, chart libraries
 *   either skip it (wrong spacing) or crash. Zero-filling here saves the
 *   frontend from handling that edge case.
 */
@Getter
@Builder
public class TrendPoint {

    private String month;       // "YYYY-MM"
    private BigDecimal income;
    private BigDecimal expense;
}
