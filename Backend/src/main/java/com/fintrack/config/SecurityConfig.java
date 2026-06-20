package com.fintrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.common.dto.ApiErrorResponse;
import com.fintrack.security.CustomUserDetailsService;
import com.fintrack.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration — the most critical security class in the project.
 *
 * @EnableWebSecurity      — Activates Spring Security's web support.
 * @EnableMethodSecurity   — Enables @PreAuthorize on methods (optional in MVP, good practice).
 * @Configuration          — This class defines @Bean methods.
 *
 * KEY DECISIONS:
 *
 * 1. STATELESS session: we never create an HTTP session (JWT is the only auth mechanism).
 *    SessionCreationPolicy.STATELESS → Spring Security won't create or use HttpSession.
 *
 * 2. CSRF disabled: CSRF attacks exploit cookie-based auth.
 *    We use Bearer tokens in headers — browsers don't auto-send them.
 *    Therefore CSRF protection is not needed and would break our API.
 *
 * 3. DaoAuthenticationProvider: connects Spring Security to our UserDetailsService
 *    and PasswordEncoder. It's used internally by Spring when you call authenticate().
 *
 * 4. JwtAuthFilter runs BEFORE UsernamePasswordAuthenticationFilter: we intercept
 *    every request, validate JWT if present, and populate the SecurityContext.
 *    Spring Security then enforces authorization based on what we set.
 *
 * 5. AuthenticationEntryPoint and AccessDeniedHandler: custom error responses
 *    for security filter failures (before DispatcherServlet, so GlobalExceptionHandler
 *    can't handle them — we do it here manually).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * Endpoints that are publicly accessible without a JWT token.
     * Everything else requires authentication (anyRequest().authenticated()).
     */
    private static final String[] PUBLIC_URLS = {
            "/auth/**",           // register, login, refresh
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/api-docs",
            "/api-docs/**",
            "/api-docs.yaml",
            "/actuator/health"    // Docker health check — must be reachable without a token
    };

    // =========================================================================
    // Security Filter Chain — the heart of Spring Security configuration
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT-based REST APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Configure CORS using our CorsConfigurationSource bean
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // HTTP security headers — defense in depth even if Nginx is misconfigured.
            // Spring Security adds X-Content-Type-Options, X-Frame-Options, and
            // Cache-Control by default. We explicitly add Referrer-Policy on top.
            //
            // INTERVIEW: "What does Referrer-Policy do?"
            //   Controls what URL is sent in the HTTP Referer header when the user
            //   navigates away or makes a cross-origin request. STRICT_ORIGIN_WHEN_CROSS_ORIGIN
            //   sends the full URL for same-origin requests but only the origin (no path/query)
            //   for cross-origin requests. This prevents leaking JWT tokens or user IDs
            //   that might appear in query strings to third-party servers.
            .headers(headers -> headers
                    .referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )

            // Stateless session — Spring Security must NOT create or store any session
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()   // public endpoints
                    .anyRequest().authenticated()               // everything else needs JWT
            )

            // Wire our DaoAuthenticationProvider (BCrypt + UserDetailsService)
            .authenticationProvider(authenticationProvider())

            // Register our JWT filter — runs BEFORE Spring's default auth filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Custom handlers for security-level errors (these happen before DispatcherServlet)
            .exceptionHandling(exception -> exception

                // 401 Unauthorized — no token, invalid token, expired token
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiErrorResponse error = ApiErrorResponse.builder()
                            .timestamp(Instant.now())
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .error("Unauthorized")
                            .message("Authentication required. Please provide a valid Bearer token.")
                            .path(request.getRequestURI())
                            .build();
                    response.getWriter().write(objectMapper.writeValueAsString(error));
                })

                // 403 Forbidden — valid token but insufficient role
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiErrorResponse error = ApiErrorResponse.builder()
                            .timestamp(Instant.now())
                            .status(HttpStatus.FORBIDDEN.value())
                            .error("Forbidden")
                            .message("You do not have permission to access this resource.")
                            .path(request.getRequestURI())
                            .build();
                    response.getWriter().write(objectMapper.writeValueAsString(error));
                })
            );

        return http.build();
    }

    // =========================================================================
    // Authentication Provider
    // =========================================================================

    /**
     * DaoAuthenticationProvider wires together:
     *   - UserDetailsService (CustomUserDetailsService): loads the user from DB by email
     *   - PasswordEncoder (BCryptPasswordEncoder): verifies the provided password against the hash
     *
     * When Spring Security authenticates a user, it:
     *   1. Calls userDetailsService.loadUserByUsername(email) → gets User from DB
     *   2. Calls passwordEncoder.matches(rawPassword, user.getPassword()) → verifies hash
     *   3. If both checks pass → authentication succeeds
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // =========================================================================
    // Password Encoder
    // =========================================================================

    /**
     * BCryptPasswordEncoder — the industry standard for password hashing.
     *
     * WHY BCrypt?
     *   - Adaptive: cost factor (default 10) can be increased as hardware gets faster
     *   - Salted: generates a unique random salt per password — two identical passwords
     *     produce different hashes. No rainbow table attacks.
     *   - Slow by design: makes brute-force attacks computationally expensive
     *
     * Usage: passwordEncoder.encode("rawPassword") → "$2a$10$abcdef..."
     *        passwordEncoder.matches("rawPassword", "$2a$10$abcdef...") → true/false
     *
     * The cost factor 10 is the Spring default. In production, 12 is recommended.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // =========================================================================
    // CORS Configuration
    // =========================================================================

    /**
     * CORS (Cross-Origin Resource Sharing) — browser security mechanism.
     *
     * WHY we need CORS:
     * Our frontend (http://localhost:5173) makes API calls to the backend
     * (http://localhost:8080). They are on different origins (different port).
     * Browsers block cross-origin requests by default (Same-Origin Policy).
     * We tell the browser: "requests from these origins are allowed."
     *
     * In production, allowedOrigins should be your exact frontend domain.
     * Wildcard "*" is not allowed when allowCredentials = true.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(List.of("Authorization", "X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight response for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply to all paths
        return source;
    }
}
