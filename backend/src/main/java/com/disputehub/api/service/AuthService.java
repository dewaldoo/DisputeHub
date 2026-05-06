package com.disputehub.api.service;

import com.disputehub.api.dto.JwtResponse;
import com.disputehub.api.dto.LoginRequest;
import com.disputehub.api.dto.RegisterRequest;
import com.disputehub.api.entity.Role;
import com.disputehub.api.entity.User;
import com.disputehub.api.exception.BadRequestException;
import com.disputehub.api.repository.UserRepository;
import com.disputehub.api.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public JwtResponse register(RegisterRequest request) {
        // Check if email (username) already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("This email is already registered");
        }

        // Create new user (username field stores email address)
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(Role.CUSTOMER) // Default role
                .build();

        user = userRepository.save(user);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(authentication);

        log.info("User registered successfully - UserId: {}, Email: {}, Role: {}", user.getId(), user.getUsername(), user.getRole());
        return new JwtResponse(token, user.getId(), user.getUsername(), user.getRole().name());
    }

    public JwtResponse login(LoginRequest request) {
        // Authenticate using username (which is an email address)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String token = jwtTokenProvider.generateToken(authentication);

        log.info("User logged in successfully - UserId: {}, Email: {}, Role: {}", user.getId(), user.getUsername(), user.getRole());
        return new JwtResponse(token, user.getId(), user.getUsername(), user.getRole().name());
    }
}
