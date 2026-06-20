package com.fintrack.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled maintenance task that removes expired refresh tokens from the database.
 *
 * WHY IS THIS NEEDED?
 * Every login and every token refresh creates a new row in the refresh_tokens table.
 * Tokens expire after 7 days, but they are not automatically deleted — they just sit
 * in the table forever, taking up space and adding noise.
 *
 * Without this job, a user who logs in once a day would accumulate ~365 expired rows
 * per year. Over thousands of users, this becomes a real database bloat problem.
 *
 * HOW DOES @Scheduled WORK?
 * Spring's task execution infrastructure (activated by @EnableScheduling on
 * FinTrackApplication) scans for @Scheduled methods at startup.
 * It runs them in a background thread pool, separate from the HTTP request threads.
 *
 * cron = "0 0 3 * * *"  — standard 6-field cron expression:
 *   second(0) minute(0) hour(3) day-of-month(*) month(*) day-of-week(*)
 *   = "at 03:00:00, every day, every month, any weekday"
 *
 * WHY 3 AM?
 * Off-peak hours → minimal DB load during the DELETE operation.
 * The time zone is the JVM's default; for predictability set a specific zone:
 *   @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
 *
 * TRANSACTION:
 * @Transactional is required because @Modifying repository methods must run inside
 * a transaction. The @Transactional here opens one for the duration of this method.
 *
 * INTERVIEW: "What's the difference between @Scheduled and a message queue for this?"
 *   @Scheduled is simple and works for single-instance deployments.
 *   If the app runs on multiple pods (Kubernetes), ALL pods will run the cleanup at 3 AM,
 *   causing duplicate DELETE attempts. This is harmless (second DELETE finds 0 rows),
 *   but wasteful. A proper solution for multi-instance deployments is:
 *     - ShedLock: a distributed lock using the DB, so only one pod runs the job.
 *     - A Kubernetes CronJob: one pod is spun up just for the cleanup task.
 *   For a personal finance MVP with one instance, @Scheduled is perfectly adequate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void deleteExpiredRefreshTokens() {
        log.info("Refresh token cleanup job started");
        int deleted = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
        log.info("Refresh token cleanup completed: {} expired token(s) removed", deleted);
    }
}
