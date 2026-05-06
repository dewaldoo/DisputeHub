package com.disputehub.api.service;

import com.disputehub.api.dto.CreateDisputeRequest;
import com.disputehub.api.dto.UpdateDisputeStatusRequest;
import com.disputehub.api.entity.*;
import com.disputehub.api.exception.BadRequestException;
import com.disputehub.api.exception.ResourceNotFoundException;
import com.disputehub.api.repository.AuditLogRepository;
import com.disputehub.api.repository.DisputeRepository;
import com.disputehub.api.repository.TransactionRepository;
import com.disputehub.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @CacheEvict(value = {"transactions", "disputeableTransactions", "myDisputes", "allDisputes"}, allEntries = true)
    @Transactional
    public Dispute createDispute(CreateDisputeRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Verify transaction belongs to user
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only dispute your own transactions");
        }

        // Check if transaction already has a dispute
        if (disputeRepository.findByTransaction_Id(transaction.getId()).isPresent()) {
            throw new BadRequestException("This transaction already has an active dispute");
        }

        // Create dispute
        Dispute dispute = Dispute.builder()
                .transaction(transaction)
                .user(user)
                .reason(request.getReason())
                .description(request.getDescription())
                .evidenceUrl(request.getEvidenceUrl())
                .status(DisputeStatus.PENDING)
                .build();

        dispute = disputeRepository.save(dispute);

        // Add business context to MDC for subsequent logs
        MDC.put("disputeId", String.valueOf(dispute.getId()));
        MDC.put("transactionId", String.valueOf(transaction.getId()));

        // Create audit log
        createAuditLog(dispute, user, "CREATED", null, DisputeStatus.PENDING.name());

        log.info("Dispute created successfully - Reason: {}, Status: {}",
                 dispute.getReason(), dispute.getStatus());
        return dispute;
    }

    @Cacheable(value = "myDisputes", key = "#username")
    public List<Dispute> getMyDisputes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return disputeRepository.findByUser_IdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Get all disputes (admin only) - PAGINATED, NOT CACHED.
     *
     * Admin views can have 100k+ disputes. Pagination prevents OOM.
     * No caching: each page/sort combination would be a different cache entry = cache pollution.
     *
     * Uses JOIN FETCH to prevent N+1 queries (loads user + transaction in single query).
     */
    public Page<Dispute> getAllDisputes(Pageable pageable) {
        return disputeRepository.findAllWithDetails(pageable);
    }

    public Dispute getDisputeById(Long id, String username) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Allow if admin or owns the dispute
        if (user.getRole() != Role.ADMIN && !dispute.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You can only view your own disputes");
        }

        return dispute;
    }

    @CacheEvict(value = {"myDisputes", "allDisputes", "auditLogs"}, allEntries = true)
    @Transactional
    public Dispute updateDisputeStatus(Long id, UpdateDisputeStatusRequest request, String username) {
        // Add dispute ID to MDC for all logs in this operation
        MDC.put("disputeId", String.valueOf(id));

        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        User admin = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only admin can update status
        if (admin.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only admins can update dispute status");
        }

        // Don't allow updating resolved disputes
        if (dispute.isResolved()) {
            throw new BadRequestException("Cannot update a resolved dispute");
        }

        DisputeStatus oldStatus = dispute.getStatus();
        dispute.setStatus(request.getStatus());

        if (request.getResolutionNotes() != null) {
            dispute.setResolutionNotes(request.getResolutionNotes());
        }

        dispute = disputeRepository.save(dispute);

        // Create audit log
        createAuditLog(dispute, admin, "STATUS_CHANGED", oldStatus.name(), request.getStatus().name());

        log.info("Dispute status updated - OldStatus: {}, NewStatus: {}, Notes: {}",
                 oldStatus, request.getStatus(), request.getResolutionNotes());
        return dispute;
    }

    private void createAuditLog(Dispute dispute, User actor, String action, String oldValue, String newValue) {
        AuditLog log = AuditLog.builder()
                .dispute(dispute)
                .actor(actor)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(log);
    }

    @Cacheable(value = "auditLogs", key = "#disputeId")
    public List<AuditLog> getDisputeAuditLogs(Long disputeId) {
        if (!disputeRepository.existsById(disputeId)) {
            throw new ResourceNotFoundException("Dispute not found");
        }
        return auditLogRepository.findByDispute_IdOrderByTimestampAsc(disputeId);
    }

    public java.util.Map<String, Long> getDisputeStatistics() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("PENDING", disputeRepository.countByStatus(DisputeStatus.PENDING));
        stats.put("UNDER_REVIEW", disputeRepository.countByStatus(DisputeStatus.UNDER_REVIEW));
        stats.put("MERCHANT_CONTACTED", disputeRepository.countByStatus(DisputeStatus.MERCHANT_CONTACTED));
        stats.put("RESOLVED", disputeRepository.countByStatus(DisputeStatus.RESOLVED));
        stats.put("REJECTED", disputeRepository.countByStatus(DisputeStatus.REJECTED));
        stats.put("TOTAL", disputeRepository.count());
        return stats;
    }
}
