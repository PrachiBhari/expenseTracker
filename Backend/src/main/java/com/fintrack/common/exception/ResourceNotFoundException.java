package com.fintrack.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource does not exist OR belongs to another user.
 *
 * Why return 404 for "belongs to another user" instead of 403 Forbidden?
 * Returning 403 confirms the resource EXISTS but the caller lacks access.
 * That leaks information — an attacker now knows ID 42 belongs to someone.
 * Returning 404 for both "not found" and "not yours" reveals nothing.
 * This is called "Security through obscurity of resource existence."
 *
 * Example usage:
 *   expense with id=42 doesn't exist   → throw ResourceNotFoundException
 *   expense with id=42 belongs to Bob  → throw ResourceNotFoundException (same!)
 *
 * Maps to HTTP 404 via GlobalExceptionHandler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }
}
