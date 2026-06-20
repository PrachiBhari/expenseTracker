package com.fintrack.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Shared base class for all audited entities.
 *
 * @MappedSuperclass — NOT an entity itself. JPA maps its fields into the
 *   child entity's table. Think of it as a "mixin" for created_at/updated_at.
 *
 * @EntityListeners(AuditingEntityListener.class) — Registers the Spring Data
 *   listener that intercepts @PrePersist and @PreUpdate lifecycle events
 *   and auto-fills the annotated fields.
 *
 * Why Instant instead of LocalDateTime?
 *   Instant = a single moment in UTC (no timezone ambiguity).
 *   LocalDateTime has no timezone info — if the JVM timezone ever changes,
 *   your stored times would be misread. Instant is always UTC, always safe.
 *
 * updatable = false on created_at: once written on INSERT, never changed on UPDATE.
 *
 * @SuperBuilder — required so subclass builders include createdAt/updatedAt.
 *   Lombok's @Builder on a subclass only generates builder methods for fields
 *   declared in that subclass, not inherited fields. MapStruct validates
 *   @Mapping(target = "createdAt", ignore = true) by checking the builder —
 *   without @SuperBuilder on the parent, this check fails at compile time.
 *
 * @NoArgsConstructor(force = true) — JPA requires a no-args constructor.
 *   @SuperBuilder adds its own protected all-args constructor, which removes
 *   Java's default no-args. Subclass @NoArgsConstructor calls super() so we
 *   need this protected no-args to be present on the abstract parent.
 */
@Getter
@SuperBuilder
@NoArgsConstructor(force = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
