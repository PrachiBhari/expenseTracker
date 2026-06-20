package com.fintrack.security;

import com.fintrack.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security's UserDetailsService implementation.
 *
 * Spring Security calls loadUserByUsername() in TWO places:
 *
 * 1. During JWT filter validation (JwtAuthFilter):
 *    After parsing the JWT and getting the email, we load the full user object
 *    to verify token validity and get current authorities.
 *
 * 2. During DaoAuthenticationProvider authentication (not used directly in our case,
 *    but wired in SecurityConfig for completeness).
 *
 * WHY load from DB on every request?
 * The JWT contains the email, but roles/enabled status can change between requests.
 * Loading from DB ensures we always use the current state — if an admin disables
 * an account, the next request is rejected even if the JWT is still valid.
 *
 * Trade-off: this is one extra DB query per authenticated request. For high-traffic
 * systems, you'd add caching (Redis) or embed all claims in the JWT (accepting
 * stale data until the token expires). For this MVP, DB lookup per request is fine.
 *
 * @Transactional(readOnly = true) — this is a SELECT query; no writes.
 * readOnly=true tells Hibernate to optimize the session (no dirty checking).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * The "username" in Spring Security is our user's email address.
     * Our User entity implements UserDetails, so we return it directly.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email));
    }
}
