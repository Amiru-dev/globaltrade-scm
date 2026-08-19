package com.globaltrade.ejb.service;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates resource pooling strategies and optimization for EJB applications.
 *
 * <p><strong>Critical Analysis - Resource Pooling Benefits:</strong></p>
 * <ul>
 *   <li><strong>Performance:</strong> Pooling eliminates object creation overhead,
 *       reducing response time by ~10-30% for pooled resources.</li>
 *   <li><strong>Memory Efficiency:</strong> Reuses objects instead of creating new ones,
 *       reducing garbage collection pressure and memory usage.</li>
 *   <li><strong>Scalability:</strong> Limits resource consumption while maintaining
 *       high throughput under concurrent load.</li>
 *   <li><strong>Stability:</strong> Prevents resource exhaustion by limiting maximum
 *       pool size and implementing proper queuing.</li>
 * </ul>
 *
 * <p><strong>Resource Types in EJB Applications:</strong></p>
 * <ul>
 *   <li><strong>EJB Instance Pool:</strong> Container manages pool of stateless bean instances</li>
 *   <li><strong>Database Connection Pool:</strong> Reuses database connections</li>
 *   <li><strong>Thread Pool:</strong> Manages concurrent request processing</li>
 *   <li><strong>JMS Connection Pool:</strong> Reuses messaging connections</li>
 * </ul>
 */
@Stateless
@RolesAllowed({"SYSTEM_ADMIN"})
public class ResourcePoolingAnalysisService {

