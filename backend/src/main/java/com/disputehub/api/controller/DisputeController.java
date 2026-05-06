package com.disputehub.api.controller;

import com.disputehub.api.dto.AuditLogResponse;
import com.disputehub.api.dto.CreateDisputeRequest;
import com.disputehub.api.dto.DisputeResponse;
import com.disputehub.api.dto.MessageResponse;
import com.disputehub.api.dto.UpdateDisputeStatusRequest;
import com.disputehub.api.entity.AuditLog;
import com.disputehub.api.entity.Dispute;
import com.disputehub.api.mapper.AuditLogMapper;
import com.disputehub.api.mapper.DisputeMapper;
import com.disputehub.api.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@Tag(name = "Disputes", description = "Transaction dispute management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DisputeController {

    private static final Logger log = LoggerFactory.getLogger(DisputeController.class);

    @Autowired
    private DisputeService disputeService;

    @Autowired
    private DisputeMapper disputeMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Operation(
        summary = "Create a new dispute",
        description = "Customer can dispute a transaction they believe is unauthorized, incorrect, or fraudulent. Each transaction can only be disputed once."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dispute created successfully",
            content = @Content(schema = @Schema(implementation = DisputeResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - transaction already disputed, not owned by customer, or validation failed",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        )
    })
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DisputeResponse> createDispute(
            @Valid @RequestBody CreateDisputeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /api/disputes - User: {}, TransactionId: {}", userDetails.getUsername(), request.getTransactionId());
        Dispute dispute = disputeService.createDispute(request, userDetails.getUsername());
        return ResponseEntity.ok(disputeMapper.toResponse(dispute));
    }

    @Operation(
        summary = "Get my disputes",
        description = "Retrieve all disputes created by the authenticated customer, ordered by creation date (newest first)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of customer's disputes retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        )
    })
    @GetMapping("/my-disputes")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<DisputeResponse>> getMyDisputes(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/disputes/my-disputes - User: {}", userDetails.getUsername());
        List<Dispute> disputes = disputeService.getMyDisputes(userDetails.getUsername());
        List<DisputeResponse> responses = disputes.stream()
                .map(disputeMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get all disputes (Admin)",
        description = "Retrieve paginated list of all disputes across all customers. Supports sorting and pagination."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Paginated disputes retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires ADMIN role"
        )
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DisputeResponse>> getAllDisputes(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field: createdAt, status, updatedAt") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: ASC or DESC") @RequestParam(defaultValue = "DESC") String direction) {
        log.info("GET /api/disputes - Admin fetching all disputes - Page: {}, Size: {}", page, size);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(sortDirection, sortBy));
        Page<Dispute> disputes = disputeService.getAllDisputes(pageable);
        Page<DisputeResponse> responses = disputes.map(disputeMapper::toResponse);
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get dispute by ID",
        description = "Retrieve a specific dispute. Customers can only view their own disputes. Admins can view all disputes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dispute retrieved successfully",
            content = @Content(schema = @Schema(implementation = DisputeResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - customer trying to access another customer's dispute"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Dispute not found"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<DisputeResponse> getDisputeById(
            @Parameter(description = "Dispute ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/disputes/{} - User: {}", id, userDetails.getUsername());
        Dispute dispute = disputeService.getDisputeById(id, userDetails.getUsername());
        return ResponseEntity.ok(disputeMapper.toResponse(dispute));
    }

    @Operation(
        summary = "Update dispute status (Admin)",
        description = "Admin can update dispute status: PENDING → UNDER_REVIEW → MERCHANT_CONTACTED → RESOLVED/REJECTED. Creates audit log entry for each status change."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dispute status updated successfully",
            content = @Content(schema = @Schema(implementation = DisputeResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status transition or validation failed"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires ADMIN role"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Dispute not found"
        )
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DisputeResponse> updateDisputeStatus(
            @Parameter(description = "Dispute ID") @PathVariable Long id,
            @Valid @RequestBody UpdateDisputeStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("PUT /api/disputes/{}/status - Admin: {}, NewStatus: {}", id, userDetails.getUsername(), request.getStatus());
        Dispute dispute = disputeService.updateDisputeStatus(id, request, userDetails.getUsername());
        return ResponseEntity.ok(disputeMapper.toResponse(dispute));
    }

    @Operation(
        summary = "Get dispute audit logs (Admin)",
        description = "Retrieve complete audit trail for a dispute showing all status changes, who made them, and when."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Audit logs retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - requires ADMIN role"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Dispute not found"
        )
    })
    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getDisputeAuditLogs(
            @Parameter(description = "Dispute ID") @PathVariable Long id) {
        log.info("GET /api/disputes/{}/audit-logs", id);
        List<AuditLog> auditLogs = disputeService.getDisputeAuditLogs(id);
        List<AuditLogResponse> responses = auditLogs.stream()
                .map(auditLogMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
