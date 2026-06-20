package com.fintrack.expense;

import com.fintrack.category.Category;
import com.fintrack.common.BaseEntity;
import com.fintrack.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Entity: maps to the 'expenses' table.
 *
 * Key design decisions:
 *
 * 1. BigDecimal for amount (not double/float):
 *    double 0.1 + 0.2 = 0.30000000000000004 in binary floating-point.
 *    BigDecimal gives exact decimal arithmetic — essential for money.
 *    DB column is NUMERIC(12,2): 12 total digits, 2 after decimal point.
 *    Max value: 9,999,999,999.99 — more than enough for personal finance.
 *
 * 2. LocalDate for expenseDate (not LocalDateTime):
 *    Expenses happen on a DATE, not a specific time.
 *    Using LocalDate avoids timezone issues and matches the DB DATE column.
 *    The user picks a date in the UI, not a timestamp.
 *
 * 3. Two separate @ManyToOne relationships (user and category):
 *    - user: ownership — every query filters WHERE user_id = ?
 *    - category: classification — for grouping and analytics
 *    Both use FetchType.LAZY for performance.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owner of this expense record.
     * Every service method validates: does currentUserId == expense.user.id?
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The category this expense belongs to.
     * nullable = false: an expense must always have a category.
     * The DB FK has no CASCADE — deleting a category is blocked if it has expenses.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Exact decimal amount. Never use double for money.
     * precision = 12 digits total, scale = 2 decimal places.
     * DB CHECK constraint ensures amount > 0.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Optional free-text note (e.g., "Groceries from BigBasket").
     * Searchable via the text filter on the transactions page.
     */
    @Column(length = 255)
    private String description;

    /**
     * The calendar date of the expense (not timestamp).
     * LocalDate maps to PostgreSQL DATE column.
     */
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /**
     * Optional payment method: UPI, Cash, Debit Card, Credit Card, etc.
     * Free text — not an enum so users aren't restricted.
     */
    @Column(name = "payment_method", length = 40)
    private String paymentMethod;
}
