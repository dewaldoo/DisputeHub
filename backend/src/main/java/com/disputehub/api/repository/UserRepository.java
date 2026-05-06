package com.disputehub.api.repository;

import com.disputehub.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity database operations.
 *
 * EXPLANATION - SPRING DATA JPA MAGIC:
 * You just declare an interface, Spring automatically implements it!
 *
 * HOW IT WORKS:
 * 1. Extend JpaRepository<Entity, IDType>
 * 2. Spring generates implementation class at runtime
 * 3. You get CRUD methods for free: save(), findById(), findAll(), delete(), etc.
 *
 * NO NEED TO WRITE:
 * ```java
 * @Repository
 * public class UserRepositoryImpl implements UserRepository {
 *     @PersistenceContext
 *     private EntityManager em;
 *
 *     public User save(User user) {
 *         em.persist(user);
 *         return user;
 *     }
 *     // ... 50 more lines of boilerplate
 * }
 * ```
 *
 * BUILT-IN METHODS (inherited from JpaRepository):
 * - save(User user) - Insert or update
 * - findById(Long id) - Find by primary key
 * - findAll() - Get all users
 * - delete(User user) - Delete user
 * - count() - Count total users
 * - existsById(Long id) - Check if exists
 *
 * CUSTOM QUERY METHODS:
 * Spring Data JPA can generate queries from method names!
 * Just follow naming conventions:
 * - findBy<FieldName> → SELECT * FROM users WHERE field_name = ?
 * - findBy<Field1>And<Field2> → WHERE field1 = ? AND field2 = ?
 * - findBy<Field>OrderBy<Field2>Desc → WHERE ... ORDER BY field2 DESC
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username.
     *
     * METHOD NAME PARSING:
     * - "find" → SELECT query
     * - "By" → WHERE clause follows
     * - "Username" → WHERE username = ?
     *
     * GENERATED SQL:
     * SELECT * FROM users WHERE username = ?
     *
     * RETURN TYPE:
     * Optional<User> - May or may not find a user
     * - If found: Optional.of(user)
     * - If not found: Optional.empty()
     * - Avoids NullPointerException, forces null checking
     *
     * USAGE:
     * ```java
     * Optional<User> userOpt = userRepository.findByUsername("john");
     * if (userOpt.isPresent()) {
     *     User user = userOpt.get();
     *     // ... use user
     * } else {
     *     throw new UserNotFoundException("User not found");
     * }
     * ```
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if username already exists.
     *
     * METHOD NAME PARSING:
     * - "exists" → Returns boolean
     * - "By" → WHERE clause
     * - "Username" → WHERE username = ?
     *
     * GENERATED SQL:
     * SELECT COUNT(*) > 0 FROM users WHERE username = ?
     *
     * USAGE:
     * Useful for registration validation:
     * ```java
     * if (userRepository.existsByUsername("john")) {
     *     throw new UsernameAlreadyExistsException();
     * }
     * ```
     */
    boolean existsByUsername(String username);

    /**
     * Find user by email address.
     *
     * METHOD NAME PARSING:
     * - "find" → SELECT query
     * - "By" → WHERE clause follows
     * - "Email" → WHERE email = ?
     *
     * GENERATED SQL:
     * SELECT * FROM users WHERE email = ?
     *
     * USAGE:
     * For email-based authentication (enterprise standard)
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists.
     *
     * GENERATED SQL:
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     *
     * USAGE:
     * Prevents duplicate email registrations
     */
    boolean existsByEmail(String email);

}
