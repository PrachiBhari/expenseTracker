package com.fintrack.income;

import com.fintrack.common.dto.PageResponse;
import com.fintrack.income.dto.IncomeRequest;
import com.fintrack.income.dto.IncomeResponse;
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
 * REST controller for Income CRUD.
 * All endpoints are PROTECTED.
 *
 * Full paths (context-path = /api/v1):
 *   GET    /api/v1/incomes           → paginated list with filters
 *   POST   /api/v1/incomes           → create
 *   GET    /api/v1/incomes/{id}      → get one
 *   PUT    /api/v1/incomes/{id}      → update
 *   DELETE /api/v1/incomes/{id}      → delete
 *
 * Exactly mirrors ExpenseController — consistent, predictable API shape.
 * Default sort is incomeDate DESC (most recent income first).
 */
@RestController
@RequestMapping("/incomes")
@RequiredArgsConstructor
@Tag(name = "Incomes", description = "Manage income records")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping
    @Operation(summary = "List incomes with optional filters and pagination")
    public ResponseEntity<PageResponse<IncomeResponse>> getIncomes(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q,

            @PageableDefault(size = 20, sort = "incomeDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                incomeService.getIncomes(userId, from, to, categoryId, minAmount, maxAmount, q, pageable)
        );
    }

    @PostMapping
    @Operation(summary = "Create a new income record")
    public ResponseEntity<IncomeResponse> createIncome(
            @Valid @RequestBody IncomeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incomeService.createIncome(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single income record by ID")
    public ResponseEntity<IncomeResponse> getIncomeById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(incomeService.getIncomeById(id, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an income record")
    public ResponseEntity<IncomeResponse> updateIncome(
            @PathVariable Long id,
            @Valid @RequestBody IncomeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(incomeService.updateIncome(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an income record")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        incomeService.deleteIncome(id, userId);
        return ResponseEntity.noContent().build();
    }
}
