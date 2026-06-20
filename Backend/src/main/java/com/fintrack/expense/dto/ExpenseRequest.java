package com.fintrack.expense.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for POST /expenses and PUT /expenses/{id}.
 *
 * Validation rules (PRD US-2.1 AC1 and AC2):
 *
 * amount:
 *   @NotNull        — required field
 *   @DecimalMin("0.01") — must be greater than 0 (smallest 2-decimal positive value)
 *   @Digits(10, 2)  — max 10 integer digits, exactly 2 decimal places
 *                     prevents "250.999" (3 decimal places) from being accepted
 *
 * categoryId:
 *   @NotNull — required; service validates that it belongs to this user and is EXPENSE type
 *
 * expenseDate:
 *   @NotNull — required; no restriction on past/future (user may enter old expenses)
 *
 * description, paymentMethod: optional — no @NotBlank, can be null
 */
@Getter
@NoArgsConstructor
public class ExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2,
            message = "Amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private Long categoryId;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @Size(max = 40, message = "Payment method must not exceed 40 characters")
    private String paymentMethod;
}
