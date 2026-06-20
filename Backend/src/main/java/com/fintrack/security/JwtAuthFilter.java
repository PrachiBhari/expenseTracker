package com.fintrack.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs ONCE per HTTP request (OncePerRequestFilter).
 *
 * This filter sits in Spring Security's filter chain, BEFORE the
 * UsernamePasswordAuthenticationFilter. Its job:
 *
 * 1. Read the Authorization header from the request
 * 2. If it starts with "Bearer ", extract the token
 * 3. Parse the token to get the user's email
 * 4. Load the user from DB (via CustomUserDetailsService)
 * 5. Validate the token (signature + expiry + email match)
 * 6. If valid, set the Authentication in the SecurityContext
 *    → Spring Security now treats this request as authenticated
 *
 * The filter NEVER throws an exception to the client — if the token is missing
 * or invalid, we simply don't set authentication and let Spring Security's
 * downstream handlers decide what to do (return 401 for protected endpoints).
 *
 * WHY OncePerRequestFilter?
 * In some Servlet configurations, a filter can be invoked multiple times per
 * request (e.g., for error dispatch). OncePerRequestFilter guarantees exactly
 * one execution, preventing duplicate auth processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Step 1: No Authorization header OR doesn't start with "Bearer " → skip
        // The request will still continue. If the endpoint requires auth,
        // Spring Security will reject it after the filter chain completes.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract token — everything after "Bearer "
        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            // Step 3: Parse token to get email.
            // If the token is expired, tampered with, or malformed, JwtException is thrown.
            userEmail = jwtService.extractEmail(jwt);
        } catch (ExpiredJwtException e) {
            // Normal: user held a tab open, session timed out, or the access token expired.
            // Log at DEBUG — this is not suspicious, just a regular expiry.
            // Client should use /auth/refresh to get a new access token.
            log.debug("JWT token expired for subject '{}': {}", e.getClaims().getSubject(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (SignatureException e) {
            // SECURITY ALERT: token was modified after signing (signature doesn't match).
            // This is a sign of tampering. Log at WARN so security monitoring can detect it.
            // Never reveal WHY validation failed to the caller — they get a plain 401.
            log.warn("JWT signature verification failed — possible token tampering from IP {}: {}",
                    request.getRemoteAddr(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (MalformedJwtException e) {
            // Token structure is not valid JWT format (header.payload.signature).
            // Could be a client bug or a probe — log at DEBUG.
            log.debug("Malformed JWT token: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        } catch (JwtException e) {
            // Catch-all for any other JJWT exception (unsupported JWT, claims conflict, etc.)
            log.debug("JWT validation failed: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Step 4: If we got an email AND the SecurityContext has no auth yet
        // (prevents re-authenticating an already authenticated request)
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the user from DB to get up-to-date roles and enabled status
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            // Step 5: Final validation (expiry + email match)
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Step 6: Create an Authentication object and put it in the SecurityContext
                // This is what "authenticating" the request means in Spring Security.
                // null credentials: we verified via JWT, no password needed here.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Attach HTTP request details (IP address, session id) to the auth object
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Store in the SecurityContext — makes this request "authenticated"
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authentication successful for user: {}", userEmail);
            }
        }

        // Always pass the request to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
