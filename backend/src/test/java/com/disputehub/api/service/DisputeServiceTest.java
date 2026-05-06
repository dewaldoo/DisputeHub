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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DisputeService.
 */
@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private DisputeService disputeService;

    private User testCustomer;
    private Transaction testTransaction;
    private CreateDisputeRequest testRequest;

    @BeforeEach
    void setUp() {
        testCustomer = User.builder()
                .id(1L)
                .username("john.doe@example.com")
                .email("john.doe@example.com")
                .fullName("John Doe")
                .role(Role.CUSTOMER)
                .build();

        testTransaction = Transaction.builder()
                .id(100L)
                .user(testCustomer)
                .merchantName("Amazon")
                .amount(new BigDecimal("499.99"))
                .transactionDate(LocalDateTime.now().minusDays(2))
                .category("Online Shopping")
                .referenceNumber("TXN100")
                .build();

        testRequest = new CreateDisputeRequest();
        testRequest.setTransactionId(100L);
        testRequest.setReason("UNAUTHORIZED");
        testRequest.setDescription("I did not make this purchase");
    }

    @Test
    void createDispute_shouldSucceed_whenValidRequest() {
        // ARRANGE: Setup mock behavior
        when(userRepository.findByUsername("john.doe@example.com"))
                .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findById(100L))
                .thenReturn(Optional.of(testTransaction));
        when(disputeRepository.findByTransaction_Id(100L))
                .thenReturn(Optional.empty()); // No existing dispute
        when(disputeRepository.save(any(Dispute.class)))
                .thenAnswer(invocation -> {
                    Dispute dispute = invocation.getArgument(0);
                    dispute.setId(1L); // Simulate DB-generated ID
                    return dispute;
                });

        // ACT: Call the method being tested
        Dispute result = disputeService.createDispute(testRequest, "john.doe@example.com");

        // ASSERT: Verify results
        assertThat(result).isNotNull();
        assertThat(result.getReason()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getStatus()).isEqualTo(DisputeStatus.PENDING);
        assertThat(result.getUser()).isEqualTo(testCustomer);
        assertThat(result.getTransaction()).isEqualTo(testTransaction);

        // Verify repository methods were called
        verify(disputeRepository).save(any(Dispute.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void createDispute_shouldThrowException_whenTransactionNotFound() {
        // ARRANGE
        when(userRepository.findByUsername("john.doe@example.com"))
                .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findById(100L))
                .thenReturn(Optional.empty()); // Transaction doesn't exist

        // ACT & ASSERT
        assertThatThrownBy(() ->
                disputeService.createDispute(testRequest, "john.doe@example.com")
        )
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found");

        // Verify dispute was NOT saved
        verify(disputeRepository, never()).save(any());
    }

    @Test
    void createDispute_shouldThrowException_whenTransactionBelongsToOtherUser() {
        // ARRANGE
        User otherUser = User.builder()
                .id(2L)
                .username("jane.smith@example.com")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByUsername("jane.smith@example.com"))
                .thenReturn(Optional.of(otherUser));
        when(transactionRepository.findById(100L))
                .thenReturn(Optional.of(testTransaction)); // Belongs to testCustomer, not otherUser

        // ACT & ASSERT
        assertThatThrownBy(() ->
                disputeService.createDispute(testRequest, "jane.smith@example.com")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("You can only dispute your own transactions");

        verify(disputeRepository, never()).save(any());
    }

    @Test
    void createDispute_shouldThrowException_whenTransactionAlreadyDisputed() {
        // ARRANGE
        Dispute existingDispute = Dispute.builder()
                .id(1L)
                .transaction(testTransaction)
                .status(DisputeStatus.PENDING)
                .build();

        when(userRepository.findByUsername("john.doe@example.com"))
                .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findById(100L))
                .thenReturn(Optional.of(testTransaction));
        when(disputeRepository.findByTransaction_Id(100L))
                .thenReturn(Optional.of(existingDispute)); // Already has dispute!

        // ACT & ASSERT
        assertThatThrownBy(() ->
                disputeService.createDispute(testRequest, "john.doe@example.com")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has an active dispute");

        verify(disputeRepository, never()).save(any());
    }

    @Test
    void updateDisputeStatus_shouldSucceed_whenAdmin() {
        // ARRANGE
        User admin = User.builder()
                .id(99L)
                .username("admin@disputehub.com")
                .role(Role.ADMIN)
                .build();

        Dispute existingDispute = Dispute.builder()
                .id(1L)
                .transaction(testTransaction)
                .user(testCustomer)
                .status(DisputeStatus.PENDING)
                .build();

        UpdateDisputeStatusRequest statusRequest = new UpdateDisputeStatusRequest();
        statusRequest.setStatus(DisputeStatus.UNDER_REVIEW);
        statusRequest.setResolutionNotes("Investigating with merchant");

        when(disputeRepository.findById(1L))
                .thenReturn(Optional.of(existingDispute));
        when(userRepository.findByUsername("admin@disputehub.com"))
                .thenReturn(Optional.of(admin));
        when(disputeRepository.save(any(Dispute.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        Dispute result = disputeService.updateDisputeStatus(1L, statusRequest, "admin@disputehub.com");

        // ASSERT
        assertThat(result.getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);
        assertThat(result.getResolutionNotes()).isEqualTo("Investigating with merchant");
        verify(disputeRepository).save(existingDispute);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void updateDisputeStatus_shouldThrowException_whenNotAdmin() {
        // ARRANGE
        Dispute existingDispute = Dispute.builder()
                .id(1L)
                .status(DisputeStatus.PENDING)
                .build();

        UpdateDisputeStatusRequest statusRequest = new UpdateDisputeStatusRequest();
        statusRequest.setStatus(DisputeStatus.UNDER_REVIEW);

        when(disputeRepository.findById(1L))
                .thenReturn(Optional.of(existingDispute));
        when(userRepository.findByUsername("john.doe@example.com"))
                .thenReturn(Optional.of(testCustomer)); // CUSTOMER, not ADMIN

        // ACT & ASSERT
        assertThatThrownBy(() ->
                disputeService.updateDisputeStatus(1L, statusRequest, "john.doe@example.com")
        )
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only admins can update dispute status");

        verify(disputeRepository, never()).save(any());
    }

}
