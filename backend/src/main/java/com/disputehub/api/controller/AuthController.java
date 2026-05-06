package com.disputehub.api.controller;

import com.disputehub.api.dto.JwtResponse;
import com.disputehub.api.dto.LoginRequest;
import com.disputehub.api.dto.MessageResponse;
import com.disputehub.api.dto.RegisterRequest;
import com.disputehub.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller - Handles login and registration.
 *
 * @RestController = @Controller + @ResponseBody
 * @RequestMapping defines base path for all endpoints in this controller
 * @CrossOrigin allows CORS (already configured globally, but can override per controller)
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Operation(
        summary = "Register new user",
        description = "Create a new user account (CUSTOMER or ADMIN role). Returns JWT token upon successful registration."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input or username already exists",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        )
    })
    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - Email: {}", request.getUsername());
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
        summary = "Login user",
        description = "Authenticate user and receive JWT token for accessing protected endpoints."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        )
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Email: {}", request.getUsername());
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
        summary = "Health check",
        description = "Check if the authentication service is running."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Service is healthy",
        content = @Content(schema = @Schema(implementation = MessageResponse.class))
    )
    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(new MessageResponse("API is running"));
    }
}
