package com.disputehub.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC Logging Filter - Adds contextual information to all logs.
 *
 * EXPLANATION - What is MDC?
 * MDC (Mapped Diagnostic Context) is a thread-local key-value store provided by SLF4J.
 * Any key-value pairs you put in MDC are automatically included in EVERY log statement
 * within that thread.
 *
 * HOW IT WORKS:
 * ```
 * Request arrives
 *     ↓
 * [MdcLoggingFilter] ← Runs FIRST
 *     ├─ Generates traceId: "abc-123-def-456"
 *     ├─ Stores in MDC.put("traceId", "abc-123-def-456")
 *     └─ Stores userId, path, method, etc.
 *     ↓
 * [JwtAuthenticationFilter] ← Authenticates user
 *     ↓
 * [Controller]
 *     └─ log.info("Dispute created")
 *         Output includes: traceId, userId, path (from MDC!)
 *     ↓
 * [Service]
 *     └─ log.info("Saving to database")
 *         Output ALSO includes: traceId, userId (same MDC context!)
 *     ↓
 * Finally block: MDC.clear() ← Clean up
 * ```
 *
 * WHY CORRELATION IDs?
 * Imagine customer calls support: "I got an error at 2:34 PM"
 * Support searches logs: "Show all logs with traceId=abc-123"
 * Result: See ENTIRE request flow (controller → service → repository → error)
 *
 * DISTRIBUTED TRACING:
 * If your system calls other microservices:
 * - Service A generates traceId: "abc-123"
 * - Service A calls Service B, passes traceId in header
 * - Service B uses same traceId in its logs
 * - You can trace request across ALL services!
 *
 * @Order(1): Ensures this filter runs FIRST (before authentication)
 * @Component: Registered as Spring bean
 */
@Component
@Order(1)
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcLoggingFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    /**
     * Add contextual information to MDC for all logs in this request.
     *
     * WHAT GETS ADDED TO MDC:
     * - traceId: Correlation ID for entire request (UUID)
     * - spanId: Identifier for this specific operation
     * - requestMethod: HTTP method (GET, POST, etc.)
     * - requestPath: Endpoint path (/api/disputes)
     * - clientIp: Client IP address
     * - userId: Authenticated user email (added after authentication)
     * - userRole: User role (CUSTOMER/ADMIN)
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Generate or extract trace ID
            // If client sends X-Trace-Id header, use it (for distributed tracing)
            // Otherwise generate new UUID
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }

            String spanId = request.getHeader(SPAN_ID_HEADER);
            if (spanId == null || spanId.isBlank()) {
                spanId = UUID.randomUUID().toString();
            }

            // Add request context to MDC
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestPath", request.getRequestURI());
            MDC.put("clientIp", request.getRemoteAddr());

            // Add trace ID to response header (useful for debugging)
            response.setHeader(TRACE_ID_HEADER, traceId);

            // Log request start
            log.debug("Request started - {} {}", request.getMethod(), request.getRequestURI());

            // Continue filter chain
            filterChain.doFilter(request, response);

            // After request completes, try to add user context
            // (Authentication might be set by JwtAuthenticationFilter)
            addUserContextToMdc();

            // Log request completion
            log.debug("Request completed - {} {} - Status: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus());

        } finally {
            // CRITICAL: Clear MDC to prevent memory leak
            // Thread pools reuse threads, so MDC must be cleaned up
            MDC.clear();
        }
    }

    /**
     * Add authenticated user information to MDC.
     *
     * TIMING:
     * This runs AFTER JwtAuthenticationFilter has authenticated the user.
     * If authentication succeeded, SecurityContextHolder contains user details.
     *
     * RESULT:
     * All subsequent logs in this request include userId and userRole.
     */
    private void addUserContextToMdc() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {

                String username = authentication.getName();
                MDC.put("userId", username);

                // Extract role
                String role = authentication.getAuthorities().stream()
                        .findFirst()
                        .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN");
                MDC.put("userRole", role);
            }
        } catch (Exception e) {
            // Don't fail request if MDC population fails
            log.warn("Failed to populate MDC with user context", e);
        }
    }

}
