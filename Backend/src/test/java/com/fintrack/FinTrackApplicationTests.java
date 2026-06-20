package com.fintrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * Uses the 'test' profile so H2 is used instead of PostgreSQL.
 *
 * If this test fails, something is wrong with configuration or wiring.
 * It is the fastest feedback loop for broken Spring beans.
 */
@SpringBootTest
@ActiveProfiles("test")
class FinTrackApplicationTests {

    @Test
    void contextLoads() {
        // If the context fails to load, Spring throws an exception and this test fails.
        // No assertions needed — the test is the context loading itself.
    }
}
