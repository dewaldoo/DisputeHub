package com.disputehub.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter - Intercepts every HTTP request to check for JWT token.
 *
 * EXPLANATION - Servlet Filters:
 * Filters sit between client and controller, processing requests/responses.
 *
 * REQUEST FLOW:
 * ```
 * Client Request
 *     ↓
 * [JwtAuthenticationFilter] ← We are here
 *     ↓
 * [Spring Security Filters]
 *     ↓
 * [Controller]
 *     ↓
 * [Service]
 *     ↓
 * [Repository]
 *     ↓
 * Database
 * ```
 *
 * OncePerRequestFilter:
 * Ensures filter is executed only once per request (even if request forwarded/included)
 *
 * WHAT THIS FILTER DOES:
 * 1. Extract JWT from Authorization header
 * 2. Validate JWT signature and expiration
 * 3. Extract username from JWT
 * 4. Load user details from database
 * 5. Set authentication in Spring Security context
 * 6. Allow request to proceed to controller
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * Main filter method - called for every HTTP request.
     *
     * PARAMETERS:
     * - request: Incoming HTTP request
     * - response: Outgoing HTTP response
     * - filterChain: Chain of filters (pass request to next filter)
     *
     * @throws ServletException, IOException: Required by servlet spec
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Step 1: Extract JWT token from request
            String jwt = getJwtFromRequest(request);

            // Step 2: Validate token and extract username
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);

                // Step 3: Load full user details from database
                // WHY LOAD FROM DB?
                // - JWT contains username but not full user object
                // - Need authorities/roles for authorization checks
                // - Verify user still exists and is enabled
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Step 4: Create authentication token
                // UsernamePasswordAuthenticationToken is Spring Security's way of representing authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,                    // Principal (the user)
                                null,                           // Credentials (not needed, already authenticated)
                                userDetails.getAuthorities()    // Authorities (roles/permissions)
                        );

                // Attach request details (IP address, session ID, etc.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Step 5: Set authentication in security context
                // THIS IS THE KEY STEP!
                // Makes user available throughout request via SecurityContextHolder
                // Enables @PreAuthorize, @Secured annotations
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log error but don't block request
            // Request will proceed unauthenticated
            // Protected endpoints will return 401 Unauthorized
            log.error("Could not set user authentication in security context", ex);
        }

        // Step 6: Continue filter chain
        // Pass request to next filter or controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     *
     * EXPECTED HEADER FORMAT:
     * ```
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * ```
     *
     * STEPS:
     * 1. Get Authorization header
     * 2. Check if it starts with "Bearer "
     * 3. Extract token part (after "Bearer ")
     *
     * RETURNS:
     * - Token string if present
     * - null if header missing or invalid format
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix, return token
            return bearerToken.substring(7);
        }

        return null;
    }

}
