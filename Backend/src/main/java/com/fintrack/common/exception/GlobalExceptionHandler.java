package com.fintrack.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fintrack.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Global exception handler — the single place that converts exceptions into HTTP responses.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 * Intercepts exceptions thrown from ANY @RestController in the application.
 * Returns the standard ApiErrorResponse JSON shape for every exception type.
 *
 * IMPORTANT LIMITATION:
 * This class only handles exceptions thrown AFTER the DispatcherServlet processes the request.
 * Exceptions from Spring Security filters (JWT validation failures, invalid token)
 * happen BEFORE the DispatcherServlet — they are handled by the
 * AuthenticationEntryPoint and AccessDeniedHandler in SecurityConfig.
 *
 * ORDER OF HANDLERS:
 * Spring picks the most specific matching handler. The catch-all Exception.class
 * must be last — it matches everything, so specific handlers must be declared first.
 *
 * requestId:
 * Every response includes the MDC requestId set by RequestLoggingFilter.
 * This lets clients and ops teams correlate error responses to log lines.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // 400 Bad Request — Validation failures from @Valid on request bodies
    // =========================================================================

    /**
     * Handles Bean Validation failures (@Valid on @RequestBody).
     * Collects ALL field errors and returns them as a list — client gets all
     * problems at once instead of fixing one and discovering the next.
     *
     * Example trigger: POST /expenses with no amount and no date.
     * Response: 400 with fieldErrors: [{amount: "..."}, {expenseDate: "..."}]
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ApiErrorResponse.FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiErrorResponse.FieldValidationError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        return buildError(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Validation failed for " + fieldErrors.size() + " field(s)",
                request, fieldErrors);
    }

    /**
     * Handles malformed JSON bodies and invalid enum values in request body.
     *
     * Two most common triggers:
     *   1. Client sends invalid JSON (missing brace, extra comma, etc.)
     *      → generic "Request body is missing or malformed" message.
     *
     *   2. Client sends an invalid enum value, e.g. { "type": "BLAH" }
     *      → helpful message: "Invalid value 'BLAH' for field 'type'. Accepted values: [INCOME, EXPENSE]"
     *
     * Interview: "How does Jackson know which enum values are valid?"
     *   ife.getTargetType().getEnumConstants() returns the enum's declared constants.
     *   This is Java reflection — safe to call since we know the type is an enum.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String message = "Request body is missing or malformed";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {

            String fieldName = ife.getPath().isEmpty()
                    ? "field"
                    : ife.getPath().get(ife.getPath().size() - 1).getFieldName();

            message = String.format("Invalid value '%s' for field '%s'. Accepted values: %s",
                    ife.getValue(), fieldName,
                    Arrays.toString(ife.getTargetType().getEnumConstants()));
        }

        log.warn("Unreadable HTTP message at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    /**
     * Handles type mismatches in path variables or request parameters.
     *
     * Example trigger: GET /expenses/abc   (id must be Long, "abc" can't be parsed)
     * Response: 400 "Invalid value 'abc' for parameter 'id'"
     *
     * Without this handler, Spring would return a 500 with a MethodArgumentTypeMismatchException
     * stack trace — confusing for clients and leaks internal info.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        log.warn("Type mismatch at {}: {}", request.getRequestURI(), message);
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    /**
     * Handles @Validated constraint violations on method parameters.
     *
     * Example trigger: @Validated on a controller + @Positive on a path variable.
     *   GET /categories/-1  → ConstraintViolationException: must be positive
     *
     * Note: This is different from MethodArgumentNotValidException.
     *   MethodArgumentNotValidException: @Valid on @RequestBody (object-level validation)
     *   ConstraintViolationException: @Validated on controller + constraint on param/return
     *
     * The property path from Hibernate Validator looks like: "methodName.paramName"
     * We extract just the param name (after the last dot) for a cleaner field name.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<ApiErrorResponse.FieldValidationError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(v -> ApiErrorResponse.FieldValidationError.builder()
                        .field(extractLeafName(v.getPropertyPath().toString()))
                        .message(v.getMessage())
                        .build())
                .toList();

        return buildError(HttpStatus.BAD_REQUEST, "Validation Failed",
                "Request parameter validation failed", request, fieldErrors);
    }

    /**
     * Handles business rule violations thrown explicitly by service code.
     *
     * Example triggers:
     *   - IncomeService: "Category 'Food' is an EXPENSE category. Income must use an INCOME category."
     *   - ExpenseService: same inverse check
     *
     * These are programmer-meaningful errors that the client can act on:
     * they indicate a semantic mismatch between the request and the data model.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // =========================================================================
    // 401 Unauthorized — Bad login credentials
    // =========================================================================

    /**
     * Handles failed login attempts (wrong email or password).
     * BadCredentialsException is thrown by UserService.login() when login fails.
     *
     * Generic message — never reveal which one is wrong.
     * This prevents user enumeration: attacker can't tell if email exists.
     *
     * Note: 401 from an EXPIRED or MISSING JWT token is handled by the
     * AuthenticationEntryPoint in SecurityConfig, not here.
     */
    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            RuntimeException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Invalid email or password", request);
    }

    // =========================================================================
    // 404 Not Found — Resource doesn't exist or doesn't belong to the user
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // =========================================================================
    // 405 Method Not Allowed
    // =========================================================================

    /**
     * Handles requests with a valid path but wrong HTTP method.
     *
     * Example trigger: POST /analytics/summary (only GET is allowed)
     * Response: 405 with Spring's default message listing allowed methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not allowed: {} {}", request.getMethod(), request.getRequestURI());
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed",
                ex.getMessage(), request);
    }

    // =========================================================================
    // 409 Conflict — Duplicate resources or category-in-use
    // =========================================================================

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryInUse(
            CategoryInUseException ex,
            HttpServletRequest request) {

        return buildError(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // =========================================================================
    // 500 Internal Server Error — Unexpected exceptions (catch-all, MUST be last)
    // =========================================================================

    /**
     * Catch-all for any exception not handled above.
     *
     * Logs the full stack trace server-side (for debugging) but returns
     * only a generic message to the client — no stack trace, no class names,
     * no internal details. This prevents information leakage.
     *
     * The production profile also sets:
     *   server.error.include-message: never
     *   server.error.include-stacktrace: never
     * As a second line of defense.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {} [requestId={}]: {}",
                request.getRequestURI(), MDC.get("requestId"), ex.getMessage(), ex);

        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // =========================================================================
    // Private helpers — reduce repetition across all handlers
    // =========================================================================

    /**
     * Builds a standard error response with no fieldErrors.
     * All handlers that don't return field-level details use this.
     */
    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status, String error, String message, HttpServletRequest request) {

        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .requestId(MDC.get("requestId"))   // null if filter didn't run → omitted by @JsonInclude
                        .build()
        );
    }

    /**
     * Builds a standard error response with a list of fieldErrors.
     * Used by validation handlers that collect per-field problems.
     */
    private ResponseEntity<ApiErrorResponse> buildError(
            HttpStatus status, String error, String message,
            HttpServletRequest request, List<ApiErrorResponse.FieldValidationError> fieldErrors) {

        return ResponseEntity.status(status).body(
                ApiErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .requestId(MDC.get("requestId"))
                        .fieldErrors(fieldErrors)
                        .build()
        );
    }

    /**
     * Extracts the leaf segment from a Hibernate Validator property path.
     * Input:  "createExpense.request.amount"  or  "getTrends.months"
     * Output: "amount"                        or  "months"
     */
    private String extractLeafName(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
}
