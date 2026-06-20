package com.fintrack.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for RefreshToken entity.
 *
 * @Modifying on delete methods:
 *   Spring Data JPA requires @Modifying on any query that changes data
 *   (INSERT, UPDATE, DELETE) defined via @Query or derived methods.
 *   Without it, Spring assumes the method is a SELECT and throws an error.
 *
 * @Transactional is NOT added here — it's added at the service layer.
 *   Repositories should not manage transaction boundaries; services do.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Look up a refresh token by its string value.
     * Called on every POST /auth/refresh to validate the incoming token.
     * Returns empty if the token doesn't exist (already used, revoked, or invalid).
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Delete all refresh tokens for a user — used on logout.
     * Ensures all sessions are terminated, not just the current one.
     *
     * @Modifying + derived delete method: Spring Data generates
     *   DELETE FROM refresh_tokens WHERE user_id = ?
     */
    @Modifying
    void deleteByUserId(Long userId);

    /**
     * Delete a specific token by its value — used during token rotation.
     * After validating a refresh token, we delete it and issue a new one.
     */
    @Modifying
    void deleteByToken(String token);

    /**
     * Bulk delete all tokens whose expiry timestamp is before the given instant.
     * Called nightly by RefreshTokenCleanupTask to prevent unbounded table growth.
     *
     * Returns the number of rows deleted — logged by the cleanup task.
     *
     * INTERVIEW: "Why return int instead of void?"
     *   So the caller can log how many rows were deleted. Knowing "0 deleted" vs
     *   "1,200 deleted" is operationally useful for detecting unusual activity
     *   (e.g., mass token expiry after a secret rotation).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteAllExpiredBefore(@Param("now") Instant now);
}
