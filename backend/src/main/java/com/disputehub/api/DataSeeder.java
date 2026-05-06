package com.disputehub.api;

import com.disputehub.api.entity.*;
import com.disputehub.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Seeder - Populates database with sample data for demo/testing.
 *
 * CommandLineRunner runs after application starts.
 * Useful for:
 * - Demo data
 * - Initial admin user
 * - Test data for development
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only seed if database is empty
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping data seeder.");
            return;
        }

        log.info("Seeding database with sample data...");

        // Create admin user
        User admin = User.builder()
                .username("admin@disputehub.com")
                .password(passwordEncoder.encode("Admin@123"))
                .fullName("System Administrator")
                .email("admin@disputehub.com")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);

        // Create customer users
        User customer1 = User.builder()
                .username("john.doe@example.com")
                .password(passwordEncoder.encode("Password@123"))
                .fullName("John Doe")
                .email("john.doe@example.com")
                .role(Role.CUSTOMER)
                .build();
        customer1 = userRepository.save(customer1);

        User customer2 = User.builder()
                .username("jane.smith@example.com")
                .password(passwordEncoder.encode("Password@123"))
                .fullName("Jane Smith")
                .email("jane.smith@example.com")
                .role(Role.CUSTOMER)
                .build();
        customer2 = userRepository.save(customer2);

        // Create transactions for customer1
        List<Transaction> transactions1 = new ArrayList<>();

        transactions1.add(Transaction.builder()
                .user(customer1)
                .merchantName("Amazon")
                .amount(new BigDecimal("499.99"))
                .transactionDate(LocalDateTime.now().minusDays(5))
                .category("Online Shopping")
                .description("Laptop purchase")
                .referenceNumber("TXN001")
                .build());

        transactions1.add(Transaction.builder()
                .user(customer1)
                .merchantName("Woolworths")
                .amount(new BigDecimal("250.50"))
                .transactionDate(LocalDateTime.now().minusDays(3))
                .category("Groceries")
                .description("Weekly grocery shopping")
                .referenceNumber("TXN002")
                .build());

        transactions1.add(Transaction.builder()
                .user(customer1)
                .merchantName("Netflix")
                .amount(new BigDecimal("129.00"))
                .transactionDate(LocalDateTime.now().minusDays(2))
                .category("Entertainment")
                .description("Monthly subscription")
                .referenceNumber("TXN003")
                .build());

        transactions1.add(Transaction.builder()
                .user(customer1)
                .merchantName("Uber")
                .amount(new BigDecimal("85.50"))
                .transactionDate(LocalDateTime.now().minusDays(1))
                .category("Transport")
                .description("Trip to office")
                .referenceNumber("TXN004")
                .build());

        transactionRepository.saveAll(transactions1);

        // Create transactions for customer2
        List<Transaction> transactions2 = new ArrayList<>();

        transactions2.add(Transaction.builder()
                .user(customer2)
                .merchantName("Takealot")
                .amount(new BigDecimal("1250.00"))
                .transactionDate(LocalDateTime.now().minusDays(7))
                .category("Online Shopping")
                .description("Electronics purchase")
                .referenceNumber("TXN005")
                .build());

        transactions2.add(Transaction.builder()
                .user(customer2)
                .merchantName("Pick n Pay")
                .amount(new BigDecimal("450.75"))
                .transactionDate(LocalDateTime.now().minusDays(4))
                .category("Groceries")
                .description("Weekly shopping")
                .referenceNumber("TXN006")
                .build());

        transactions2.add(Transaction.builder()
                .user(customer2)
                .merchantName("Fraudulent Store XYZ")
                .amount(new BigDecimal("5000.00"))
                .transactionDate(LocalDateTime.now().minusDays(2))
                .category("Unknown")
                .description("Suspicious transaction")
                .referenceNumber("TXN007")
                .build());

        transactionRepository.saveAll(transactions2);

        // Create dispute for customer1
        Transaction disputedTx1 = transactions1.get(0); // Amazon purchase
        Dispute dispute1 = Dispute.builder()
                .transaction(disputedTx1)
                .user(customer1)
                .reason("INCORRECT_AMOUNT")
                .description("I was charged R499.99 but the advertised price was R399.99")
                .status(DisputeStatus.UNDER_REVIEW)
                .build();
        dispute1 = disputeRepository.save(dispute1);

        // Create audit logs for dispute1
        AuditLog log1 = AuditLog.builder()
                .dispute(dispute1)
                .actor(customer1)
                .action("CREATED")
                .oldValue(null)
                .newValue(DisputeStatus.PENDING.name())
                .build();
        auditLogRepository.save(log1);

        AuditLog log2 = AuditLog.builder()
                .dispute(dispute1)
                .actor(admin)
                .action("STATUS_CHANGED")
                .oldValue(DisputeStatus.PENDING.name())
                .newValue(DisputeStatus.UNDER_REVIEW.name())
                .notes("Investigating with merchant")
                .build();
        auditLogRepository.save(log2);

        // Create dispute for customer2
        Transaction disputedTx2 = transactions2.get(2); // Fraudulent transaction
        Dispute dispute2 = Dispute.builder()
                .transaction(disputedTx2)
                .user(customer2)
                .reason("UNAUTHORIZED")
                .description("I did not authorize this transaction. I don't recognize this merchant.")
                .status(DisputeStatus.PENDING)
                .build();
        dispute2 = disputeRepository.save(dispute2);

        AuditLog log3 = AuditLog.builder()
                .dispute(dispute2)
                .actor(customer2)
                .action("CREATED")
                .oldValue(null)
                .newValue(DisputeStatus.PENDING.name())
                .build();
        auditLogRepository.save(log3);

        log.info("Database seeded successfully!");
        log.info("\n=== TEST ACCOUNTS ===");
        log.info("Admin: email=admin@disputehub.com, password=Admin@123");
        log.info("Customer 1: email=john.doe@example.com, password=Password@123");
        log.info("Customer 2: email=jane.smith@example.com, password=Password@123");
        log.info("====================\n");
    }
}
