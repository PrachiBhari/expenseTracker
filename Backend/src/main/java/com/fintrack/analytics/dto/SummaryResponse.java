package com.fintrack.analytics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response for GET /analytics/summary?from=&to=
 *
 * Contains the three headline numbers shown on the dashboard:
 *   totalIncome   — sum of all income in the date range
 *   totalExpense  — sum of all expenses in the date range
 *   netBalance    — totalIncome minus totalExpense (can be negative)
 *
 * The date range is echoed back so the frontend can display "June 2026" etc.
 *
 * Interview: "Why echo from/to in the response?"
 *   Because the frontend may default the dates server-side (e.g. current month).
 *   Echoing lets the frontend know exactly which period the numbers cover,
 *   without the client having to track its own defaults.
 */
@Getter
@Builder
public class SummaryResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate from;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate to;

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;
}
