package com.disputehub.api.controller;

import com.disputehub.api.dto.MessageResponse;
import com.disputehub.api.dto.TransactionResponse;
import com.disputehub.api.entity.Transaction;
import com.disputehub.api.mapper.TransactionMapper;
import com.disputehub.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Transactions", description = "Bank transaction viewing endpoints for customers")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionMapper transactionMapper;

    @Operation(
        summary = "Get my transactions",
        description = "Retrieve paginated list of all transactions for the authenticated customer. Includes both disputed and non-disputed transactions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        )
    })
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/transactions - User: {}, Page: {}, Size: {}", userDetails.getUsername(), page, size);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Cap at 100
        Page<Transaction> transactions = transactionService.getMyTransactions(userDetails.getUsername(), pageable);
        Page<TransactionResponse> responses = transactions.map(transactionMapper::toResponse);
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get disputable transactions",
        description = "Retrieve paginated list of transactions that can be disputed (transactions without existing disputes). Useful for dispute creation form."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Disputable transactions retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        )
    })
    @GetMapping("/disputeable")
    public ResponseEntity<Page<TransactionResponse>> getDisputeableTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/transactions/disputeable - User: {}, Page: {}, Size: {}", userDetails.getUsername(), page, size);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Cap at 100
        Page<Transaction> transactions = transactionService.getDisputeableTransactions(userDetails.getUsername(), pageable);
        Page<TransactionResponse> responses = transactions.map(transactionMapper::toResponse);
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieve a specific transaction. Customer can only view their own transactions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transaction retrieved successfully",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - customer trying to access another customer's transaction",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Transaction not found",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @Parameter(description = "Transaction ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/transactions/{} - User: {}", id, userDetails.getUsername());
        Transaction transaction = transactionService.getTransactionById(id, userDetails.getUsername());
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }
}
