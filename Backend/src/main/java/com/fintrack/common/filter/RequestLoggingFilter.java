package com.fintrack.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that runs on every HTTP request to:
 *   1. Assign a unique requestId (for log correlation)
 *   2. Log the incoming request and outgoing response with timing
 *
 * WHY MDC (Mapped Diagnostic Context)?
 *   MDC is a per-thread key-value store that Logback reads when formatting log lines.
 *   By putting requestId into MDC, EVERY log line produced during that request
 *   (in the service, repository, etc.) automatically includes the requestId.
 *   In production, you can grep by requestId to see all logs for one request.
 *   Without MDC, you'd have to correlate logs manually — nearly impossible at scale.
 *
 * WHY @Order(HIGHEST_PRECEDENCE)?
 *   This filter must run BEFORE Spring Security's FilterChainProxy.
 *   If it ran after, security logs (e.g. "JWT validation failed") wouldn't have a requestId.
 *   HIGHEST_PRECEDENCE = Integer.MIN_VALUE = runs first in the filter chain.
 *
 * WHY extend OncePerRequestFilter?
 *   Servlet filters can theoretically be called multiple times per request
 *   (e.g. on forward/include dispatches). OncePerRequestFilter guarantees
 *   doFilterInternal is called exactly once per HTTP request.
 *
 * requestId strategy:
 *   - If the client sends X-Request-ID header, use it (client-side correlation).
 *   - Otherwise generate an 8-char hex ID (compact, readable in logs).
 *   - Echo it back in the response so clients can track their own requests.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = resolveRequestId(request);

        // Put into MDC — all log lines on this thread now include [requestId]
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        // Echo back in response so clients can correlate (useful for bug reports)
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startMillis = System.currentTimeMillis();
        try {
            log.info("→ {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            // 'finally' guarantees this runs even if an exception escapes the chain
            long durationMs = System.currentTimeMillis() - startMillis;
            log.info("← {} {} {} in {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);

            // ALWAYS remove from MDC after request completes.
            // Thread pools reuse threads — stale MDC values from one request
            // would bleed into the next request on the same thread.
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    /**
     * Use the client's X-Request-ID if provided; otherwise generate an 8-char hex ID.
     * 8 chars gives 16^8 = 4 billion combinations — sufficient for request correlation
     * without cluttering the logs with a full UUID.
     */
    private String resolveRequestId(HttpServletRequest request) {
        String clientRequestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(clientRequestId)) {
            return clientRequestId;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
