package com.fintrack.user;

import com.fintrack.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * JPA Entity: maps to the 'users' table.
 *
 * Implements UserDetails so Spring Security can use it directly as the
 * authentication principal — no need for a separate UserPrincipal wrapper.
 *
 * Trade-off: implementing UserDetails on the entity couples the domain
 * model to Spring Security. A cleaner (but more complex) alternative is
 * a separate UserPrincipal wrapper. For MVP + interview clarity, direct
 * implementation is the better choice.
 *
 * Lombok annotations used:
 *   @Getter / @Setter   — generate all getters and setters
 *   @Builder            — builder pattern: User.builder().email("...").build()
 *   @NoArgsConstructor  — required by JPA (JPA instantiates entities via no-arg constructor)
 *   @AllArgsConstructor — required by @Builder when @NoArgsConstructor is also present
 *
 * Why NOT @Data on JPA entities?
 *   @Data generates equals() + hashCode() using ALL fields.
 *   JPA entities are often in a "detached" state with lazily loaded fields.
 *   Calling equals/hashCode on a detached entity can trigger
 *   LazyInitializationException or produce inconsistent results.
 *   We let equals/hashCode default to Object identity (reference equality),
 *   which is safe for entities managed by the JPA session.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email is the user's unique login identifier.
     * Unique constraint is also enforced at DB level (uq_users_email in V1__init.sql).
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * BCrypt hash of the user's password.
     * Column is named password_hash — we NEVER store plaintext.
     * The field name is passwordHash to be explicit in Java code.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 120)
    private String fullName;

    /**
     * EnumType.STRING stores the enum name ("USER") in the DB.
     * Default value set here matches the DB DEFAULT 'USER'.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // =========================================================================
    // UserDetails interface implementation
    // Spring Security calls these methods to make authorization decisions.
    // =========================================================================

    /**
     * Returns the user's granted authorities (roles).
     * Spring Security requires the "ROLE_" prefix for role-based checks
     * like hasRole("USER") in SecurityConfig.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security calls getPassword() to compare with the provided password.
     * We return the BCrypt hash — Spring's PasswordEncoder handles the comparison.
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Spring Security uses getUsername() as the unique principal identifier.
     * We use email as the username (not a separate username field).
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * These three methods return true for MVP simplicity.
     * In production: isAccountNonLocked() could check a 'locked_at' field
     * after too many failed login attempts (brute-force protection).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Returns our 'enabled' field — allows soft-disabling accounts
     * without deleting data. Spring Security rejects disabled users at login.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
