package com.disputehub.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Provider - Creates and validates JWT tokens.
 *
 * EXPLANATION - What is JWT?
 * JWT (JSON Web Token) is a compact, self-contained way to securely transmit information.
 *
 * STRUCTURE:
 * JWT has 3 parts separated by dots: HEADER.PAYLOAD.SIGNATURE
 *
 * 1. HEADER (Base64 encoded):
 *    ```json
 *    {
 *      "alg": "HS256",
 *      "typ": "JWT"
 *    }
 *    ```
 *
 * 2. PAYLOAD (Base64 encoded):
 *    ```json
 *    {
 *      "sub": "john_doe",          // subject (username)
 *      "iat": 1713273600,          // issued at
 *      "exp": 1713277200,          // expiration
 *      "role": "CUSTOMER"          // custom claim
 *    }
 *    ```
 *
 * 3. SIGNATURE:
 *    ```
 *    HMACSHA256(
 *      base64UrlEncode(header) + "." + base64UrlEncode(payload),
 *      secretKey
 *    )
 *    ```
 *
 * EXAMPLE TOKEN:
 * eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTcxMzI3MzYwMCwiZXhwIjoxNzEzMjc3MjAwfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
 *
 * WHY JWT?
 * - STATELESS: Server doesn't store sessions (scales horizontally)
 * - SELF-CONTAINED: Contains all user info (no database lookup needed)
 * - SECURE: Signature prevents tampering
 * - CROSS-DOMAIN: Works across different services/domains
 *
 * SECURITY:
 * - Anyone can decode the token (it's Base64, not encrypted)
 * - But only server with secret key can create/verify signature
 * - Never put sensitive data in token (passwords, credit cards)
 *
 * @Component - Spring manages this as a singleton bean
 */
@Component
public class JwtTokenProvider {

    /**
     * Secret key for signing JWTs
     * @Value injects value from application.properties
     *
     * IMPORTANT:
     * - Must be at least 256 bits (32 characters) for HS256
     * - NEVER commit secret keys to git
     * - Use environment variables in production
     * - Rotate keys periodically
     */
    @Value("${jwt.secret-key}")
    private String jwtSecret;

    /**
     * Token validity period in milliseconds
     * Default: 3600000 ms = 1 hour
     *
     * TRADE-OFFS:
     * Short expiry (15 min):
     * - More secure (stolen token expires quickly)
     * - Poor UX (user logged out frequently)
     *
     * Long expiry (7 days):
     * - Better UX
     * - Less secure (stolen token valid longer)
     *
     * BEST PRACTICE:
     * - Short access token (15 min - 1 hour)
     * - Long refresh token (7-30 days)
     * - Implement token refresh endpoint
     */
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Validate JWT secret on application startup.
     * Fails fast if secret is missing or too weak.
     */
    @PostConstruct
    public void validateSecret() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT secret key is not configured. Set JWT_SECRET environment variable.");
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 characters (256 bits) for HS256 algorithm.");
        }

        log.info("JWT secret key validated successfully");
    }

    /**
     * Generate JWT token from Spring Security Authentication.
     *
     * HOW IT WORKS:
     * 1. Extract username from authenticated user
     * 2. Create JWT with username as subject
     * 3. Set issue time and expiration
     * 4. Sign with secret key
     *
     * CALLED BY:
     * AuthController after successful login:
     * ```java
     * Authentication auth = authenticationManager.authenticate(credentials);
     * String token = jwtTokenProvider.generateToken(auth);
     * return ResponseEntity.ok(new JwtResponse(token));
     * ```
     */
    public String generateToken(Authentication authentication) {
        // Get user details from authentication object
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Current time
        Date now = new Date();

        // Expiration time (current time + validity period)
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        // Build the secret key from configured secret string
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        // Create and return JWT
        return Jwts.builder()
                .subject(userDetails.getUsername())           // Who the token is about (username)
                .issuedAt(now)                                // When token was created
                .expiration(expiryDate)                       // When token expires
                .signWith(key)                                // Sign with secret key (HS256 algorithm)
                .compact();                                   // Build and serialize to string
    }

    /**
     * Extract username from JWT token.
     *
     * USAGE:
     * ```java
     * String token = "eyJhbGciOiJIUzI1NiIs...";
     * String username = jwtTokenProvider.getUsernameFromToken(token);
     * // username = "john_doe"
     * ```
     *
     * HOW IT WORKS:
     * 1. Parse JWT using secret key
     * 2. Verify signature (throws exception if tampered)
     * 3. Extract subject claim (username)
     */
    public String getUsernameFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)                              // Verify signature
                .build()
                .parseSignedClaims(token)                     // Parse token
                .getPayload();                                // Get payload (claims)

        return claims.getSubject();                            // Return username
    }

    /**
     * Validate JWT token.
     *
     * CHECKS:
     * 1. Signature is valid (not tampered)
     * 2. Token not expired
     * 3. Token format is correct
     *
     * EXCEPTIONS:
     * - MalformedJwtException: Invalid JWT format
     * - ExpiredJwtException: Token expired
     * - UnsupportedJwtException: Wrong algorithm
     * - IllegalArgumentException: Empty/null token
     * - SignatureException: Signature verification failed (tampered token)
     *
     * USAGE:
     * ```java
     * if (jwtTokenProvider.validateToken(token)) {
     *     // Token is valid, allow access
     * } else {
     *     // Token invalid, deny access
     * }
     * ```
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

}
