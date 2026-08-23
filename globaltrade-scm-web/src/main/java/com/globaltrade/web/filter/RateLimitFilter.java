package com.globaltrade.web.filter;

import com.globaltrade.common.util.SystemConstants;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiting filter for authentication endpoints.
 *
 * <p><strong>Critical Analysis — Rate Limiting Strategy:</strong></p>
 * <ul>
 *   <li><strong>Sliding Window:</strong> Uses a sliding window per client IP
 *       to count requests within the configured time window.</li>
 *   <li><strong>Scope:</strong> Only applies to token-issuing endpoints
 *       (/auth/login and /auth/refresh) to prevent brute-force attacks.</li>
 *   <li><strong>In-Memory:</strong> Uses ConcurrentHashMap for thread-safe
 *       tracking. Suitable for single-instance deployments.</li>
 *   <li><strong>Cleanup:</strong> Lazy eviction of expired entries on each
 *       request to prevent unbounded memory growth.</li>
 * </ul>
 *
 * <p><strong>Limitation:</strong> For clustered Payara deployments, replace
 * with a distributed rate limiter (Redis, Hazelcast).</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 100) // Run before authentication filter
public class RateLimitFilter implements ContainerRequestFilter {

    // Map of client IP -> deque of request timestamps (epoch millis)
    private static final Map<String, ConcurrentLinkedDeque<Long>> REQUEST_LOG = new ConcurrentHashMap<>();

    // Lazy cleanup counter — evict stale entries every N requests
    private static final int CLEANUP_INTERVAL = 100;
    private static int requestCounter = 0;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        // Only rate-limit token-issuing endpoints
        if (!isRateLimitedPath(path)) {
            return;
        }

        String clientIp = getClientIp(requestContext);
        int maxRequests = getMaxRequests(path);
        long windowMs = SystemConstants.RATE_LIMIT_WINDOW_SECONDS * 1000L;
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // Get or create request log for this client
        ConcurrentLinkedDeque<Long> timestamps = REQUEST_LOG.computeIfAbsent(
                clientIp, k -> new ConcurrentLinkedDeque<>());

        // Evict expired timestamps from the front of the deque
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        // Check if limit exceeded
        if (timestamps.size() >= maxRequests) {
            long oldestInWindow = timestamps.peekFirst();
            long retryAfterSeconds = Math.max(1, (oldestInWindow + windowMs - now) / 1000);

            requestContext.abortWith(
                    Response.status(429) // Too Many Requests
                            .header("Retry-After", String.valueOf(retryAfterSeconds))
                            .entity("{\"error\":\"Too many requests. Please try again later.\"}")
                            .type("application/json")
                            .build()
            );
            return;
        }

        // Record this request
        timestamps.addLast(now);

        // Periodic cleanup of stale client entries
        if (++requestCounter % CLEANUP_INTERVAL == 0) {
            cleanupStaleEntries(windowStart);
        }
    }

    /**
     * Checks if the request path should be rate-limited.
     */
    private boolean isRateLimitedPath(String path) {
        return path.endsWith("/auth/login") || path.endsWith("/auth/refresh");
    }

    /**
     * Returns the max requests allowed for the given path.
     */
    private int getMaxRequests(String path) {
        if (path.endsWith("/auth/login")) {
            return SystemConstants.RATE_LIMIT_LOGIN_MAX_REQUESTS;
        }
        return SystemConstants.RATE_LIMIT_REFRESH_MAX_REQUESTS;
    }

    /**
     * Extracts client IP from request, respecting X-Forwarded-For for proxied requests.
     */
    private String getClientIp(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Take the first IP (original client) from the chain
            return forwarded.split(",")[0].trim();
        }
        // Fall back to direct remote address via servlet request
        // JAX-RS doesn't expose remote address directly; use a reasonable default
        String remoteHost = requestContext.getHeaderString("X-Real-IP");
        return remoteHost != null ? remoteHost : "unknown";
    }

    /**
     * Removes entries for clients with no recent requests to prevent memory leaks.
     */
    private void cleanupStaleEntries(long windowStart) {
        REQUEST_LOG.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Long> timestamps = entry.getValue();
            // Remove expired timestamps
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            // Remove entry if no timestamps remain
            return timestamps.isEmpty();
        });
    }
}
