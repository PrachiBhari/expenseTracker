package com.fintrack.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when creating a resource that already exists.
 *
 * Used for:
 *   - Registration with an email already in use
 *   - Creating a category with a name + type that already exists for this user
 *
 * Maps to HTTP 409 Conflict — the request conflicts with current state of the server.
 *
 * Note for registration: we still return 409 (not a generic 400) so the client
 * can show a specific "email already in use" message. This is acceptable because
 * we only reveal that a conflict exists, not other account details.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
