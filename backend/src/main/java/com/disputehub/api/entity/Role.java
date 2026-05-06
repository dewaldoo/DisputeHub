package com.disputehub.api.entity;

/**
 * User roles in the system.
 *
 * EXPLANATION:
 * Enum is a special Java type for defining a fixed set of constants.
 * Much safer than using strings or integers because:
 * - Type-safe: Can't assign invalid value
 * - Compile-time checking: Typos caught at compile time
 * - Self-documenting: Clear what values are valid
 *
 * EXAMPLE:
 * User user = new User();
 * user.setRole(Role.CUSTOMER);  // ✓ Valid
 * user.setRole("CUSTOMER");     // ✗ Compile error (type mismatch)
 * user.setRole("CUSTMER");      // ✗ Compile error (typo caught)
 *
 * IN DATABASE:
 * Because we use @Enumerated(EnumType.STRING) in User entity,
 * this will be stored as "CUSTOMER" or "ADMIN" string in the database.
 */
public enum Role {
    /**
     * Regular customer who can:
     * - View their own transactions
     * - Create disputes on their transactions
     * - Track their dispute status
     */
    CUSTOMER,

    /**
     * Admin user who can:
     * - View all disputes across all customers
     * - Change dispute statuses
     * - Access admin dashboard
     * - View audit logs
     */
    ADMIN
}
