package com.disputehub.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking all actions taken on disputes.
 *
 * EXPLANATION:
 * Provides complete audit trail for compliance and debugging.
 * Every status change, update, or action on a dispute creates an audit log entry.
 *
 * WHY AUDIT LOGS ARE CRITICAL IN BANKING:
 * - Regulatory compliance (financial regulations require audit trails)
 * - Security (detect unauthorized changes)
 * - Debugging (understand how dispute reached current state)
 * - Accountability (who changed what and when)
 * - Dispute resolution (evidence for customer complaints)
 *
 * EXAMPLE ENTRIES:
 * - "Admin John changed status from PENDING to UNDER_REVIEW"
 * - "Customer Jane submitted dispute"
 * - "Admin Sarah added resolution notes: 'Approved refund'"
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MANY-TO-ONE RELATIONSHIP
     * Many audit logs belong to one dispute
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    /**
     * MANY-TO-ONE RELATIONSHIP
     * Many audit logs created by one user (the actor who performed the action)
     *
     * "Actor" = Person who performed the action
     * Could be customer (submitting dispute) or admin (changing status)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    /**
     * What action was performed
     * Examples:
     * - "STATUS_CHANGED"
     * - "CREATED"
     * - "RESOLUTION_NOTES_ADDED"
     * - "EVIDENCE_UPLOADED"
     *
     * COULD BE ENHANCED:
     * Make this an enum for type safety
     */
    @Column(nullable = false)
    private String action;

    /**
     * Previous value before change
     * Example: "PENDING" (when status changed from PENDING to UNDER_REVIEW)
     *
     * Null for create actions (no previous value)
     */
    private String oldValue;

    /**
     * New value after change
     * Example: "UNDER_REVIEW" (when status changed from PENDING to UNDER_REVIEW)
     */
    private String newValue;

    /**
     * Additional context or notes about the action
     * Example: "Customer provided receipt as evidence"
     */
    @Column(length = 500)
    private String notes;

    /**
     * When the action occurred
     *
     * IMPORTANT:
     * - Use updatable=false to prevent accidental changes
     * - Timestamps in audit logs should NEVER be modified
     * - This is the source of truth for "when did this happen?"
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

}
