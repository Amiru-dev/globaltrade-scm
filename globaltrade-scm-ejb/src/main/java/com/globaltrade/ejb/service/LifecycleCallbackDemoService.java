package com.globaltrade.ejb.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive demonstration of EJB lifecycle callbacks and best practices.
 *
 * <p><strong>Critical Analysis - Lifecycle Callback Types:</strong></p>
 * <ul>
 *   <li><strong>@PostConstruct:</strong> Called after bean instantiation and dependency injection.
 *       Used for initialization that requires injected resources.</li>
 *   <li><strong>@PreDestroy:</strong> Called before bean removal. Used for cleanup and resource release.</li>
 *   <li><strong>@PostActivate:</strong> Called after stateful bean activation from secondary storage.</li>
 *   <li><strong>@PrePassivate:</strong> Called before stateful bean passivation to secondary storage.</li>
 *   <li><strong>@AroundConstruct:</strong> Interceptor around bean construction (EJB 3.2+).</li>
 *   <li><strong>@Remove:</strong> Business method that triggers stateful bean destruction.</li>
 * </ul>
 *
 * <p><strong>Resource Management Best Practices:</strong></p>
 * <ul>
 *   <li>Initialize expensive resources in @PostConstruct</li>
 *   <li>Release resources in @PreDestroy to prevent leaks</li>
 *   <li>Handle passivation/activation for stateful beans</li>
 *   <li>Use try-finally blocks for resource cleanup in business methods</li>
 * </ul>
 */

/**
 * Stateless bean demonstrating basic lifecycle callbacks.
 */
@Stateless
@SuppressWarnings("WeakerAccess")
class StatelessLifecycleDemo {

    private static final Logger LOGGER = Logger.getLogger(StatelessLifecycleDemo.class.getName());
    private static int instanceCount = 0;
    private int instanceId;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Called after bean creation and dependency injection.
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Initialize resource references</li>
     *   <li>Validate configuration</li>
     *   <li>Cache frequently accessed data</li>
     *   <li>Register with external services</li>
     * </ul>
     */
    @PostConstruct
    public void initialize() {
        instanceId = ++instanceCount;
        LOGGER.info("LIFECYCLE: Stateless bean #" + instanceId + " initialized. Total instances: " + instanceCount);

        // Resource initialization
        LOGGER.fine("LIFECYCLE: EntityManager injected: " + (em != null));
    }

    /**
     * Called before bean removal from pool.
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Release external resources</li>
     *   <li>Unregister from external services</li>
     *   <li>Flush cached data</li>
     *   <li>Log cleanup activities</li>
     * </ul>
     */
    @PreDestroy
    public void cleanup() {
        LOGGER.info("LIFECYCLE: Stateless bean #" + instanceId + " being destroyed");
        instanceCount--;
        LOGGER.info("LIFECYCLE: Remaining instances: " + instanceCount);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String performOperation() {
        return "Operation performed by instance #" + instanceId;
    }
}

/**
 * Stateful bean demonstrating extended lifecycle callbacks including passivation/activation.
 */
@Stateful
@SuppressWarnings("WeakerAccess")
class StatefulLifecycleDemo implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(StatefulLifecycleDemo.class.getName());

    private String sessionId;
    private List<String> operationsLog;
    private long creationTime;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Called when stateful bean is created for a client session.
     */
    @PostConstruct
    public void initialize() {
        sessionId = "SESSION-" + System.currentTimeMillis();
        operationsLog = new ArrayList<>();
        creationTime = System.currentTimeMillis();

        LOGGER.info("LIFECYCLE: Stateful bean created for session " + sessionId);
    }

    /**
     * Called before bean is passivated (serialized to secondary storage).
     *
     * <p><strong>Passivation Analysis:</strong></p>
     * <ul>
     *   <li>Occurs when container needs to free memory</li>
     *   <li>Bean state is serialized to disk</li>
     *   <li>Non-serializable resources must be cleaned up</li>
     *   <li>EntityManager is automatically handled by container</li>
     * </ul>
     */
    @PrePassivate
    public void beforePassivation() {
        LOGGER.info("LIFECYCLE: Stateful bean " + sessionId + " being passivated. Operations: " + operationsLog.size());

        // Clean up non-serializable resources if any
        // EntityManager is automatically handled by container
    }

    /**
     * Called after bean is activated (deserialized from secondary storage).
     *
     * <p><strong>Activation Analysis:</strong></p>
     * <ul>
     *   <li>Occurs when client calls method on passivated bean</li>
     *   <li>Bean state is restored from disk</li>
     *   <li>Re-acquire non-serializable resources</li>
     *   <li>EntityManager is automatically re-injected</li>
     * </ul>
     */
    @PostActivate
    public void afterActivation() {
        LOGGER.info("LIFECYCLE: Stateful bean " + sessionId + " activated. Operations: " + operationsLog.size());

        // Re-acquire any non-serializable resources if needed
        // EntityManager is automatically re-injected by container
    }

    /**
     * Called before bean destruction.
     */
    @PreDestroy
    public void cleanup() {
        long sessionDuration = (System.currentTimeMillis() - creationTime) / 1000;
        LOGGER.info("LIFECYCLE: Stateful bean " + sessionId + " destroyed. Duration: " + sessionDuration + "s, Operations: " + operationsLog.size());
    }

