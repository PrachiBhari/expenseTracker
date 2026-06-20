package com.fintrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing.
 *
 * Why a separate class instead of putting @EnableJpaAuditing on FinTrackApplication?
 * When Spring Boot tests load a partial context (e.g., @DataJpaTest or @WebMvcTest),
 * they don't load @SpringBootApplication. Putting @EnableJpaAuditing on the main class
 * causes test failures because the auditing infrastructure isn't available in slice tests.
 * A separate @Configuration class gives Spring more fine-grained control.
 *
 * What this enables:
 *   @CreatedDate  → auto-populates 'created_at' on entity persist
 *   @LastModifiedDate → auto-populates 'updated_at' on entity persist + update
 * Both fields use Instant (UTC) as the Java type.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // No beans needed — @EnableJpaAuditing registers the AuditingEntityListener automatically.
}
