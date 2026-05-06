package com.disputehub.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for AuditLog entity.
 *
 * Provides audit trail information without circular references.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String action;
    private String oldValue;
    private String newValue;
    private String notes;
    private LocalDateTime timestamp;

    // Dispute details (flattened)
    private Long disputeId;

    // Actor details (flattened)
    private Long actorId;
    private String actorUsername;
    private String actorFullName;
}
