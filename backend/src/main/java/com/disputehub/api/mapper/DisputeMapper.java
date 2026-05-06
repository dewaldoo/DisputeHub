package com.disputehub.api.mapper;

import com.disputehub.api.dto.DisputeResponse;
import com.disputehub.api.entity.Dispute;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Dispute entities to DTOs.
 *
 * WHY A SEPARATE MAPPER CLASS?
 * - Single Responsibility: Mapping logic in one place
 * - Testability: Can unit test mapping separately
 * - Reusability: Same mapper used across multiple controllers
 * - Maintainability: Easy to modify mappings when requirements change
 *
 * ENTERPRISE PATTERN:
 * In large projects, use MapStruct library for automatic mapping generation.
 * For this assessment, manual mapping shows understanding of the concept.
 */
@Component
public class DisputeMapper {

    /**
     * Convert Dispute entity to DisputeResponse DTO.
     *
     * IMPORTANT: This flattens the object graph to prevent circular references.
     * Instead of returning nested objects (Dispute → Transaction → User),
     * we flatten relevant fields into a single DTO.
     */
    public DisputeResponse toResponse(Dispute dispute) {
        return DisputeResponse.builder()
                .id(dispute.getId())
                .reason(dispute.getReason())
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .evidenceUrl(dispute.getEvidenceUrl())
                .resolutionNotes(dispute.getResolutionNotes())
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .resolvedAt(dispute.getResolvedAt())
                // Flatten transaction details
                .transactionId(dispute.getTransaction().getId())
                .merchantName(dispute.getTransaction().getMerchantName())
                .amount(dispute.getTransaction().getAmount())
                .transactionDate(dispute.getTransaction().getTransactionDate())
                .transactionCategory(dispute.getTransaction().getCategory())
                // Flatten user details
                .userId(dispute.getUser().getId())
                .username(dispute.getUser().getUsername())
                .userFullName(dispute.getUser().getFullName())
                .build();
    }
}
