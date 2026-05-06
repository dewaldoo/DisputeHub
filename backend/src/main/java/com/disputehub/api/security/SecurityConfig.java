package com.disputehub.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security Configuration - Configures Spring Security for JWT authentication.
 *
 * EXPLANATION:
 * This is the heart of security configuration.
 * Defines:
 * - Which endpoints are public vs protected
 * - How authentication works (JWT)
 * - Password encoding (BCrypt)
 * - CORS policy
 * - Authorization rules
 *
 * @Configuration - This class provides Spring beans
 * @EnableWebSecurity - Enables Spring Security
 * @EnableMethodSecurity - Enables @PreAuthorize, @Secured annotations on methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Password Encoder Bean - BCrypt hashing algorithm.
     *
     * EXPLANATION - Why BCrypt?
     *
     * NEVER STORE PLAIN TEXT PASSWORDS:
     * ```
     * ❌ password = "myPassword123"
     * ```
     * If database is breached, all passwords are exposed.
     *
     * DON'T USE SIMPLE HASHING (MD5, SHA1):
     * ```
     * ❌ password = md5("myPassword123")
     *    = "482c811da5d5b4bc6d497ffa98491e38"
     * ```
     * Problems:
     * - Same password always produces same hash (rainbow table attacks)
     * - Fast to compute (billions of hashes per second on GPUs)
     *
     * USE BCRYPT:
     * ```
     * ✓ password = bcrypt("myPassword123")
     *    = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     * ```
     * Benefits:
     * - SALT: Random string added to password before hashing
     *   Same password → different hash each time
     * - SLOW: Intentionally slow (configurable "cost" factor)
     *   Makes brute force attacks impractical
     * - FUTURE-PROOF: Can increase cost over time as computers get faster
     *
     * BCRYPT HASH STRUCTURE:
     * $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
     * │  │  │                                                           │
     * │  │  │                                                           └─ Hash (31 chars)
     * │  │  └─ Salt (22 chars)
     * │  └─ Cost factor (10 = 2^10 = 1024 rounds)
     * └─ Algorithm version (2a)
     *
     * COST FACTOR:
     * - 10 (default): ~0.1 seconds per hash
     * - 12: ~0.4 seconds per hash
     * - 14: ~1.6 seconds per hash
     * Trade-off: Security vs Performance
     *
     * USAGE IN CODE:
     * ```java
     * // Encoding (during registration)
     * String plain = "myPassword123";
     * String hashed = passwordEncoder.encode(plain);
     * user.setPassword(hashed);
     * userRepository.save(user);
     *
     * // Verification (during login)
     * boolean matches = passwordEncoder.matches("myPassword123", hashed);
     * // matches = true
     * ```
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider - Connects UserDetailsService and PasswordEncoder.
     *
     * EXPLANATION:
     * DaoAuthenticationProvider is Spring Security's default authentication mechanism.
     * It:
     * 1. Uses UserDetailsService to load user
     * 2. Uses PasswordEncoder to verify password
     *
     * AUTHENTICATION FLOW:
     * ```
     * Login Request: { username: "john", password: "plain123" }
     *      ↓
     * AuthenticationManager
     *      ↓
     * DaoAuthenticationProvider
     *      ├─ Calls: userDetailsService.loadUserByUsername("john")
     *      │  Returns: UserDetails (with hashed password)
     *      ├─ Calls: passwordEncoder.matches("plain123", hashedPassword)
     *      │  Returns: true/false
     *      └─ If true: Return Authentication object
     *         If false: Throw BadCredentialsException
     * ```
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager - Main entry point for authentication.
     *
     * USAGE IN CONTROLLER:
     * ```java
     * @PostMapping("/login")
     * public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
     *     Authentication authentication = authenticationManager.authenticate(
     *         new UsernamePasswordAuthenticationToken(
     *             request.getUsername(),
     *             request.getPassword()
     *         )
     *     );
     *     String token = jwtTokenProvider.generateToken(authentication);
     *     return ResponseEntity.ok(new JwtResponse(token));
     * }
     * ```
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security Filter Chain - Main security configuration.
     *
     * THIS IS THE KEY CONFIGURATION!
     * Defines:
     * - Which URLs require authentication
     * - Which roles can access which endpoints
     * - Session management (stateless for JWT)
     * - CORS configuration
     * - CSRF protection (disabled for REST APIs)
     * - Filter order
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS - Allow frontend to call backend
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF - Disable for REST APIs
                // EXPLANATION:
                // CSRF (Cross-Site Request Forgery) protection is for traditional web apps
                // where authentication is cookie-based.
                // For JWT (token in Authorization header), CSRF is not needed.
                .csrf(csrf -> csrf.disable())

                // Session Management - Stateless (no sessions)
                // EXPLANATION:
                // Traditional apps store session on server.
                // With JWT, we don't use sessions - token contains all info.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC ENDPOINTS (no authentication required)
                        // - /api/auth/** - Login and register endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // - Actuator health endpoints (monitoring)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // - Actuator admin endpoints (require ADMIN role)
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // - Swagger/OpenAPI endpoints (public for API documentation)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()

                        // ALL OTHER ENDPOINTS require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT filter before Spring Security's authentication filter
                // EXPLANATION:
                // Our JwtAuthenticationFilter runs first to:
                // 1. Extract JWT from request
                // 2. Validate JWT
                // 3. Set authentication in SecurityContext
                // Then Spring Security checks if user is authorized
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration - Allow frontend to call backend.
     *
     * EXPLANATION - What is CORS?
     * CORS (Cross-Origin Resource Sharing) is a browser security feature.
     *
     * SCENARIO:
     * - Frontend: http://localhost:3000 (React)
     * - Backend:  http://localhost:8080 (Spring Boot)
     * - Different origins (different ports)
     *
     * WITHOUT CORS CONFIGURATION:
     * ```
     * Frontend (localhost:3000) sends request to Backend (localhost:8080)
     *     ↓
     * Browser blocks request with error:
     * "Access to fetch at 'http://localhost:8080/api/transactions' from origin
     *  'http://localhost:3000' has been blocked by CORS policy"
     * ```
     *
     * WITH CORS CONFIGURATION:
     * Backend tells browser: "I allow requests from localhost:3000"
     * Browser allows the request.
     *
     * IMPORTANT:
     * - CORS is a BROWSER security feature
     * - Postman/curl don't care about CORS
     * - Only affects web browsers
     *
     * PRODUCTION CONFIGURATION:
     * Configure specific allowed origins instead of wildcard.
     * Never use "*" (allow all origins) in production!
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow frontend URLs
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // Allow all HTTP methods (GET, POST, PUT, DELETE, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all headers (Authorization, Content-Type, etc.)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
