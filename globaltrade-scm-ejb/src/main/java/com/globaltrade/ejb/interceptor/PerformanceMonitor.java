package com.globaltrade.ejb.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.InvocationContext;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class-level interceptor for monitoring business method execution performance.
 *
 * <p><strong>Interceptor Level: DEFAULT (ejb-jar.xml)</strong> — Configured as
 * a default interceptor in the deployment descriptor to apply to all EJB
 * beans without annotation changes. This demonstrates XML-based interceptor
 * binding as an alternative to annotation-based binding.</p>
 *
 * <p><strong>Critical Analysis — Performance Monitoring Overhead:</strong></p>
 * <ul>
 *   <li>Each intercepted call adds ~0.1ms overhead for timing measurement.
 *       For a system processing thousands of operations per minute, this is
 *       acceptable and provides invaluable performance visibility.</li>
 *   <li>Uses ConcurrentHashMap for thread-safe metric storage without
 *       synchronization bottlenecks.</li>
 *   <li>Also intercepts {@code @Timeout} methods via {@code @AroundTimeout}
 *       to monitor timer callback performance — critical for ensuring
 *       timer methods don't exceed their interval.</li>
 * </ul>
 *
 * <p><strong>Performance Thresholds:</strong></p>
 * <ul>
 *   <li>WARNING: &gt; 1000ms — Method is slow, may impact user experience</li>
 *   <li>SEVERE: &gt; 5000ms — Method is critically slow, investigate immediately</li>
 * </ul>
 */
public class PerformanceMonitor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PerformanceMonitor.class.getName());

    private static final long WARN_THRESHOLD_MS = 1000;
    private static final long SEVERE_THRESHOLD_MS = 5000;

    // Thread-safe performance metrics storage
    private static final Map<String, AtomicLong> invocationCounts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> totalExecutionTimes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> maxExecutionTimes = new ConcurrentHashMap<>();

    /**
     * Monitors business method execution time.
     */
    @AroundInvoke
    public Object monitorPerformance(InvocationContext ctx) throws Exception {
        return executeWithMonitoring(ctx, "BUSINESS");
    }

    /**
     * Monitors timer @Timeout callback execution time.
     * Critical for ensuring timer methods complete within their interval.
     */
    @AroundTimeout
    public Object monitorTimerPerformance(InvocationContext ctx) throws Exception {
        return executeWithMonitoring(ctx, "TIMER");
    }

    /**
     * Core monitoring logic shared between business and timer methods.
     */
    private Object executeWithMonitoring(InvocationContext ctx, String methodType)
            throws Exception {

        String className = ctx.getTarget().getClass().getSimpleName();
        String methodName = ctx.getMethod().getName();
        String key = className + "." + methodName;

        long startTime = System.nanoTime();

        try {
            Object result = ctx.proceed();
            return result;

        } finally {
            long elapsed = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

            // Update metrics
            invocationCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            totalExecutionTimes.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(elapsed);
            maxExecutionTimes.computeIfAbsent(key, k -> new AtomicLong(0))
                    .updateAndGet(max -> Math.max(max, elapsed));

            // Log based on threshold
            if (elapsed > SEVERE_THRESHOLD_MS) {
                LOGGER.log(Level.SEVERE,
                        "PERFORMANCE CRITICAL [{0}]: {1} took {2}ms (threshold: {3}ms)",
                        new Object[]{methodType, key, elapsed, SEVERE_THRESHOLD_MS});
            } else if (elapsed > WARN_THRESHOLD_MS) {
                LOGGER.log(Level.WARNING,
                        "PERFORMANCE WARNING [{0}]: {1} took {2}ms (threshold: {3}ms)",
                        new Object[]{methodType, key, elapsed, WARN_THRESHOLD_MS});
            } else {
                LOGGER.log(Level.FINE,
                        "PERFORMANCE [{0}]: {1} completed in {2}ms",
                        new Object[]{methodType, key, elapsed});
            }
        }
    }

    // ======================== Metrics Access (for dashboard) ========================

    /**
     * Returns invocation count for a specific method.
     */
    public static long getInvocationCount(String methodKey) {
        AtomicLong count = invocationCounts.get(methodKey);
        return count != null ? count.get() : 0;
    }

    /**
     * Returns average execution time for a specific method.
     */
    public static double getAverageExecutionTime(String methodKey) {
        AtomicLong total = totalExecutionTimes.get(methodKey);
        AtomicLong count = invocationCounts.get(methodKey);
        if (total == null || count == null || count.get() == 0) return 0.0;
        return (double) total.get() / count.get();
    }

    /**
     * Returns the maximum execution time recorded for a specific method.
     */
    public static long getMaxExecutionTime(String methodKey) {
        AtomicLong max = maxExecutionTimes.get(methodKey);
        return max != null ? max.get() : 0;
    }

    /**
     * Returns all collected performance metrics.
     */
    public static Map<String, AtomicLong> getAllInvocationCounts() {
        return new ConcurrentHashMap<>(invocationCounts);
    }

    /**
     * Resets all performance metrics.
     */
    public static void resetMetrics() {
        invocationCounts.clear();
        totalExecutionTimes.clear();
        maxExecutionTimes.clear();
    }
}