    private static final Logger LOGGER = Logger.getLogger(ResourcePoolingAnalysisService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    // Simulated pool metrics for demonstration
    private static final Map<String, AtomicInteger> poolMetrics = new HashMap<>();
    private static final Map<String, AtomicInteger> poolUsage = new HashMap<>();

    static {
        poolMetrics.put("EJB_INSTANCE_POOL", new AtomicInteger(10));
        poolMetrics.put("DB_CONNECTION_POOL", new AtomicInteger(20));
        poolMetrics.put("THREAD_POOL", new AtomicInteger(50));
        poolMetrics.put("JMS_CONNECTION_POOL", new AtomicInteger(5));

        poolUsage.put("EJB_INSTANCE_POOL", new AtomicInteger(0));
        poolUsage.put("DB_CONNECTION_POOL", new AtomicInteger(0));
        poolUsage.put("THREAD_POOL", new AtomicInteger(0));
        poolUsage.put("JMS_CONNECTION_POOL", new AtomicInteger(0));
    }

    /**
     * Demonstrates EJB instance pooling behavior.
     *
     * <p><strong>EJB Pool Configuration:</strong></p>
     * <ul>
     *   <li><strong>Stateless Beans:</strong> Container maintains pool, typically 10-100 instances</li>
     *   <li><strong>Stateful Beans:</strong> Not pooled - one instance per client session</li>
     *   <li><strong>Singleton Beans:</strong> Single instance per application</li>
     * </ul>
     *
     * <p><strong>Pool Size Tuning:</strong></p>
     * <ul>
     *   <li>Too small: Performance degradation under load, request queuing</li>
     *   <li>Too large: Memory waste, increased garbage collection</li>
     *   <li>Optimal: Based on average request rate and request processing time</li>
     * </ul>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void analyzeEJBInstancePooling() {
        LOGGER.log(Level.INFO, "POOL: Analyzing EJB instance pooling strategy");

        int poolSize = poolMetrics.get("EJB_INSTANCE_POOL").get();
        int currentUsage = poolUsage.get("EJB_INSTANCE_POOL").get();

        LOGGER.log(Level.INFO, "POOL: EJB Instance Pool - Size: {0}, Current Usage: {1}",
                new Object[]{poolSize, currentUsage});
        LOGGER.log(Level.INFO, "POOL: Utilization: {0}%",
                (currentUsage * 100.0 / poolSize));

        // Pool size recommendations
        if (currentUsage > poolSize * 0.8) {
            LOGGER.log(Level.WARNING, "POOL: High utilization (>80%) - consider increasing pool size");
        } else if (currentUsage < poolSize * 0.2) {
            LOGGER.log(Level.INFO, "POOL: Low utilization (<20%) - pool size may be oversized");
        } else {
            LOGGER.log(Level.INFO, "POOL: Optimal utilization (20-80%)");
        }
    }

    /**
     * Demonstrates database connection pooling optimization.
     *
     * <p><strong>Connection Pool Configuration:</strong></p>
     * <ul>
     *   <li><strong>Initial Size:</strong> Number of connections created at startup (typically 5-10)</li>
     *   <li><strong>Maximum Size:</strong> Maximum connections allowed (typically 20-100)</li>
     *   <li><strong>Validation Query:</strong> Query to test connection health</li>
     *   <li><strong>Connection Timeout:</strong> Maximum time to wait for connection</li>
     * </ul>
     *
     * <p><strong>Payara Configuration Example:</strong></p>
     * <pre>
     * asadmin create-jdbc-connection-pool \
     *   --property maxPoolSize=50:minPoolSize=5:connectionTimeout=30 \
     *   --validationQuery="SELECT 1" GlobalTradePool
     * </pre>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void analyzeDatabaseConnectionPooling() {
        LOGGER.log(Level.INFO, "POOL: Analyzing database connection pooling strategy");

        int poolSize = poolMetrics.get("DB_CONNECTION_POOL").get();
        int currentUsage = poolUsage.get("DB_CONNECTION_POOL").get();

        LOGGER.log(Level.INFO, "POOL: DB Connection Pool - Size: {0}, Current Usage: {1}",
                new Object[]{poolSize, currentUsage});
        LOGGER.log(Level.INFO, "POOL: Utilization: {0}%",
                (currentUsage * 100.0 / poolSize));

        // Connection pool optimization recommendations
        LOGGER.log(Level.INFO, "POOL: Connection Pool Optimization Guidelines:");
        LOGGER.log(Level.INFO, "POOL: 1. Set minPoolSize to handle average load (typically 5-10)");
        LOGGER.log(Level.INFO, "POOL: 2. Set maxPoolSize to handle peak load (typically 20-100)");
        LOGGER.log(Level.INFO, "POOL: 3. Use validation query to ensure connection health");
        LOGGER.log(Level.INFO, "POOL: 4. Configure appropriate timeout (typically 30-60 seconds)");
        LOGGER.log(Level.INFO, "POOL: 5. Monitor pool metrics and adjust based on usage patterns");
    }

    /**
     * Demonstrates thread pool configuration and monitoring.
     *
     * <p><strong>Thread Pool Configuration:</strong></p>
     * <ul>
     *   <li><strong>Core Pool Size:</strong> Minimum threads always available</li>
     *   <li><strong>Maximum Pool Size:</strong> Maximum threads allowed</li>
     *   <li><strong>Queue Capacity:</strong> Maximum pending requests</li>
     *   <li><strong>Keep-Alive Time:</strong> Idle thread termination time</li>
     * </ul>
     *
     * <p><strong>Thread Pool Tuning:</strong></p>
     * <ul>
     *   <li>CPU-bound tasks: Thread count ≈ CPU cores</li>
     *   <li>I/O-bound tasks: Thread count ≈ CPU cores × 2-4</li>
     *   <li>Mixed workloads: Profile and tune based on actual patterns</li>
     * </ul>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void analyzeThreadPoolConfiguration() {
        LOGGER.log(Level.INFO, "POOL: Analyzing thread pool configuration");

        int poolSize = poolMetrics.get("THREAD_POOL").get();
        int currentUsage = poolUsage.get("THREAD_POOL").get();

        LOGGER.log(Level.INFO, "POOL: Thread Pool - Size: {0}, Current Usage: {1}",
                new Object[]{poolSize, currentUsage});
        LOGGER.log(Level.INFO, "POOL: Utilization: {0}%",
                (currentUsage * 100.0 / poolSize));

        // Thread pool optimization recommendations
        LOGGER.log(Level.INFO, "POOL: Thread Pool Optimization Guidelines:");
        LOGGER.log(Level.INFO, "POOL: 1. CPU-bound tasks: threads ≈ CPU cores");
        LOGGER.log(Level.INFO, "POOL: 2. I/O-bound tasks: threads ≈ CPU cores × 2-4");
        LOGGER.log(Level.INFO, "POOL: 3. Configure queue capacity to handle traffic spikes");
        LOGGER.log(Level.INFO, "POOL: 4. Set appropriate keep-alive time for idle threads");
        LOGGER.log(Level.INFO, "POOL: 5. Monitor thread metrics and adjust based on workload");
    }

    /**
     * Simulates pool utilization patterns under load.
     *
     * <p><strong>Load Testing Patterns:</strong></p>
     * <ul>
     *   <li><strong>Steady Load:</strong> Constant request rate, stable pool utilization</li>
     *   <li><strong>Burst Load:</strong> Periodic spikes, requires adequate queue capacity</li>
     *   <li><strong>Ramp Load:</strong> Gradual increase, allows pool growth monitoring</li>
     * </ul>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void simulatePoolUtilizationPatterns() {
        LOGGER.log(Level.INFO, "POOL: Simulating pool utilization patterns");

        // Simulate steady load
        LOGGER.log(Level.INFO, "POOL: Simulating steady load (50 requests/second)");
        simulateLoad(50, 100);

        // Simulate burst load
        LOGGER.log(Level.INFO, "POOL: Simulating burst load (100 requests/second)");
        simulateLoad(100, 50);

        // Simulate ramp load
        LOGGER.log(Level.INFO, "POOL: Simulating ramp load (10 to 100 requests/second)");
        simulateRampLoad(10, 100, 50);
    }

    private void simulateLoad(int requestsPerSecond, int durationSeconds) {
        for (int i = 0; i < durationSeconds; i++) {
            int usage = Math.min(requestsPerSecond, poolMetrics.get("DB_CONNECTION_POOL").get());
            poolUsage.get("DB_CONNECTION_POOL").set(usage);
            LOGGER.log(Level.FINE, "POOL: Second {0} - Usage: {1}", new Object[]{i, usage});
        }
    }

    private void simulateRampLoad(int startRate, int endRate, int durationSeconds) {
        for (int i = 0; i < durationSeconds; i++) {
            int currentRate = startRate + (endRate - startRate) * i / durationSeconds;
            int usage = Math.min(currentRate, poolMetrics.get("DB_CONNECTION_POOL").get());
            poolUsage.get("DB_CONNECTION_POOL").set(usage);
            LOGGER.log(Level.FINE, "POOL: Second {0} - Rate: {1}, Usage: {2}", new Object[]{i, currentRate, usage});
        }
    }

    /**
     * Generates comprehensive pool monitoring report.
     *
     * <p><strong>Monitoring Metrics:</strong></p>
     * <ul>
     *   <li><strong>Utilization:</strong> Current usage vs pool size</li>
     *   <li><strong>Wait Time:</strong> Average time waiting for pooled resource</li>
     *   <li><strong>Failure Rate:</strong> Requests rejected due to pool exhaustion</li>
     *   <li><strong>Throughput:</strong> Requests processed per time unit</li>
     * </ul>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public void generatePoolMonitoringReport() {
        LOGGER.log(Level.INFO, "POOL: Generating comprehensive pool monitoring report");

        LOGGER.log(Level.INFO, "POOL MONITORING REPORT:");
        LOGGER.log(Level.INFO, "================================");

        // EJB Instance Pool
        int ejbPoolSize = poolMetrics.get("EJB_INSTANCE_POOL").get();
        int ejbUsage = poolUsage.get("EJB_INSTANCE_POOL").get();
        LOGGER.log(Level.INFO, "EJB Instance Pool:");
        LOGGER.log(Level.INFO, "  Pool Size: {0}", ejbPoolSize);
        LOGGER.log(Level.INFO, "  Current Usage: {0}", ejbUsage);
        LOGGER.log(Level.INFO, "  Utilization: {0}%", (ejbUsage * 100.0 / ejbPoolSize));

        // Database Connection Pool
        int dbPoolSize = poolMetrics.get("DB_CONNECTION_POOL").get();
        int dbUsage = poolUsage.get("DB_CONNECTION_POOL").get();
        LOGGER.log(Level.INFO, "Database Connection Pool:");
        LOGGER.log(Level.INFO, "  Pool Size: {0}", dbPoolSize);
        LOGGER.log(Level.INFO, "  Current Usage: {0}", dbUsage);
        LOGGER.log(Level.INFO, "  Utilization: {0}%", (dbUsage * 100.0 / dbPoolSize));

        // Thread Pool
        int threadPoolSize = poolMetrics.get("THREAD_POOL").get();
        int threadUsage = poolUsage.get("THREAD_POOL").get();
        LOGGER.log(Level.INFO, "Thread Pool:");
        LOGGER.log(Level.INFO, "  Pool Size: {0}", threadPoolSize);
        LOGGER.log(Level.INFO, "  Current Usage: {0}", threadUsage);
        LOGGER.log(Level.INFO, "  Utilization: {0}%", (threadUsage * 100.0 / threadPoolSize));

        LOGGER.log(Level.INFO, "================================");
        LOGGER.log(Level.INFO, "POOL RECOMMENDATIONS:");
        LOGGER.log(Level.INFO, "1. Monitor pool metrics regularly (every 5-15 minutes)");
        LOGGER.log(Level.INFO, "2. Set alerts for high utilization (>80%) and pool exhaustion");
        LOGGER.log(Level.INFO, "3. Adjust pool sizes based on usage patterns and load testing");
        LOGGER.log(Level.INFO, "4. Implement connection validation for database pools");
        LOGGER.log(Level.INFO, "5. Use appropriate timeout values to prevent resource starvation");
    }

    /**
     * Demonstrates pool exhaustion and queue handling.
     *
     * <p><strong>Pool Exhaustion Handling:</strong></p>
     * <ul>
     *   <li><strong>Queue Requests:</strong> When pool is full, requests queue up</li>
     *   <li><strong>Timeout:</strong> Requests timeout if queue is full or wait time exceeded</li>
     *   <li><strong>Fail Fast:</strong> Reject requests immediately when pool is full</li>
     *   <li><strong>Create New:</strong> Temporarily exceed pool size (if configured)</li>
     * </ul>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void demonstratePoolExhaustionHandling() {
        LOGGER.log(Level.INFO, "POOL: Demonstrating pool exhaustion handling strategies");

        int poolSize = poolMetrics.get("DB_CONNECTION_POOL").get();

        // Simulate pool exhaustion
        LOGGER.log(Level.INFO, "POOL: Simulating pool exhaustion scenario");
        poolUsage.get("DB_CONNECTION_POOL").set(poolSize + 10); // Exceed pool size

        LOGGER.log(Level.WARNING, "POOL: Pool exhaustion detected - {0} requests waiting",
                poolUsage.get("DB_CONNECTION_POOL").get() - poolSize);

        LOGGER.log(Level.INFO, "POOL: Exhaustion Handling Strategies:");
        LOGGER.log(Level.INFO, "1. Queue requests with timeout (most common)");
        LOGGER.log(Level.INFO, "2. Fail fast and return error immediately");
        LOGGER.log(Level.INFO, "3. Temporarily exceed pool size (if configured)");
        LOGGER.log(Level.INFO, "4. Scale horizontally (add more application servers)");

        // Reset for next demonstration
        poolUsage.get("DB_CONNECTION_POOL").set(0);
    }
}