package com.fintrack.user;

/**
 * User roles for Spring Security authorization.
 *
 * MVP only uses USER. ADMIN is defined now so the DB column is flexible.
 * Spring Security expects authorities prefixed with "ROLE_", so in code
 * we call: new SimpleGrantedAuthority("ROLE_" + role.name())
 *   → "ROLE_USER" or "ROLE_ADMIN"
 *
 * Stored as VARCHAR in the DB via @Enumerated(EnumType.STRING).
 * EnumType.STRING stores "USER" / "ADMIN" — readable in the DB.
 * EnumType.ORDINAL (default) stores 0/1 — brittle if enum order changes.
 */
public enum Role {
    USER,
    ADMIN
}
