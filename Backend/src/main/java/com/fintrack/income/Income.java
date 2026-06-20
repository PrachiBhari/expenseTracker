package com.fintrack.income;

import com.fintrack.category.Category;
import com.fintrack.common.BaseEntity;
import com.fintrack.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Entity: maps to the 'incomes' table.
 *
 * Mirror of Expense with domain differences:
 *
 * 1. category_id is NULLABLE (income may be uncategorized).
 *    In Expense, category is required; in Income, it's optional.
 *    @JoinColumn without nullable = false allows NULL.
 *
 * 2. source field instead of paymentMethod:
 *    Records where the money came from (e.g., "Employer", "Freelance Client").
 *    Distinct from a category — a category is "Salary" (what kind of income),
 *    a source is "TechCorp Pvt Ltd" (who paid).
 *
 * 3. incomeDate instead of expenseDate:
 *    Same concept: the calendar date money was received.
 *
 * Everything else (BigDecimal for amount, LocalDate, LAZY fetch) is identical
 * to Expense — same reasoning applies.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "incomes")
public class Income extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Category is optional for income records.
     * No nullable = false here — the column allows NULL.
     * In the UI, income can be logged without picking a category.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Where the money came from: employer name, client name, "Side project", etc.
     * Separate from category — more specific, user-defined free text.
     */
    @Column(length = 120)
    private String source;

    @Column(length = 255)
    private String description;

    @Column(name = "income_date", nullable = false)
    private LocalDate incomeDate;
}
