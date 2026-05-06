package com.disputehub.api.mapper;

import com.disputehub.api.dto.TransactionResponse;
import com.disputehub.api.entity.Transaction;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Transaction entities to DTOs.
 */
@Component
public class TransactionMapper {

    /**
     * Convert Transaction entity to TransactionResponse DTO.
     *
     * Flattens the object graph to prevent circular references
     * and includes dispute status information.
     */
    public TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .merchantName(transaction.getMerchantName())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .referenceNumber(transaction.getReferenceNumber())
                .createdAt(transaction.getCreatedAt())
                // Flatten user details
                .userId(transaction.getUser().getId())
                .username(transaction.getUser().getUsername())
                // Flatten dispute status
                .hasDispute(transaction.getDispute() != null)
                .disputeId(transaction.getDispute() != null ? transaction.getDispute().getId() : null)
                .build();
    }
}
