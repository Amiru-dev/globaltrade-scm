package com.globaltrade.ejb.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Singleton;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Session privacy and management service.
 *
 * <p><strong>Critical Analysis — Session Security Measures:</strong></p>
 * <ul>
 *   <li><strong>Session timeout:</strong> 30 minutes for web users (configurable).
 *       Prevents session hijacking from abandoned browser sessions.</li>
 *   <li><strong>Concurrent session limiting:</strong> Users can only have one
 *       active session at a time. Previous sessions are invalidated on new login.</li>
 *   <li><strong>Session tracking:</strong> All active sessions are monitored
 *       for security oversight and anomaly detection.</li>
 *   <li><strong>HTTPS enforcement:</strong> Configured at the web layer (web.xml)
 *       to ensure session tokens are never transmitted in cleartext.</li>
 * </ul>
 */
@Singleton
@RolesAllowed({"SYSTEM_ADMIN"})
public class SessionManager implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private static final int MAX_CONCURRENT_SESSIONS_PER_USER = 1;
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    // Active session tracking: username → SessionInfo
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    /**
     * Registers a new user session. Invalidates existing sessions if limit exceeded.
     */
    public void registerSession(String username, String sessionId) {
        SessionInfo existing = activeSessions.get(username);

        if (existing != null) {
            LOGGER.log(Level.WARNING,
                    "SESSION SECURITY: User {0} already has an active session ({1}). " +
                            "Invalidating previous session.",
                    new Object[]{username, existing.sessionId});
        }

        SessionInfo newSession = new SessionInfo(sessionId, username, LocalDateTime.now());
        activeSessions.put(username, newSession);

        LOGGER.log(Level.INFO, "Session registered: user={0}, session={1}",
                new Object[]{username, sessionId});
    }

    /**
     * Invalidates a user's session.
     */
    public void invalidateSession(String username) {
        SessionInfo removed = activeSessions.remove(username);
        if (removed != null) {
            LOGGER.log(Level.INFO, "Session invalidated: user={0}, duration={1}s",
                    new Object[]{username, removed.getDurationSeconds()});
        }
    }

    /**
     * Checks if a session is valid and not expired.
     */
    public boolean isSessionValid(String username, String sessionId) {
        SessionInfo session = activeSessions.get(username);
        if (session == null) return false;
        if (!session.sessionId.equals(sessionId)) return false;

        // Check timeout
        LocalDateTime expiryTime = session.createdAt.plusMinutes(SESSION_TIMEOUT_MINUTES);
        if (LocalDateTime.now().isAfter(expiryTime)) {
            LOGGER.log(Level.WARNING,
                    "SESSION EXPIRED: user={0}, created={1}, timeout={2}min",
                    new Object[]{username, session.createdAt, SESSION_TIMEOUT_MINUTES});
            activeSessions.remove(username);
            return false;
        }

        return true;
    }

    /**
     * Returns count of active sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Returns all active sessions for monitoring.
     */
    public Map<String, SessionInfo> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    /**
     * Cleans up expired sessions.
     */
    public int cleanupExpiredSessions() {
        int cleaned = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            LocalDateTime expiry = entry.getValue().createdAt.plusMinutes(SESSION_TIMEOUT_MINUTES);
            if (now.isAfter(expiry)) {
                activeSessions.remove(entry.getKey());
                cleaned++;
            }
        }

        if (cleaned > 0) {
            LOGGER.log(Level.INFO, "Cleaned up {0} expired sessions", cleaned);
        }
        return cleaned;
    }

    /**
     * Inner class representing session metadata.
     */
    public static class SessionInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String sessionId;
        public final String username;
        public final LocalDateTime createdAt;

        public SessionInfo(String sessionId, String username, LocalDateTime createdAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.createdAt = createdAt;
        }

        public long getDurationSeconds() {
            return java.time.Duration.between(createdAt, LocalDateTime.now()).getSeconds();
        }
    }
}
