package com.disputehub.api.security;

import com.disputehub.api.entity.User;
import com.disputehub.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService - Loads user data for Spring Security.
 *
 * EXPLANATION:
 * UserDetailsService is a core Spring Security interface for loading user data.
 * Spring Security calls this during authentication to fetch user details.
 *
 * WHY CUSTOM IMPLEMENTATION?
 * - Spring Security doesn't know about our User entity
 * - Needs a bridge between our database and Spring Security
 * - We provide that bridge by implementing UserDetailsService
 *
 * WHEN IS THIS CALLED?
 * 1. During login: AuthenticationManager calls this to validate credentials
 * 2. During JWT validation: JwtAuthenticationFilter calls this to load user
 *
 * @Service - Spring manages this as a singleton bean
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Load user by username.
     *
     * CALLED BY:
     * - Spring Security during login
     * - JwtAuthenticationFilter during request authentication
     *
     * FLOW DURING LOGIN:
     * ```
     * 1. User submits login form: { username: "john", password: "pass123" }
     *
     * 2. AuthenticationManager calls loadUserByUsername("john")
     *
     * 3. We fetch user from database
     *
     * 4. Return UserDetails object with:
     *    - username
     *    - hashed password
     *    - authorities (roles)
     *
     * 5. AuthenticationManager compares:
     *    - Submitted password (plain text)
     *    - Stored password (BCrypt hash)
     *    using PasswordEncoder
     *
     * 6. If match:
     *    - Create Authentication object
     *    - Generate JWT token
     *    - Return token to client
     *
     * 7. If no match:
     *    - Throw BadCredentialsException
     *    - Return 401 Unauthorized
     * ```
     *
     * WHY RETURN UserDetails?
     * Spring Security needs a standard interface.
     * Our User entity implements UserDetails, so we can return it directly.
     *
     * @param username The username to load
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username: " + username)
                );

        return user; // User implements UserDetails, so we can return it directly
    }

}
