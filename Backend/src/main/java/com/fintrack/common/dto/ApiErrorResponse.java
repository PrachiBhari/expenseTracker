package com.fintrack.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned for ALL error responses.
 *
 * Every error — validation, not found, auth, server error — returns this same shape.
 * This makes the frontend's error handling uniform:
 *   catch(error) → read error.response.data.message → show to user.
 *
 * @JsonInclude(NON_NULL) — fields that are null are OMITTED from JSON output.
 *   fieldErrors: only present on 400 Validation errors.
 *   requestId:   only present when the RequestLoggingFilter has run (all normal requests).
 *
 * Example 400 response:
 * {
 *   "timestamp": "2026-06-21T10:00:00Z",
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "Validation failed for 2 field(s)",
 *   "path": "/api/v1/expenses",
 *   "requestId": "a1b2c3d4",
 *   "fieldErrors": [
 *     { "field": "amount",      "message": "must be greater than 0" },
 *     { "field": "expenseDate", "message": "must not be null" }
 *   ]
 * }
 *
 * Example 404 response (fieldErrors omitted, only requestId + core fields):
 * {
 *   "timestamp": "2026-06-21T10:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Expense not found with id: 99",
 *   "path": "/api/v1/expenses/99",
 *   "requestId": "d4e5f6a7"
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /**
     * Unique ID for this request — sourced from MDC (set by RequestLoggingFilter).
     * Clients can include this in bug reports; ops teams can grep logs by this ID
     * to see every log line from the failed request.
     */
    private final String requestId;

    /**
     * Populated only for 400 Validation errors (from @Valid on request bodies).
     * Null (and omitted from JSON) for all other error types.
     */
    private final List<FieldValidationError> fieldErrors;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldValidationError {
        private final String field;
        private final String message;
    }
}
