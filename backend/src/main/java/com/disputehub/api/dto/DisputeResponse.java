package com.disputehub.api.dto;

import com.disputehub.api.entity.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Dispute entity.
 *
 * WHY USE DTOs INSTEAD OF ENTITIES?
 * 1. Prevents circular reference issues (User → Transaction → Dispute → User...)
 * 2. Decouples API contract from database schema
 * 3. Controls what data is exposed (don't leak sensitive fields)
 * 4. Allows different representations for different endpoints
 * 5. Makes API evolution easier (change entity without breaking API)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeResponse {
    private Long id;
    private String reason;
    private String description;
    private DisputeStatus status;
    private String evidenceUrl;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    // Transaction details (flatten to avoid circular reference)
    private Long transactionId;
    private String merchantName;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String transactionCategory;

    // User details (flatten to avoid circular reference)
    private Long userId;
    private String username;
    private String userFullName;
}
