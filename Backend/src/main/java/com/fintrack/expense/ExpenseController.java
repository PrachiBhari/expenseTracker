package com.fintrack.expense;

import com.fintrack.common.dto.PageResponse;
import com.fintrack.expense.dto.ExpenseRequest;
import com.fintrack.expense.dto.ExpenseResponse;
import com.fintrack.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * REST controller for Expense CRUD.
 * All endpoints are PROTECTED.
 *
 * Full paths (context-path = /api/v1):
 *   GET    /api/v1/expenses           → paginated list with filters
 *   POST   /api/v1/expenses           → create
 *   GET    /api/v1/expenses/{id}      → get one
 *   PUT    /api/v1/expenses/{id}      → update (full replace)
 *   DELETE /api/v1/expenses/{id}      → delete
 *
 * PAGINATION (GET /expenses):
 * Spring resolves Pageable from query params automatically:
 *   ?page=0&size=20&sort=expenseDate,desc
 *
 * @PageableDefault sets defaults when these params are absent:
 *   page  = 0 (first page)
 *   size  = 20 (20 items per page)
 *   sort  = expenseDate DESC (most recent first, as required by PRD US-2.2 AC1)
 *
 * FILTER PARAMS (all optional, all passed to service):
 *   from, to         — date range
 *   categoryId       — specific category
 *   minAmount, maxAmount — amount range
 *   q                — text search on description
 */
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Manage expense records")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "List expenses with optional filters and pagination")
    public ResponseEntity<PageResponse<ExpenseResponse>> getExpenses(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q,

            @PageableDefault(size = 20, sort = "expenseDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                expenseService.getExpenses(userId, from, to, categoryId, minAmount, maxAmount, q, pageable)
        );
    }

    @PostMapping
    @Operation(summary = "Create a new expense")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single expense by ID")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(expenseService.getExpenseById(id, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an expense")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(expenseService.updateExpense(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an expense")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        expenseService.deleteExpense(id, userId);
        return ResponseEntity.noContent().build();
    }
}
