package com.disputehub.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request DTO.
 * DTOs (Data Transfer Objects) separate API layer from domain layer.
 *
 * Uses email-based authentication for enterprise-grade security.
 * The 'username' field accepts email addresses.
 */
@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
