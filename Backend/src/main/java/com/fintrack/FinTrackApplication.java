package com.fintrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration      : this class can define @Bean methods
 *   - @EnableAutoConfiguration : Spring Boot auto-configures beans based on classpath
 *   - @ComponentScan      : scans com.fintrack.* for @Component, @Service, @Repository, etc.
 *
 * @EnableScheduling activates Spring's @Scheduled task execution.
 * Without this annotation, any @Scheduled method is silently ignored.
 * We use it for the nightly expired refresh-token cleanup job.
 *
 * JPA Auditing (@EnableJpaAuditing) lives in its own JpaAuditingConfig class
 * to keep this entry point clean and to avoid test issues with auditing.
 */
@SpringBootApplication
@EnableScheduling
public class FinTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinTrackApplication.class, args);
    }
}
