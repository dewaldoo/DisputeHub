package com.disputehub.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dispute entity representing a customer's challenge to a transaction.
 *
 * EXPLANATION:
 * When a customer believes a transaction is fraudulent, incorrect, or unauthorized,
 * they create a Dispute. This entity tracks the dispute through its lifecycle
 * from submission to resolution.
 *
 * KEY FEATURES:
 * - Links to transaction being disputed
 * - Tracks status changes over time
 * - Stores customer's reason and evidence
 * - Records admin's resolution notes
 * - Maintains audit trail via AuditLog entities
 *
 * INDEXES:
 * - user_id: Filter by customer (getMyDisputes)
 * - status: Admin filters by status (pending, resolved, etc.)
 * - created_at: Common sort field
 * - transaction_id: Unique constraint + lookup
 */
@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_dispute_user_id", columnList = "user_id"),
    @Index(name = "idx_dispute_status", columnList = "status"),
    @Index(name = "idx_dispute_created_at", columnList = "created_at"),
    @Index(name = "idx_dispute_transaction_id", columnList = "transaction_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ONE-TO-ONE RELATIONSHIP
     * One dispute is about exactly one transaction
     *
     * @OneToOne: Opposite side of Transaction.dispute
     * @JoinColumn: This table owns the relationship (has the foreign key)
     * nullable=false: Every dispute MUST reference a transaction
     * unique=true: Each transaction can only have ONE dispute
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    /**
     * MANY-TO-ONE RELATIONSHIP
     * Many disputes can be created by one user
     *
     * WHY STORE user_id when we have transaction.user?
     * - Performance: Don't need to join through transaction to get user
     * - Flexibility: Could support third-party disputes in future
     * - Clarity: Makes it explicit who created the dispute
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Reason for the dispute
     * Examples:
     * - "UNAUTHORIZED" - didn't make this transaction
     * - "INCORRECT_AMOUNT" - was charged wrong amount
     * - "SERVICE_NOT_RECEIVED" - paid but didn't get goods/service
     * - "DUPLICATE_CHARGE" - was charged twice
     * - "OTHER" - custom reason
     *
     * COULD BE ENHANCED:
     * Make this an enum for better validation and reporting
     */
    @Column(nullable = false)
    private String reason;

    /**
     * Detailed description from customer
     * Example: "I never authorized this R500 charge from XYZ Store.
     *          My card was in my possession at the time."
     */
    @Column(nullable = false, length = 1000)
    private String description;

    /**
     * Current status of the dispute
     * See DisputeStatus enum for possible values and workflow
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    /**
     * Admin's notes about the resolution
     * Examples:
     * - "Verified with merchant. Charge was legitimate. Customer confirmed purchase."
     * - "Fraudulent transaction confirmed. Full refund issued."
     * - "Merchant provided proof of delivery. Rejecting dispute."
     *
     * Only populated when status is RESOLVED or REJECTED
     */
    @Column(length = 1000)
    private String resolutionNotes;

    /**
     * File path or URL to customer's evidence
     * Examples:
     * - Receipt photo
     * - Screenshot of issue
     * - Email correspondence
     *
     * CURRENT IMPLEMENTATION: Just stores string (file path or URL)
     * COULD BE ENHANCED: Store files in blob storage (AWS S3, Azure Blob)
     */
    private String evidenceUrl;

    /**
     * When the dispute was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the dispute was last modified
     *
     * AUTOMATICALLY UPDATED:
     * Thanks to @PreUpdate callback below
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * When the dispute was resolved/rejected
     * Null if still in progress
     *
     * USEFUL FOR:
     * - Calculating resolution time (resolvedAt - createdAt)
     * - SLA monitoring (did we resolve within X days?)
     */
    private LocalDateTime resolvedAt;

    /**
     * ONE-TO-MANY RELATIONSHIP
     * One dispute has many audit log entries
     *
     * AUDIT TRAIL:
     * Every time the dispute status changes, we create an AuditLog entry.
     * This provides:
     * - Complete history of who did what and when
     * - Regulatory compliance (banking regulations require audit trails)
     * - Debugging (understand why dispute ended up in certain state)
     *
     * @Builder.Default: Lombok Builder pattern - initializes empty list
     * Without this, builder would set field to null
     */
    @OneToMany(mappedBy = "dispute", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();

    /**
     * JPA LIFECYCLE CALLBACKS
     * These methods are automatically called by JPA at specific times
     */

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // All disputes start in PENDING status
        if (status == null) {
            status = DisputeStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // If status changed to RESOLVED or REJECTED, record when
        if ((status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED) && resolvedAt == null) {
            resolvedAt = LocalDateTime.now();
        }
    }

    /**
     * HELPER METHOD
     * Check if dispute is in a final state
     *
     * USEFUL FOR:
     * - Preventing status changes on closed disputes
     * - UI logic (show/hide action buttons)
     */
    public boolean isResolved() {
        return status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED;
    }

    /**
     * HELPER METHOD
     * Add audit log entry to dispute
     *
     * WHY HELPER METHOD?
     * - Maintains bidirectional relationship
     * - Ensures dispute reference is set in audit log
     * - Cleaner than doing this manually everywhere
     */
    public void addAuditLog(AuditLog auditLog) {
        auditLogs.add(auditLog);
        auditLog.setDispute(this);
    }
}
