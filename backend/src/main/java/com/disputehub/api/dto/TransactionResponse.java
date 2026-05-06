package com.disputehub.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Transaction entity.
 *
 * WHY USE DTOs?
 * - Prevents circular references (Transaction → User → Transactions...)
 * - Decouples API from database schema
 * - Controls data exposure (no sensitive user data leaked)
 * - Allows different views for different contexts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String merchantName;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String category;
    private String description;
    private String referenceNumber;
    private LocalDateTime createdAt;

    // User details (flattened to avoid circular reference)
    private Long userId;
    private String username;

    // Dispute status (flattened - indicates if transaction has an active dispute)
    private Boolean hasDispute;
    private Long disputeId;
}
