package com.fintrack.security;

import com.fintrack.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA Entity: maps to the 'refresh_tokens' table.
 *
 * Does NOT extend BaseEntity because:
 *   - Refresh tokens are never "updated" — they are created and then deleted.
 *   - We don't need updated_at.
 *   - created_at is set manually in the service, not via JPA auditing.
 *
 * Why store refresh tokens in the database?
 *   Unlike JWTs (stateless, verified by signature), refresh tokens MUST be
 *   stored server-side so they can be:
 *   1. Revoked immediately (logout, security incident)
 *   2. Validated (check if expired, check if it was already used)
 *   3. Rotated (issue a new one, invalidate the old one)
 *
 * Token value:
 *   A randomly generated UUID — unpredictable, 36-char string.
 *   NOT a JWT — it's just a random lookup key.
 *   The DB has a UNIQUE constraint on it.
 *
 * Token rotation strategy (implemented in AuthService):
 *   On every /auth/refresh call:
 *     1. Validate the incoming refresh token (exists in DB? not expired?)
 *     2. Delete the old token from DB
 *     3. Issue a new access token + new refresh token
 *   This means each refresh token can only be used ONCE.
 *   If a stolen token is used after the legitimate user has refreshed, it's rejected.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user this token was issued to.
     * ON DELETE CASCADE in DB: deleting user also deletes all their refresh tokens.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The random UUID token string stored in the client (localStorage).
     * Unique constraint ensures no collisions (UUID collision probability: negligible).
     */
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    /**
     * When this token expires. Checked on every /auth/refresh call.
     * Tokens past this time are rejected even if they exist in the DB.
     * Expired tokens are cleaned up lazily (on use) or by a scheduled job in future.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * When this token was created. Set manually in the service.
     * Useful for auditing: "this token was issued 3 days ago".
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
