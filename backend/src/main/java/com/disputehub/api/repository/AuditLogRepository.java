package com.disputehub.api.repository;

import com.disputehub.api.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for AuditLog entity.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific dispute, ordered by timestamp.
     *
     * USE CASE:
     * Display complete history of a dispute:
     * - When it was created
     * - Who changed status and when
     * - What notes were added
     *
     * EXAMPLE OUTPUT:
     * 1. 2024-04-16 10:00 - John Doe (CUSTOMER) - CREATED
     * 2. 2024-04-16 11:30 - Admin Sarah - STATUS_CHANGED: PENDING → UNDER_REVIEW
     * 3. 2024-04-16 14:00 - Admin Sarah - STATUS_CHANGED: UNDER_REVIEW → MERCHANT_CONTACTED
     * 4. 2024-04-17 09:00 - Admin Sarah - STATUS_CHANGED: MERCHANT_CONTACTED → RESOLVED
     */
    List<AuditLog> findByDispute_IdOrderByTimestampAsc(Long disputeId);

    /**
     * Find all actions performed by a specific user (actor).
     *
     * USE CASE:
     * Security audit: "Show me all actions taken by user X"
     * Performance review: "How many disputes did Admin Y process this month?"
     */
    List<AuditLog> findByActor_IdOrderByTimestampDesc(Long actorId);

    /**
     * Find audit logs by action type.
     *
     * USE CASE:
     * Reporting: "Show me all status changes today"
     * Analytics: "How many disputes were resolved this week?"
     */
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

}
