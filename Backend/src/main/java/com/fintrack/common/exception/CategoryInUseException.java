package com.fintrack.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts to delete a category that still has
 * transactions (expenses or incomes) referencing it.
 *
 * The DB FK constraint would also prevent this (RESTRICT behavior),
 * but we catch it at the service layer first to return a clear,
 * user-friendly message instead of a cryptic DB error.
 *
 * Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(String categoryName) {
        super("Category '" + categoryName + "' cannot be deleted because it has transactions. " +
              "Please reassign or delete those transactions first.");
    }
}
