package com.disputehub.api.repository;

import com.disputehub.api.entity.Dispute;
import com.disputehub.api.entity.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Dispute entity.
 */
@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    /**
     * Find all disputes for a specific user, ordered by creation date (newest first).
     *
     * USE CASE:
     * Customer viewing their dispute history
     */
    List<Dispute> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /**
     * Find disputes by status.
     *
     * USE CASE:
     * Admin dashboard filtering by status:
     * - Show all "UNDER_REVIEW" disputes that need attention
     * - Show all "PENDING" disputes waiting to be picked up
     */
    List<Dispute> findByStatusOrderByCreatedAtAsc(DisputeStatus status);

    /**
     * Find all disputes (for admin) with pagination and JOIN FETCH to avoid N+1.
     *
     * PAGINATED VERSION with eager loading.
     * Prevents N+1 queries by fetching user and transaction in single query.
     *
     * NOTE: countQuery is required when using JOIN FETCH with pagination.
     * Without it, Spring would apply LIMIT to the JOIN result, not the base query.
     */
    @Query(value = "SELECT DISTINCT d FROM Dispute d " +
                   "LEFT JOIN FETCH d.user " +
                   "LEFT JOIN FETCH d.transaction",
           countQuery = "SELECT COUNT(d) FROM Dispute d")
    Page<Dispute> findAllWithDetails(Pageable pageable);

    /**
     * Find disputes by status with JOIN FETCH for admin filtering.
     */
    @Query(value = "SELECT DISTINCT d FROM Dispute d " +
                   "LEFT JOIN FETCH d.user " +
                   "LEFT JOIN FETCH d.transaction " +
                   "WHERE d.status = :status",
           countQuery = "SELECT COUNT(d) FROM Dispute d WHERE d.status = :status")
    Page<Dispute> findByStatusWithDetails(@Param("status") DisputeStatus status, Pageable pageable);

    /**
     * Find dispute by transaction ID.
     *
     * WHY OPTIONAL?
     * - Transaction might not have a dispute
     * - Forces caller to handle "not found" case
     * - Safer than returning null
     *
     * USE CASE:
     * Check if transaction already has dispute before allowing new dispute
     * ```java
     * if (disputeRepository.findByTransaction_Id(transactionId).isPresent()) {
     *     throw new DisputeAlreadyExistsException();
     * }
     * ```
     */
    Optional<Dispute> findByTransaction_Id(Long transactionId);

    /**
     * Count disputes by status.
     *
     * USE CASE:
     * Admin dashboard metrics:
     * - 5 disputes submitted
     * - 3 under review
     * - 10 resolved
     */
    long countByStatus(DisputeStatus status);

    /**
     * Count total disputes for a user.
     *
     * USE CASE:
     * User profile stats: "You have created 7 disputes"
     */
    long countByUser_Id(Long userId);

    /**
     * Find disputes by multiple statuses (flexible filtering).
     *
     * METHOD NAME PARSING:
     * - StatusIn → WHERE status IN (?, ?, ...)
     *
     * GENERATED SQL:
     * SELECT * FROM disputes
     * WHERE status IN ('PENDING', 'UNDER_REVIEW')
     * ORDER BY created_at ASC
     *
     * USE CASE:
     * Admin filtering: "Show me all active disputes"
     * ```java
     * List<DisputeStatus> activeStatuses = Arrays.asList(
     *     DisputeStatus.PENDING,
     *     DisputeStatus.UNDER_REVIEW,
     *     DisputeStatus.MERCHANT_CONTACTED
     * );
     * List<Dispute> activeDisputes = disputeRepository.findByStatusInOrderByCreatedAtAsc(activeStatuses);
     * ```
     */
    List<Dispute> findByStatusInOrderByCreatedAtAsc(List<DisputeStatus> statuses);

}
