package com.disputehub.api.service;

import com.disputehub.api.entity.Transaction;
import com.disputehub.api.entity.User;
import com.disputehub.api.exception.ResourceNotFoundException;
import com.disputehub.api.repository.TransactionRepository;
import com.disputehub.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get paginated transactions for a user.
     * Caches ONLY the first page (most common access pattern).
     *
     * STRATEGY: Cache first page only to avoid cache pollution.
     * In banking systems, users can accumulate millions of transactions over time.
     * Pagination is required to prevent OOM, but caching every page wastes memory.
     * Most users only view the first page (recent transactions).
     */
    @Cacheable(value = "transactions", key = "#username", condition = "#pageable.pageNumber == 0")
    public Page<Transaction> getMyTransactions(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return transactionRepository.findByUser_IdOrderByTransactionDateDesc(user.getId(), pageable);
    }

    /**
     * Get paginated disputeable transactions for a user.
     * Caches ONLY the first page.
     */
    @Cacheable(value = "disputeableTransactions", key = "#username", condition = "#pageable.pageNumber == 0")
    public Page<Transaction> getDisputeableTransactions(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return transactionRepository.findDisputeableTransactionsByUserId(user.getId(), pageable);
    }

    public Transaction getTransactionById(Long id, String username) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify transaction belongs to user
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Transaction not found");
        }

        return transaction;
    }
}
