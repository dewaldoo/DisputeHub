package com.disputehub.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing a bank transaction.
 *
 * EXPLANATION:
 * Represents a customer's bank transaction (purchase, payment, etc.)
 * Customers can view their transactions and dispute them if fraudulent/incorrect.
 *
 * WHY BigDecimal FOR MONEY?
 * - NEVER use float/double for money (rounding errors!)
 * - float: 0.1 + 0.2 = 0.30000000000000004 ❌
 * - BigDecimal: 0.1 + 0.2 = 0.3 ✓
 * - BigDecimal provides exact decimal arithmetic (critical for financial apps)
 *
 * INDEXES:
 * - user_id: Most queries filter by user (getMyTransactions)
 * - transaction_date: Common sort field and date range queries
 * - Combined index (user_id, transaction_date): Optimizes the main customer query
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_user_id", columnList = "user_id"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_transaction_user_date", columnList = "user_id, transaction_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * MANY-TO-ONE RELATIONSHIP
     * Many transactions belong to one user
     *
     * @ManyToOne: Opposite side of User.transactions @OneToMany
     * @JoinColumn: Specifies foreign key column name in this table
     * nullable=false: Transaction MUST have a user (enforced at database level)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Merchant/store name where transaction occurred
     * Examples: "Amazon", "Woolworths", "Netflix"
     */
    @Column(nullable = false)
    private String merchantName;

    /**
     * Transaction amount in ZAR (South African Rand)
     *
     * @Column:
     * - precision=19: Total number of digits (before + after decimal)
     * - scale=2: Number of decimal places (cents)
     * Example: 1234567890123456.99 (19 total digits, 2 after decimal)
     *
     * WHY precision=19, scale=2?
     * - Handles up to 999,999,999,999,999.99 (quadrillion - more than enough)
     * - scale=2 for cents (R123.45)
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * When the transaction occurred (not when it was inserted into database)
     */
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    /**
     * Transaction category for filtering/reporting
     * Examples: "Groceries", "Entertainment", "Online Shopping", "Transport"
     */
    private String category;

    /**
     * Additional transaction details
     * Example: "Subscription payment" or "Refund for order #12345"
     */
    private String description;

    /**
     * Reference number from bank/payment processor
     * Useful for tracking disputes with merchant
     */
    private String referenceNumber;

    /**
     * Timestamp when transaction was recorded in our system
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ONE-TO-ONE RELATIONSHIP
     * One transaction can have at most one dispute
     *
     * mappedBy="transaction": Dispute entity owns this relationship
     * cascade=CascadeType.ALL: Deleting transaction deletes its dispute
     * orphanRemoval=true: If dispute is removed from transaction, delete it
     */
    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Dispute dispute;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Helper method to check if transaction can be disputed.
     */
    public boolean canBeDisputed() {
        return dispute == null;
    }
}
