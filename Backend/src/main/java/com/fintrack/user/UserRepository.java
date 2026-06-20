package com.fintrack.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 *
 * Extends JpaRepository<User, Long>:
 *   - User     = the entity type
 *   - Long     = the type of the primary key (id field)
 *
 * JpaRepository provides out of the box:
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), ...
 *
 * Spring Data generates the implementation at startup by reading method names.
 * No SQL or JPQL needed for simple queries.
 *
 * Derived query method naming rules:
 *   findBy{FieldName}      → SELECT ... WHERE field_name = ?
 *   existsBy{FieldName}    → SELECT COUNT(*) > 0 WHERE field_name = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Used during login: load the user record to verify their password.
     * Also used by CustomUserDetailsService to load UserDetails for JWT validation.
     * Returns Optional so we can handle "user not found" gracefully.
     */
    Optional<User> findByEmail(String email);

    /**
     * Used during registration: check if email is already taken
     * before trying to save (to give a clear error instead of a DB exception).
     */
    boolean existsByEmail(String email);
}
