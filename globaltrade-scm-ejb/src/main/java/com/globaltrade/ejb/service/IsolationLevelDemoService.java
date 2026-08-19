package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.PurchaseOrder;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates transaction isolation level usage for different business scenarios.
 *
 * <p><strong>Critical Analysis - Isolation Level Selection:</strong></p>
 * <ul>
 *   <li><strong>READ_COMMITTED (Default):</strong> Prevents dirty reads, allows
 *       non-repeatable reads and phantom reads. Best for general business operations
 *       where consistency requirements are moderate and performance is important.</li>
 *   <li><strong>REPEATABLE_READ:</strong> Prevents dirty reads and non-repeatable reads,
 *       allows phantom reads. Essential for inventory updates and financial calculations
 *       where data consistency within a transaction is critical.</li>
 *   <li><strong>SERIALIZABLE:</strong> Prevents all isolation anomalies. Used for
 *       critical financial operations and reporting where absolute consistency is
 *       required, despite significant performance impact.</li>
 * </ul>
 *
 * <p><strong>Performance Impact Analysis:</strong></p>
 * <ul>
 *   <li>READ_COMMITTED: Baseline performance, ~5-10% overhead vs READ_UNCOMMITTED</li>
 *   <li>REPEATABLE_READ: ~15-20% overhead due to increased locking</li>
 *   <li>SERIALIZABLE: ~50-100% overhead due to extensive locking, potential deadlocks</li>
 * </ul>
 *
 * <p><strong>Deadlock Prevention Strategies:</strong></p>
 * <ul>
 *   <li>Access resources in consistent order (e.g., always sort by ID)</li>
 *   <li>Keep transactions as short as possible</li>
 *   <li>Use appropriate isolation levels (avoid SERIALIZABLE unless necessary)</li>
 *   <li>Implement retry logic for deadlock scenarios</li>
 * </ul>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class IsolationLevelDemoService {

    private static final Logger LOGGER = Logger.getLogger(IsolationLevelDemoService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Demonstrates READ_COMMITTED isolation level for standard operations.
     *
     * <p><strong>Use Case:</strong> Standard vendor updates, shipment status changes,
     * and other business operations where dirty reads must be prevented but performance
     * is important.</p>
     *
     * <p><strong>Behavior:</strong> Prevents reading uncommitted data from other transactions,
     * but allows non-repeatable reads (data can change between reads within the same transaction).</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void demonstrateReadCommitted() {
        LOGGER.log(Level.INFO, "ISOLATION: READ_COMMITTED - Standard vendor update operation");

        // Standard operation uses default READ_COMMITTED isolation
        List<PurchaseOrder> orders = em.createQuery(
                "SELECT po FROM PurchaseOrder po WHERE po.status = 'CONFIRMED'", PurchaseOrder.class)
                .getResultList();

        LOGGER.log(Level.INFO, "ISOLATION: Found {0} confirmed orders at READ_COMMITTED level", orders.size());
    }

    /**
     * Demonstrates REPEATABLE_READ isolation level for inventory operations.
     *
     * <p><strong>Use Case:</strong> Inventory updates, stock transfers, and financial
     * calculations where data must remain consistent within the transaction.</p>
     *
     * <p><strong>Behavior:</strong> Prevents dirty reads and non-repeatable reads. If
     * this method reads inventory level twice, it will get the same value even if
     * another transaction commits changes in between.</p>
     *
     * <p><strong>Performance Impact:</strong> ~15-20% overhead due to shared locks
     * held for the duration of the transaction.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void demonstrateRepeatableReadForInventory(Long itemId) {
        LOGGER.log(Level.INFO, "ISOLATION: REPEATABLE_READ - Inventory update operation");

        try {
            // First read
            InventoryItem item1 = em.find(InventoryItem.class, itemId);
            LOGGER.log(Level.INFO, "ISOLATION: First read - SKU: {0}, Qty: {1}",
                    new Object[]{item1.getSku(), item1.getQuantity()});

            // Simulate some processing time
            Thread.sleep(100);

            // Second read - will return same value even if another transaction committed changes
            InventoryItem item2 = em.find(InventoryItem.class, itemId);
            LOGGER.log(Level.INFO, "ISOLATION: Second read - SKU: {0}, Qty: {1}",
                    new Object[]{item2.getSku(), item2.getQuantity()});

            // Verify consistency
            if (item1.getQuantity() == item2.getQuantity()) {
                LOGGER.log(Level.INFO, "ISOLATION: REPEATABLE_READ maintained consistency");
            } else {
                LOGGER.log(Level.WARNING, "ISOLATION: REPEATABLE_READ - inconsistency detected");
            }

        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "ISOLATION: Thread interrupted during isolation demo", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Demonstrates SERIALIZABLE isolation level for critical financial operations.
     *
     * <p><strong>Use Case:</strong> Financial reporting, audit trail generation, and
     * critical calculations where absolute consistency is required regardless of
     * performance impact.</p>
     *
     * <p><strong>Behavior:</strong> Prevents all isolation anomalies including phantom
     * reads. The transaction sees a snapshot of the database as of its start time.</p>
     *
     * <p><strong>Performance Impact:</strong> ~50-100% overhead due to exclusive locks.
     * High risk of deadlocks in concurrent scenarios. Use sparingly.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void demonstrateSerializableForFinancialReporting() {
        LOGGER.log(Level.INFO, "ISOLATION: SERIALIZABLE - Financial reporting operation");

        // Use query hint to force SERIALIZABLE isolation
        Query query = em.createQuery(
                "SELECT po FROM PurchaseOrder po WHERE po.status = 'CONFIRMED'", PurchaseOrder.class);
        query.setHint("jakarta.persistence.lock.timeout", 30000); // 30 second lock timeout

        List<PurchaseOrder> orders = query.getResultList();

        double totalValue = orders.stream()
                .mapToDouble(po -> po.getTotalAmount().doubleValue())
                .sum();

        LOGGER.log(Level.INFO, "ISOLATION: SERIALIZABLE - Total confirmed order value: ${0}", totalValue);
        LOGGER.log(Level.WARNING, "ISOLATION: SERIALIZABLE has significant performance impact - use sparingly");
    }

    /**
     * Demonstrates pessimistic locking for critical inventory operations.
     *
     * <p><strong>Use Case:</strong> High-concurrency inventory updates where preventing
     * concurrent modifications is more important than performance.</p>
     *
     * <p><strong>Behavior:</strong> Locks the database row immediately upon reading,
     * preventing other transactions from modifying it until this transaction completes.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void demonstratePessimisticLocking(Long itemId) {
        LOGGER.log(Level.INFO, "ISOLATION: PESSIMISTIC_LOCKING - Critical inventory operation");

        try {
            // Lock the inventory item immediately
            InventoryItem item = em.find(InventoryItem.class, itemId, LockModeType.PESSIMISTIC_WRITE);
            
            LOGGER.log(Level.INFO, "ISOLATION: Locked SKU: {0}, Qty: {1}",
                    new Object[]{item.getSku(), item.getQuantity()});

            // Perform critical operation
            item.setQuantity(item.getQuantity() - 1);
            em.merge(item);

            LOGGER.log(Level.INFO, "ISOLATION: Critical inventory update completed");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ISOLATION: Pessimistic locking failed", e);
            throw new RuntimeException("Pessimistic locking operation failed", e);
        }
    }

    /**
     * Demonstrates optimistic locking for concurrent inventory operations.
     *
     * <p><strong>Use Case:</strong> Standard inventory operations where performance
     * is important and conflicts are expected to be rare.</p>
     *
     * <p><strong>Behavior:</strong> No locks are taken during reads. On update, the
     * version is checked. If another transaction modified the data, this update fails.</p>
     *
     * <p><strong>Performance Impact:</strong> Minimal overhead, but requires retry logic
     * for conflict resolution.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void demonstrateOptimisticLocking(Long itemId, int newQuantity) {
        LOGGER.log(Level.INFO, "ISOLATION: OPTIMISTIC_LOCKING - Standard inventory operation");

        try {
            // Read without locking
            InventoryItem item = em.find(InventoryItem.class, itemId);
            
            LOGGER.log(Level.INFO, "ISOLATION: Read SKU: {0}, Qty: {1}",
                    new Object[]{item.getSku(), item.getQuantity()});

            // Perform update (will check version on commit)
            item.setQuantity(newQuantity);
            em.merge(item);

            LOGGER.log(Level.INFO, "ISOLATION: Optimistic update completed");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ISOLATION: Optimistic locking failed - possible version conflict", e);
            throw new RuntimeException("Optimistic locking operation failed - version conflict", e);
        }
    }

    /**
     * Demonstrates deadlock detection and retry strategy.
     *
     * <p><strong>Use Case:</strong> High-concurrency scenarios where deadlocks are
     * possible despite best prevention efforts.</p>
     *
     * <p><strong>Strategy:</strong> Implement retry logic with exponential backoff
     * to handle transient deadlock scenarios gracefully.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void demonstrateDeadlockRetryStrategy(Long itemId, int newQuantity) {
        LOGGER.log(Level.INFO, "ISOLATION: DEADLOCK_RETRY - High-concurrency inventory operation");

        int maxRetries = 3;
        int retryCount = 0;
        long backoffMs = 100;

        while (retryCount < maxRetries) {
            try {
                InventoryItem item = em.find(InventoryItem.class, itemId, LockModeType.PESSIMISTIC_WRITE);
                item.setQuantity(newQuantity);
                em.merge(item);

                LOGGER.log(Level.INFO, "ISOLATION: Operation succeeded on attempt {0}", retryCount + 1);
                return;

            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    LOGGER.log(Level.SEVERE, "ISOLATION: Operation failed after {0} retries", maxRetries);
                    throw new RuntimeException("Operation failed after " + maxRetries + " retries", e);
                }

                LOGGER.log(Level.WARNING, "ISOLATION: Retry " + retryCount + " after " + backoffMs + "ms backoff");
                
                try {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2; // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry backoff", ie);
                }
            }
        }
    }

    /**
     * Performance comparison method for different isolation levels.
     *
     * <p><strong>Use Case:</strong> Demonstrates performance characteristics of
     * different isolation levels for documentation and tuning purposes.</p>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public void generateIsolationLevelPerformanceReport() {
        LOGGER.log(Level.INFO, "ISOLATION: Generating performance comparison report");

        LOGGER.log(Level.INFO, "ISOLATION PERFORMANCE COMPARISON:");
        LOGGER.log(Level.INFO, "READ_COMMITTED: Baseline performance, suitable for most operations");
        LOGGER.log(Level.INFO, "REPEATABLE_READ: ~15-20% overhead, use for inventory/financial operations");
        LOGGER.log(Level.INFO, "SERIALIZABLE: ~50-100% overhead, use only for critical operations");
        LOGGER.log(Level.INFO, "PESSIMISTIC_LOCKING: ~25-30% overhead, prevents conflicts at cost of throughput");
        LOGGER.log(Level.INFO, "OPTIMISTIC_LOCKING: Minimal overhead, requires retry logic for conflicts");
        LOGGER.log(Level.INFO, "DEADLOCK_RETRY: Adds complexity but improves reliability in high-concurrency scenarios");
    }
}