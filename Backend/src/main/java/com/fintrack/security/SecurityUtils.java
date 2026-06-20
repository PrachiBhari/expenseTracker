package com.fintrack.security;

import com.fintrack.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract the current authenticated user from the SecurityContext.
 *
 * WHY is this needed?
 * After JwtAuthFilter sets the Authentication object in the SecurityContext,
 * every service method needs to know: "WHO is making this request?"
 * This class provides a single, reusable way to get that information.
 *
 * USAGE in service methods:
 *   Long userId = SecurityUtils.getCurrentUserId();
 *   // Then scope all DB queries to this userId:
 *   expenseRepository.findByIdAndUserId(expenseId, userId);
 *
 * WHY not pass userId as a parameter from the controller?
 * The controller should not handle security concerns — it just maps HTTP to method calls.
 * The service layer is where business rules and ownership checks live.
 * Calling SecurityContextHolder in the service is the Spring Security idiomatic approach.
 *
 * No @Component — static utility class, no Spring bean needed.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Utility class — no instantiation
    }

    /**
     * Returns the database ID of the currently authenticated user.
     * Throws IllegalStateException if called outside of an authenticated request.
     * (This should never happen on a properly secured endpoint.)
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user.getId();
    }

    /**
     * Returns the full User entity of the currently authenticated user.
     * The principal is set by JwtAuthFilter as the UserDetails (which is our User entity).
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException(
                    "No authenticated user found in SecurityContext. " +
                    "This endpoint must be protected.");
        }

        return (User) authentication.getPrincipal();
    }
}
