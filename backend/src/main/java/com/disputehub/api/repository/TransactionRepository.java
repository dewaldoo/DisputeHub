package com.disputehub.api.repository;

import com.disputehub.api.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a specific user, ordered by date (newest first).
     * PAGINATED VERSION - prevents OOM with millions of transactions.
     *
     * Spring Data automatically adds LIMIT/OFFSET based on Pageable.
     *
     * GENERATED SQL:
     * SELECT * FROM transactions
     * WHERE user_id = ?
     * ORDER BY transaction_date DESC
     * LIMIT ? OFFSET ?
     */
    Page<Transaction> findByUser_IdOrderByTransactionDateDesc(Long userId, Pageable pageable);

    /**
     * Find transactions for a user within a date range.
     *
     * CUSTOM JPQL QUERY:
     * When method names get too complex, use @Query annotation.
     *
     * JPQL (Java Persistence Query Language):
     * - Like SQL but uses entity names and field names (not table/column names)
     * - "Transaction t" refers to Transaction entity (not "transactions" table)
     * - "t.user.id" navigates relationships
     *
     * @Param:
     * Maps method parameters to named parameters in query (:userId, :startDate, :endDate)
     *
     * WHY JPQL INSTEAD OF SQL?
     * - Database-agnostic (works on any database)
     * - Type-safe (references entities, not tables)
     * - Cleaner navigation of relationships
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate " +
           "ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find all transactions that don't have disputes (can be disputed).
     * PAGINATED VERSION.
     */
    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.dispute IS NULL " +
           "ORDER BY t.transactionDate DESC")
    Page<Transaction> findDisputeableTransactionsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Count transactions for a user.
     *
     * METHOD NAME PARSING:
     * - count → Returns long (count)
     * - By → WHERE clause
     * - User_Id → WHERE user_id = ?
     */
    long countByUser_Id(Long userId);

}
