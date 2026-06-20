package com.fintrack.income.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for POST /incomes and PUT /incomes/{id}.
 *
 * Mirrors ExpenseRequest with two domain differences:
 *
 * 1. categoryId is NOT @NotNull — income can be uncategorized.
 *    The DB column allows NULL; the service handles null gracefully.
 *    This matches the TRD schema: incomes.category_id is nullable.
 *
 * 2. source instead of paymentMethod:
 *    Where the money came from (employer, client, etc.) rather than
 *    how it was paid.
 *
 * Validation rules for amount are identical to ExpenseRequest.
 */
@Getter
@NoArgsConstructor
public class IncomeRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2,
            message = "Amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    /**
     * Optional for income — income can be uncategorized.
     * When provided, service validates ownership + INCOME type.
     */
    private Long categoryId;

    @Size(max = 120, message = "Source must not exceed 120 characters")
    private String source;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Income date is required")
    private LocalDate incomeDate;
}
