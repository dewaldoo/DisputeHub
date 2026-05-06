package com.disputehub.api.repository;

import com.disputehub.api.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration tests using in-memory H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
class DisputeRepositoryIntegrationTest {

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User customer;
    private Transaction transaction1;
    private Transaction transaction2;

    @BeforeEach
    void setUp() {
        // Create test data using TestEntityManager
        customer = User.builder()
                .username("test.customer@example.com")
                .email("test.customer@example.com")
                .password("$2a$10$testHashedPassword")
                .fullName("Test Customer")
                .role(Role.CUSTOMER)
                .build();
        customer = entityManager.persist(customer);

        transaction1 = Transaction.builder()
                .user(customer)
                .merchantName("Test Merchant 1")
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDateTime.now().minusDays(3))
                .category("Shopping")
                .referenceNumber("TEST001")
                .build();
        transaction1 = entityManager.persist(transaction1);

        transaction2 = Transaction.builder()
                .user(customer)
                .merchantName("Test Merchant 2")
                .amount(new BigDecimal("200.00"))
                .transactionDate(LocalDateTime.now().minusDays(1))
                .category("Shopping")
                .referenceNumber("TEST002")
                .build();
        transaction2 = entityManager.persist(transaction2);

        entityManager.flush(); // Force write to DB
    }

    @Test
    void findByUser_IdOrderByCreatedAtDesc_shouldReturnUserDisputes() throws InterruptedException {
        // ARRANGE: Create disputes with time gap to ensure ordering
        Dispute dispute1 = Dispute.builder()
                .transaction(transaction1)
                .user(customer)
                .reason("UNAUTHORIZED")
                .description("Test dispute 1")
                .status(DisputeStatus.PENDING)
                .build();
        disputeRepository.save(dispute1);
        entityManager.flush();

        // Ensure createdAt timestamps are different
        Thread.sleep(10);

        Dispute dispute2 = Dispute.builder()
                .transaction(transaction2)
                .user(customer)
                .reason("INCORRECT_AMOUNT")
                .description("Test dispute 2")
                .status(DisputeStatus.UNDER_REVIEW)
                .build();
        disputeRepository.save(dispute2);

        entityManager.flush();
        entityManager.clear(); // Clear cache to force fresh query

        // ACT: Execute query
        List<Dispute> result = disputeRepository.findByUser_IdOrderByCreatedAtDesc(customer.getId());

        // ASSERT
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDescription()).isEqualTo("Test dispute 2"); // Newer first
        assertThat(result.get(1).getDescription()).isEqualTo("Test dispute 1");
    }

    @Test
    void findByTransaction_Id_shouldReturnDisputeWhenExists() {
        // ARRANGE
        Dispute dispute = Dispute.builder()
                .transaction(transaction1)
                .user(customer)
                .reason("UNAUTHORIZED")
                .description("Test dispute")
                .status(DisputeStatus.PENDING)
                .build();
        disputeRepository.save(dispute);
        entityManager.flush();

        // ACT
        Optional<Dispute> result = disputeRepository.findByTransaction_Id(transaction1.getId());

        // ASSERT
        assertThat(result).isPresent();
        assertThat(result.get().getTransaction().getId()).isEqualTo(transaction1.getId());
    }

    @Test
    void findAllWithDetails_shouldReturnPaginatedResults() {
        // ARRANGE: Create 25 disputes
        for (int i = 0; i < 25; i++) {
            Transaction txn = Transaction.builder()
                    .user(customer)
                    .merchantName("Merchant " + i)
                    .amount(new BigDecimal("50.00"))
                    .transactionDate(LocalDateTime.now().minusDays(i))
                    .category("Test")
                    .referenceNumber("TEST-" + i)
                    .build();
            txn = entityManager.persist(txn);

            Dispute dispute = Dispute.builder()
                    .transaction(txn)
                    .user(customer)
                    .reason("UNAUTHORIZED")
                    .description("Dispute " + i)
                    .status(DisputeStatus.PENDING)
                    .build();
            entityManager.persist(dispute);
        }
        entityManager.flush();
        entityManager.clear(); // Clear cache

        // ACT: Request page 0 (first 10 records)
        Pageable pageable = PageRequest.of(0, 10);
        Page<Dispute> result = disputeRepository.findAllWithDetails(pageable);

        // ASSERT: Pagination metadata
        assertThat(result.getContent()).hasSize(10); // Page size
        assertThat(result.getTotalElements()).isEqualTo(25); // Total count
        assertThat(result.getTotalPages()).isEqualTo(3); // 25 / 10 = 3 pages
        assertThat(result.getNumber()).isEqualTo(0); // Current page (0-indexed)
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isFalse();

        // ASSERT: JOIN FETCH worked (no lazy loading exception)
        Dispute firstDispute = result.getContent().get(0);
        assertThat(firstDispute.getUser()).isNotNull();
        assertThat(firstDispute.getUser().getFullName()).isEqualTo("Test Customer");
        assertThat(firstDispute.getTransaction()).isNotNull();
        assertThat(firstDispute.getTransaction().getMerchantName()).isNotBlank();

        // ACT: Request page 2 (last page)
        Pageable page2 = PageRequest.of(2, 10);
        Page<Dispute> lastPage = disputeRepository.findAllWithDetails(page2);

        // ASSERT
        assertThat(lastPage.getContent()).hasSize(5); // 25 - 20 = 5 remaining
        assertThat(lastPage.isLast()).isTrue();
    }

    @Test
    void countByStatus_shouldReturnCorrectCounts() {
        // ARRANGE: Create disputes with different statuses
        createDispute(transaction1, DisputeStatus.PENDING);
        createDispute(transaction2, DisputeStatus.UNDER_REVIEW);

        // Create more transactions for additional disputes
        Transaction txn3 = createTransaction("TEST003");
        Transaction txn4 = createTransaction("TEST004");
        createDispute(txn3, DisputeStatus.PENDING);
        createDispute(txn4, DisputeStatus.RESOLVED);

        entityManager.flush();

        // ACT
        long submittedCount = disputeRepository.countByStatus(DisputeStatus.PENDING);
        long reviewCount = disputeRepository.countByStatus(DisputeStatus.UNDER_REVIEW);
        long resolvedCount = disputeRepository.countByStatus(DisputeStatus.RESOLVED);

        // ASSERT
        assertThat(submittedCount).isEqualTo(2);
        assertThat(reviewCount).isEqualTo(1);
        assertThat(resolvedCount).isEqualTo(1);
    }

    // Helper methods
    private Dispute createDispute(Transaction txn, DisputeStatus status) {
        Dispute dispute = Dispute.builder()
                .transaction(txn)
                .user(customer)
                .reason("TEST")
                .description("Test dispute")
                .status(status)
                .build();
        return entityManager.persist(dispute);
    }

    private Transaction createTransaction(String refNumber) {
        Transaction txn = Transaction.builder()
                .user(customer)
                .merchantName("Test Merchant")
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDateTime.now())
                .category("Test")
                .referenceNumber(refNumber)
                .build();
        return entityManager.persist(txn);
    }

}
