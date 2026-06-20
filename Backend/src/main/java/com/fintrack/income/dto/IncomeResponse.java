package com.fintrack.income.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for a single income record.
 *
 * Mirrors ExpenseResponse with two domain differences:
 *   1. category is nullable — can be null for uncategorized income.
 *      @JsonInclude(NON_NULL) could be used to omit null from JSON,
 *      but sending null explicitly is cleaner for the frontend:
 *      it can check `if (income.category === null)` without ambiguity.
 *
 *   2. source instead of paymentMethod.
 */
@Getter
@Builder
public class IncomeResponse {

    private Long id;
    private BigDecimal amount;
    private CategorySummary category;   // nullable — income may be uncategorized
    private String source;
    private String description;
    private LocalDate incomeDate;
    private Instant createdAt;

    @Getter
    @Builder
    public static class CategorySummary {
        private Long id;
        private String name;
        private String color;
        private String icon;
    }
}
