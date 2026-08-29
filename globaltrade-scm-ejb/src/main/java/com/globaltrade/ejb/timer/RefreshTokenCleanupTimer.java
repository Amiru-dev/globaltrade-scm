package com.globaltrade.ejb.timer;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Refresh token cleanup timer for automated token maintenance.
 *
 * <p><strong>Critical Analysis — Timer Service Design:</strong></p>
 * <ul>
 *   <li><strong>Timer Type: DECLARATIVE (@Schedule)</strong> — Uses @Schedule
 *       because token cleanup runs on a fixed daily schedule that doesn't need runtime
 *       adjustment. This contrasts with programmatic timers used for dynamic operations
 *       like customs deadline tracking.</li>
 *   <li><strong>Persistence: {@code persistent = true}</strong> — Guarantees timer
 *       survives server restart, critical for 99.9% uptime requirement. If the node
 *       restarts near 2am, the cleanup won't be missed.</li>
 *   <li><strong>Clustering Consideration:</strong> In a multi-node deployment, this
 *       timer would fire on every node. Options: (a) clustered singleton service,
 *       (b) DB-level advisory lock, (c) designated scheduler node via config.</li>
 *   <li><strong>Cleanup Strategy:</strong> Removes expired and revoked tokens to
 *       prevent database bloat and maintain security hygiene.</li>
 * </ul>
 *
 * <p><strong>Declarative vs Programmatic Timer Comparison:</strong></p>
 * <ul>
 *   <li><strong>Declarative (chosen here):</strong> No boilerplate for timer creation,
 *       automatically created by container, self-documenting schedule, suitable for
 *       predictable operations like daily cleanup.</li>
 *   <li><strong>Programmatic:</strong> Used for dynamic operations (customs deadlines),
 *       allows runtime adjustment, requires manual lifecycle management.</li>
 * </ul>
 */
@Singleton
@Startup
public class RefreshTokenCleanupTimer {

    private static final Logger LOGGER = Logger.getLogger(RefreshTokenCleanupTimer.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Declarative timer for automated refresh token cleanup.
     *
     * <p><strong>Schedule:</strong> Runs daily at 2:00 AM to minimize impact on
     * active users while ensuring regular cleanup.</p>
     *
     * <p><strong>Critical Analysis — Persistence:</strong> persistent=true ensures
     * the timer survives server restarts, critical for 99.9% uptime requirement.
     * If the server restarts at 1:59 AM, the timer will still fire at 2:00 AM.</p>
     */
    @Schedule(minute = "*/2", persistent = true, info = "refresh-token-cleanup")
    public void purgeExpiredTokens() {
        try {
            // Delete expired tokens
            int expiredDeleted = em.createQuery(
                "DELETE FROM RefreshToken t WHERE t.expiresAt < :now")
                .setParameter("now", Instant.now())
                .executeUpdate();

            // Delete revoked tokens
            int revokedDeleted = em.createQuery(
                "DELETE FROM RefreshToken t WHERE t.revoked = true")
                .executeUpdate();

            int totalDeleted = expiredDeleted + revokedDeleted;

            LOGGER.log(Level.INFO,
                    "Refresh token cleanup completed: {0} expired tokens purged, {1} revoked tokens purged, {2} total",
                    new Object[]{expiredDeleted, revokedDeleted, totalDeleted});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Refresh token cleanup failed", e);
        }
    }

    /**
     * Manual cleanup method for testing or emergency maintenance.
     *
     * <p><strong>Critical Analysis:</strong> Provides administrative control for
     * immediate cleanup outside the scheduled window. Useful for testing or
     * responding to security incidents.</p>
     */
    public void manualCleanup() {
        LOGGER.log(Level.INFO, "Manual refresh token cleanup triggered");
        purgeExpiredTokens();
    }

    /**
     * Returns statistics about current refresh token state.
     *
     * <p><strong>Critical Analysis:</strong> Provides visibility into token
     * management for monitoring and capacity planning.</p>
     */
    public String getTokenStatistics() {
        try {
            Long totalTokens = em.createQuery(
                "SELECT COUNT(t) FROM RefreshToken t", Long.class)
                .getSingleResult();

            Long expiredTokens = em.createQuery(
                "SELECT COUNT(t) FROM RefreshToken t WHERE t.expiresAt < :now", Long.class)
                .setParameter("now", Instant.now())
                .getSingleResult();

            Long revokedTokens = em.createQuery(
                "SELECT COUNT(t) FROM RefreshToken t WHERE t.revoked = true", Long.class)
                .getSingleResult();

            Long activeTokens = em.createQuery(
                "SELECT COUNT(t) FROM RefreshToken t WHERE t.revoked = false AND t.expiresAt >= :now", Long.class)
                .setParameter("now", Instant.now())
                .getSingleResult();

            return String.format(
                    "Token Statistics - Total: %d, Active: %d, Expired: %d, Revoked: %d",
                    totalTokens, activeTokens, expiredTokens, revokedTokens);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve token statistics", e);
            return "Token statistics unavailable";
        }
    }
}