    /**
     * Business method that explicitly triggers bean removal.
     *
     * <p><strong>@Remove Analysis:</strong></p>
     * <ul>
     *   <li>Allows client-controlled bean lifecycle</li>
     *   <li>Triggers @PreDestroy callback</li>
     *   <li>Used for explicit session termination</li>
     *   <li>Cannot be called after @PreDestroy</li>
     * </ul>
     */
    @Remove
    public void endSession() {
        LOGGER.info("LIFECYCLE: Explicit session termination requested for " + sessionId);
    }

    public void logOperation(String operation) {
        operationsLog.add(operation);
        LOGGER.fine("LIFECYCLE: Operation logged: " + operation);
    }

    public List<String> getOperationsLog() {
        return new ArrayList<>(operationsLog);
    }
}

/**
 * Service demonstrating comprehensive lifecycle management patterns.
 */
@Stateless
@SuppressWarnings("WeakerAccess")
class LifecycleManagementService {

    private static final Logger LOGGER = Logger.getLogger(LifecycleManagementService.class.getName());

    /**
     * Demonstrates resource initialization patterns.
     *
     * <p><strong>Resource Initialization Best Practices:</strong></p>
     * <ul>
     *   <li>Lazy initialization for expensive resources</li>
     *   <li>Double-checked locking for thread-safe initialization</li>
     *   <li>Circuit breaker pattern for external dependencies</li>
     *   <li>Connection validation before use</li>
     * </ul>
     */
    public void demonstrateResourceInitialization() {
        LOGGER.info("LIFECYCLE: Demonstrating resource initialization patterns");

        LOGGER.info("LIFECYCLE: Resource Initialization Patterns:");
        LOGGER.info("1. Eager initialization in @PostConstruct (for essential resources)");
        LOGGER.info("2. Lazy initialization on first use (for expensive resources)");
        LOGGER.info("3. Connection validation before use (for reliability)");
        LOGGER.info("4. Circuit breaker pattern (for external dependencies)");
        LOGGER.info("5. Resource health monitoring (for proactive detection)");
    }

    /**
     * Demonstrates resource cleanup patterns.
     *
     * <p><strong>Resource Cleanup Best Practices:</strong></p>
     * <ul>
     *   <li>Always use try-finally for resource cleanup</li>
     *   <li>Implement proper exception handling in cleanup</li>
     *   <li>Log cleanup activities for debugging</li>
     *   <li>Handle cleanup failures gracefully</li>
     * </ul>
     */
    public void demonstrateResourceCleanup() {
        LOGGER.info("LIFECYCLE: Demonstrating resource cleanup patterns");

        LOGGER.info("LIFECYCLE: Resource Cleanup Patterns:");
        LOGGER.info("1. Use try-finally for guaranteed cleanup");
        LOGGER.info("2. Implement proper exception handling in cleanup");
        LOGGER.info("3. Log cleanup activities for debugging");
        LOGGER.info("4. Handle cleanup failures gracefully");
        LOGGER.info("5. Use @PreDestroy for container-managed cleanup");
    }

    /**
     * Demonstrates passivation/activation strategies.
     *
     * <p><strong>Passivation Best Practices:</strong></p>
     * <ul>
     *   <li>Keep stateful bean state minimal</li>
     *   <li>Implement Serializable for all state fields</li>
     *   <li>Clean up non-serializable resources in @PrePassivate</li>
     *   <li>Re-acquire resources in @PostActivate</li>
     * </ul>
     */
    public void demonstratePassivationStrategies() {
        LOGGER.info("LIFECYCLE: Demonstrating passivation/activation strategies");

        LOGGER.info("LIFECYCLE: Passivation/Activation Strategies:");
        LOGGER.info("1. Keep stateful bean state minimal");
        LOGGER.info("2. Implement Serializable for all state fields");
        LOGGER.info("3. Clean up non-serializable resources in @PrePassivate");
        LOGGER.info("4. Re-acquire resources in @PostActivate");
        LOGGER.info("5. Use transient for non-serializable fields");
    }

    /**
     * Generates lifecycle monitoring report.
     */
    public void generateLifecycleMonitoringReport() {
        LOGGER.info("LIFECYCLE: Generating lifecycle monitoring report");

        LOGGER.info("LIFECYCLE MONITORING REPORT:");
        LOGGER.info("================================");
        LOGGER.info("Lifecycle Callback Frequency:");
        LOGGER.info("  @PostConstruct: Once per bean instance creation");
        LOGGER.info("  @PreDestroy: Once per bean instance destruction");
        LOGGER.info("  @PostActivate: After each stateful bean activation");
        LOGGER.info("  @PrePassivate: Before each stateful bean passivation");
        LOGGER.info("  @AroundConstruct: During bean construction (EJB 3.2+)");
        LOGGER.info("  @Remove: When client calls removal method");
        LOGGER.info("================================");
        LOGGER.info("Performance Considerations:");
        LOGGER.info("  @PostConstruct overhead: ~1-5ms per bean creation");
        LOGGER.info("  @PreDestroy overhead: ~1-3ms per bean destruction");
        LOGGER.info("  Passivation overhead: ~10-50ms (depends on state size)");
        LOGGER.info("  Activation overhead: ~10-50ms (depends on state size)");
        LOGGER.info("================================");
        LOGGER.info("Best Practices:");
        LOGGER.info(" 1. Keep @PostConstruct logic lightweight");
        LOGGER.info(" 2. Ensure @PreDestroy always completes successfully");
        LOGGER.info(" 3. Minimize stateful bean state for passivation efficiency");
        LOGGER.info(" 4. Implement proper error handling in all callbacks");
        LOGGER.info("  5. Monitor lifecycle events for troubleshooting");
    }
}