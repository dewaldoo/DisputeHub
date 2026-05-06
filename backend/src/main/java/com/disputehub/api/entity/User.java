package com.disputehub.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User entity representing both customers and admin users.
 *
 * EXPLANATION OF ANNOTATIONS:
 * - @Entity: Marks this class as a JPA entity (maps to database table)
 * - @Table: Specifies table name (default would be "user" which is a reserved word in some databases)
 * - @Data: Lombok annotation that generates getters, setters, toString, equals, hashCode
 * - @Builder: Lombok annotation that implements Builder pattern for object creation
 * - @NoArgsConstructor: Generates no-args constructor (required by JPA)
 * - @AllArgsConstructor: Generates constructor with all fields (used by Builder)
 *
 * IMPLEMENTS UserDetails:
 * This is a Spring Security interface that provides user information to the framework.
 * Required methods: getUsername(), getPassword(), getAuthorities(), isAccountNonExpired(), etc.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /**
     * PRIMARY KEY
     * @Id: Marks this field as the primary key
     * @GeneratedValue: Auto-generates values for this field
     * GenerationType.IDENTITY: Uses database auto-increment feature
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UNIQUE CONSTRAINT
     * @Column: Configures column properties
     * nullable=false: Creates NOT NULL constraint in database
     * unique=true: Creates UNIQUE constraint (no two users can have same username)
     */
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Password field (will be BCrypt hashed, never stored as plain text)
     */
    @Column(nullable = false)
    private String password;

    /**
     * ENUM TYPE
     * @Enumerated: Maps Java enum to database column
     * EnumType.STRING: Stores enum name as string ("CUSTOMER" or "ADMIN")
     * Alternative: EnumType.ORDINAL stores enum position as integer (not recommended - fragile)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * User's full name (optional)
     */
    private String fullName;

    /**
     * Email address (optional, could be used for notifications)
     */
    private String email;

    /**
     * Timestamp of when user was created
     * @Column: updatable=false means this field can't be changed after initial insert
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * ONE-TO-MANY RELATIONSHIP
     * One user has many transactions
     *
     * @OneToMany: Defines one-to-many relationship
     * mappedBy="user": Indicates that Transaction entity owns this relationship
     *                  (Transaction has a "user" field with @ManyToOne)
     * cascade=CascadeType.ALL: Operations on User cascade to Transactions
     *                          (e.g., deleting user deletes their transactions)
     * orphanRemoval=true: If transaction is removed from collection, delete it from database
     *
     * fetch=FetchType.LAZY: Don't load transactions until explicitly accessed
     *                       (performance optimization - avoids N+1 query problem)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions;

    /**
     * ONE-TO-MANY RELATIONSHIP
     * One user can create many disputes
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Dispute> disputes;

    /**
     * JPA LIFECYCLE CALLBACK
     * @PrePersist: Executed before entity is inserted into database
     * Automatically sets createdAt timestamp
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ========== SPRING SECURITY USERDETAILS INTERFACE METHODS ==========

    /**
     * Returns user's authorities (roles/permissions).
     * Spring Security uses this for authorization checks.
     *
     * EXPLANATION:
     * - GrantedAuthority: Interface representing a permission
     * - SimpleGrantedAuthority: Basic implementation that wraps a string
     * - "ROLE_" prefix: Spring Security convention for roles
     * - This allows @PreAuthorize("hasRole('CUSTOMER')") to work
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns username for authentication.
     * We're using username field, but could be email or any unique identifier.
     */
    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Returns password for authentication.
     * This will be BCrypt hashed value like: $2a$10$XptfskVJplz6K6c2P5xPr.5dkXYgHECqhxPX8NvqC7bXXQ3z1VXNK
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Account expiration check.
     * Could be enhanced to check an 'expiryDate' field.
     * For now, accounts never expire.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account locking check.
     * Could be enhanced to implement account locking after X failed login attempts.
     * For now, accounts are never locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credential expiration check.
     * Could be enhanced to force password changes every N days.
     * For now, credentials never expire.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Account enabled check.
     * Could be enhanced to add an 'enabled' boolean field.
     * Useful for soft-deleting users or implementing email verification.
     * For now, all accounts are enabled.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
