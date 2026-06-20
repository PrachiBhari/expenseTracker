package com.fintrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for all JWT (JSON Web Token) operations:
 *   - Generating access tokens
 *   - Extracting claims (email, userId, expiry) from tokens
 *   - Validating tokens (signature + expiry check)
 *
 * WHAT IS A JWT?
 * A JWT has 3 parts separated by dots: header.payload.signature
 *
 *   Header:    { "alg": "HS256", "typ": "JWT" }   ← base64 encoded
 *   Payload:   { "sub": "user@email.com",          ← base64 encoded
 *                "userId": 1,
 *                "iat": 1718956800,   (issued at)
 *                "exp": 1718957700 }  (expires at)
 *   Signature: HMAC-SHA256(base64Header + "." + base64Payload, secretKey)
 *
 * The signature is what makes it tamper-proof. If anyone modifies the payload
 * (e.g., changes userId from 1 to 2), the signature no longer matches and
 * the token is rejected.
 *
 * LIBRARY: JJWT 0.12.x (io.jsonwebtoken)
 * The API changed significantly in 0.12.x. Key changes:
 *   Old: parseClaimsJws() / getBody() / setSigningKey()
 *   New: parseSignedClaims() / getPayload() / verifyWith()
 */
@Slf4j
@Service
public class JwtService {

    /**
     * Secret key read from application.yml → ${jwt.secret}.
     * MUST be >= 32 characters (256 bits) for HMAC-SHA256.
     * Injected from environment variable in all real environments.
     * NEVER commit the real secret to source control.
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Access token lifetime in milliseconds.
     * Default: 900,000 ms = 15 minutes.
     * Short-lived: limits damage if a token is stolen.
     */
    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // =========================================================================
    // Token Generation
    // =========================================================================

    /**
     * Generates a signed access token for the given user.
     *
     * Claims included in the payload:
     *   sub    → user's email (standard JWT subject claim)
     *   userId → user's database ID (for fast service-layer lookups without extra DB query)
     *   iat    → issued-at timestamp
     *   exp    → expiration timestamp
     */
    public String generateAccessToken(UserDetails userDetails) {
        com.fintrack.user.User user = (com.fintrack.user.User) userDetails;
        return buildToken(
                Map.of("userId", user.getId()),
                userDetails.getUsername(),
                accessTokenExpiration
        );
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // =========================================================================
    // Token Extraction
    // =========================================================================

    /**
     * Extracts the email (subject) from the token.
     * The subject is the email we set as the JWT "sub" claim on generation.
     * Used in JwtAuthFilter to identify the user.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generic claim extractor. Takes a Function that maps Claims → T.
     * Example: extractClaim(token, Claims::getExpiration) → expiry Date
     *          extractClaim(token, claims -> claims.get("userId", Long.class)) → userId
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // verify signature
                .build()
                .parseSignedClaims(token)     // parse and validate
                .getPayload();                // get the claims object
    }

    // =========================================================================
    // Token Validation
    // =========================================================================

    /**
     * Validates the token: checks signature (via parsing) and expiry.
     * Also checks that the token's subject matches the given UserDetails.
     *
     * Called in JwtAuthFilter after successfully parsing the token.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // =========================================================================
    // Signing Key
    // =========================================================================

    /**
     * Decodes the base64 secret and creates an HMAC-SHA256 SecretKey.
     *
     * Why base64?
     * The secret in application.yml is a plain string.
     * Keys.hmacShaKeyFor() expects raw bytes.
     * Decoders.BASE64.decode() converts our base64-encoded string to bytes.
     *
     * In production, generate the secret with: openssl rand -base64 64
     * This ensures you get 48 bytes of random data (> 256 bits).
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
