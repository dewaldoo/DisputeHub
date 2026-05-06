package com.disputehub.api.mapper;

import com.disputehub.api.dto.AuditLogResponse;
import com.disputehub.api.entity.AuditLog;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting AuditLog entities to DTOs.
 */
@Component
public class AuditLogMapper {

    /**
     * Convert AuditLog entity to AuditLogResponse DTO.
     *
     * Flattens the object graph to prevent circular references.
     */
    public AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .notes(auditLog.getNotes())
                .timestamp(auditLog.getTimestamp())
                // Flatten dispute details
                .disputeId(auditLog.getDispute().getId())
                // Flatten actor details
                .actorId(auditLog.getActor().getId())
                .actorUsername(auditLog.getActor().getUsername())
                .actorFullName(auditLog.getActor().getFullName())
                .build();
    }
}
