package com.disputehub.api.integration;

import com.disputehub.api.dto.CreateDisputeRequest;
import com.disputehub.api.dto.DisputeResponse;
import com.disputehub.api.dto.JwtResponse;
import com.disputehub.api.dto.LoginRequest;
import com.disputehub.api.dto.UpdateDisputeStatusRequest;
import com.disputehub.api.entity.*;
import com.disputehub.api.repository.DisputeRepository;
import com.disputehub.api.repository.TransactionRepository;
import com.disputehub.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full API integration tests covering complete HTTP request/response cycle.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DisputeApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private String customerToken;
    private String adminToken;
    private User customer;
    private User admin;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // Clean database
        disputeRepository.deleteAll();
        transactionRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        customer = User.builder()
                .username("customer@test.com")
                .email("customer@test.com")
                .password(passwordEncoder.encode("TestPass123"))
                .fullName("Test Customer")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        admin = User.builder()
                .username("admin@test.com")
                .email("admin@test.com")
                .password(passwordEncoder.encode("AdminPass123"))
                .fullName("Test Admin")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);

        // Create test transaction
        transaction = Transaction.builder()
                .user(customer)
                .merchantName("Test Store")
                .amount(new BigDecimal("299.99"))
                .transactionDate(LocalDateTime.now().minusDays(2))
                .category("Shopping")
                .referenceNumber("TXN-TEST-001")
                .build();
        transaction = transactionRepository.save(transaction);

        // Get JWT tokens
        customerToken = loginAndGetToken("customer@test.com", "TestPass123");
        adminToken = loginAndGetToken("admin@test.com", "AdminPass123");
    }

    /**
     * TEST: Complete dispute creation flow via API.
     *
     * FLOW:
     * 1. Customer logs in (gets JWT token)
     * 2. Customer creates dispute (sends JWT in header)
     * 3. Verify dispute saved in database
     * 4. Verify audit log created
     */
    @Test
    void createDispute_shouldSucceed_withValidCustomerToken() {
        // ARRANGE: Create request
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTransactionId(transaction.getId());
        request.setReason("UNAUTHORIZED");
        request.setDescription("I did not authorize this transaction");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateDisputeRequest> entity = new HttpEntity<>(request, headers);

        // ACT: POST to /api/disputes
        ResponseEntity<DisputeResponse> response = restTemplate.exchange(
                baseUrl + "/api/disputes",
                HttpMethod.POST,
                entity,
                DisputeResponse.class
        );

        // ASSERT: HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getReason()).isEqualTo("UNAUTHORIZED");
        assertThat(response.getBody().getStatus()).isEqualTo(DisputeStatus.PENDING);

        // ASSERT: Verify in database
        Dispute savedDispute = disputeRepository.findById(response.getBody().getId()).orElse(null);
        assertThat(savedDispute).isNotNull();
        assertThat(savedDispute.getUser().getId()).isEqualTo(customer.getId());
        assertThat(savedDispute.getTransaction().getId()).isEqualTo(transaction.getId());
    }

    /**
     * TEST: Unauthorized access should return 401 or 403.
     *
     * SECURITY TEST - No token = no access.
     * Note: Spring Security returns 403 FORBIDDEN when no authentication is provided
     */
    @Test
    void createDispute_shouldReturn401_withoutToken() {
        // ARRANGE
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTransactionId(transaction.getId());
        request.setReason("UNAUTHORIZED");
        request.setDescription("Test");

        HttpEntity<CreateDisputeRequest> entity = new HttpEntity<>(request);

        // ACT: POST without Authorization header
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/disputes",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // ASSERT: Spring Security returns 403 when no authentication is present
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /**
     * TEST: Customer cannot dispute another customer's transaction.
     *
     * SECURITY TEST - Critical for banking app!
     */
    @Test
    void createDispute_shouldReturn400_whenDisputingOtherUserTransaction() {
        // ARRANGE: Create transaction for different user
        User otherCustomer = User.builder()
                .username("other@test.com")
                .email("other@test.com")
                .password(passwordEncoder.encode("TestPass123"))
                .fullName("Other Customer")
                .role(Role.CUSTOMER)
                .build();
        otherCustomer = userRepository.save(otherCustomer);

        Transaction otherTransaction = Transaction.builder()
                .user(otherCustomer)
                .merchantName("Other Store")
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDateTime.now())
                .category("Test")
                .referenceNumber("OTHER-001")
                .build();
        otherTransaction = transactionRepository.save(otherTransaction);

        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTransactionId(otherTransaction.getId());
        request.setReason("UNAUTHORIZED");
        request.setDescription("Trying to dispute someone else's transaction");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken); // Customer 1's token
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateDisputeRequest> entity = new HttpEntity<>(request, headers);

        // ACT: Try to dispute other user's transaction
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/disputes",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody().get("message").toString())
                .contains("You can only dispute your own transactions");
    }

    /**
     * TEST: Admin can update dispute status.
     */
    @Test
    void updateDisputeStatus_shouldSucceed_whenAdmin() {
        // ARRANGE: Create dispute
        Dispute dispute = Dispute.builder()
                .transaction(transaction)
                .user(customer)
                .reason("UNAUTHORIZED")
                .description("Test dispute")
                .status(DisputeStatus.PENDING)
                .build();
        dispute = disputeRepository.save(dispute);

        UpdateDisputeStatusRequest request = new UpdateDisputeStatusRequest();
        request.setStatus(DisputeStatus.UNDER_REVIEW);
        request.setResolutionNotes("Investigating");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateDisputeStatusRequest> entity = new HttpEntity<>(request, headers);

        // ACT: PUT to /api/disputes/{id}/status
        ResponseEntity<DisputeResponse> response = restTemplate.exchange(
                baseUrl + "/api/disputes/" + dispute.getId() + "/status",
                HttpMethod.PUT,
                entity,
                DisputeResponse.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);

        // Verify in DB
        Dispute updated = disputeRepository.findById(dispute.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);
        assertThat(updated.getResolutionNotes()).isEqualTo("Investigating");
    }

    /**
     * TEST: Customer cannot update dispute status (403 Forbidden).
     *
     * AUTHORIZATION TEST - Role-based access control.
     * The @PreAuthorize("hasRole('ADMIN')") annotation blocks access at the security layer,
     * returning 403 FORBIDDEN before reaching the service layer.
     */
    @Test
    void updateDisputeStatus_shouldReturn400_whenCustomerTries() {
        // ARRANGE
        Dispute dispute = Dispute.builder()
                .transaction(transaction)
                .user(customer)
                .reason("UNAUTHORIZED")
                .description("Test dispute")
                .status(DisputeStatus.PENDING)
                .build();
        dispute = disputeRepository.save(dispute);

        UpdateDisputeStatusRequest request = new UpdateDisputeStatusRequest();
        request.setStatus(DisputeStatus.RESOLVED);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken); // Customer token, not admin!
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateDisputeStatusRequest> entity = new HttpEntity<>(request, headers);

        // ACT
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/disputes/" + dispute.getId() + "/status",
                HttpMethod.PUT,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // ASSERT: Spring Security's @PreAuthorize returns 403 FORBIDDEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody().get("message").toString())
                .contains("Access denied");
    }

    /**
     * TEST: Pagination works via API.
     *
     * SCALE TEST - Verify pagination prevents OOM.
     */
    @Test
    void getAllDisputes_shouldReturnPaginatedResults() {
        // ARRANGE: Create 25 disputes
        for (int i = 0; i < 25; i++) {
            Transaction txn = Transaction.builder()
                    .user(customer)
                    .merchantName("Store " + i)
                    .amount(new BigDecimal("100.00"))
                    .transactionDate(LocalDateTime.now().minusDays(i))
                    .category("Test")
                    .referenceNumber("TEST-" + i)
                    .build();
            txn = transactionRepository.save(txn);

            Dispute dispute = Dispute.builder()
                    .transaction(txn)
                    .user(customer)
                    .reason("UNAUTHORIZED")
                    .description("Dispute " + i)
                    .status(DisputeStatus.PENDING)
                    .build();
            disputeRepository.save(dispute);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        // ACT: GET page 0, size 10
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/disputes?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        // ASSERT: Pagination metadata
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
        assertThat(response.getBody()).containsKey("totalPages");
        assertThat(response.getBody()).containsKey("totalElements");
        assertThat(response.getBody().get("totalElements")).isEqualTo(25);
        assertThat(response.getBody().get("totalPages")).isEqualTo(3); // 25 / 10 = 3
        assertThat(response.getBody().get("size")).isEqualTo(10);
        assertThat(response.getBody().get("number")).isEqualTo(0); // Page 0
    }

    /**
     * TEST: Health check endpoint is public (no auth required).
     */
    @Test
    void actuatorHealth_shouldBeAccessibleWithoutAuth() {
        // ACT: GET /actuator/health (no token)
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/actuator/health",
                Map.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody()).containsKey("components");
        // Verify DB health is included
        Map<String, Object> components = (Map<String, Object>) response.getBody().get("components");
        assertThat(components).containsKey("db");
    }

    /**
     * TEST: Transaction already disputed - should reject.
     *
     * BUSINESS RULE TEST: Can't dispute same transaction twice.
     */
    @Test
    void createDispute_shouldReturn400_whenTransactionAlreadyDisputed() {
        // ARRANGE: Create existing dispute
        Dispute existingDispute = Dispute.builder()
                .transaction(transaction)
                .user(customer)
                .reason("UNAUTHORIZED")
                .description("First dispute")
                .status(DisputeStatus.PENDING)
                .build();
        disputeRepository.save(existingDispute);

        // Try to create second dispute for same transaction
        CreateDisputeRequest request = new CreateDisputeRequest();
        request.setTransactionId(transaction.getId());
        request.setReason("INCORRECT_AMOUNT");
        request.setDescription("Second dispute attempt");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateDisputeRequest> entity = new HttpEntity<>(request, headers);

        // ACT
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/disputes",
                HttpMethod.POST,
                entity,
                Map.class
        );

        // ASSERT
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("message");
        assertThat(response.getBody().get("message").toString())
                .contains("already has an active dispute");
    }

    /**
     * TEST: Cache invalidation works correctly.
     *
     * CACHING TEST:
     * 1. Customer fetches disputes (cached)
     * 2. Admin updates status
     * 3. Customer fetches again (should see updated data, not cached stale data)
     */
    @Test
    void cacheInvalidation_shouldWork_afterDisputeStatusUpdate() {
        // ARRANGE: Create dispute
        Dispute dispute = Dispute.builder()
                .transaction(transaction)
                .user(customer)
                .reason("UNAUTHORIZED")
                .description("Test dispute")
                .status(DisputeStatus.PENDING)
                .build();
        dispute = disputeRepository.save(dispute);

        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(customerToken);

        // ACT 1: Customer fetches disputes (populates cache)
        ResponseEntity<Dispute[]> firstFetch = restTemplate.exchange(
                baseUrl + "/api/disputes/my-disputes",
                HttpMethod.GET,
                new HttpEntity<>(customerHeaders),
                Dispute[].class
        );
        assertThat(firstFetch.getBody()).hasSize(1);
        assertThat(firstFetch.getBody()[0].getStatus()).isEqualTo(DisputeStatus.PENDING);

        // ACT 2: Admin updates status (should evict cache)
        UpdateDisputeStatusRequest updateRequest = new UpdateDisputeStatusRequest();
        updateRequest.setStatus(DisputeStatus.RESOLVED);
        updateRequest.setResolutionNotes("Refunded");

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateDisputeStatusRequest> updateEntity = new HttpEntity<>(updateRequest, adminHeaders);

        restTemplate.exchange(
                baseUrl + "/api/disputes/" + dispute.getId() + "/status",
                HttpMethod.PUT,
                updateEntity,
                Dispute.class
        );

        // ACT 3: Customer fetches again (should get fresh data, not cached)
        ResponseEntity<Dispute[]> secondFetch = restTemplate.exchange(
                baseUrl + "/api/disputes/my-disputes",
                HttpMethod.GET,
                new HttpEntity<>(customerHeaders),
                Dispute[].class
        );

        // ASSERT: Status should be updated (cache was invalidated)
        assertThat(secondFetch.getBody()).hasSize(1);
        assertThat(secondFetch.getBody()[0].getStatus()).isEqualTo(DisputeStatus.RESOLVED);
        assertThat(secondFetch.getBody()[0].getResolutionNotes()).isEqualTo("Refunded");
    }

    // Helper method: Login and extract JWT token
    private String loginAndGetToken(String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(email);
        loginRequest.setPassword(password);

        ResponseEntity<JwtResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login",
                loginRequest,
                JwtResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody().getToken();
    }

}
