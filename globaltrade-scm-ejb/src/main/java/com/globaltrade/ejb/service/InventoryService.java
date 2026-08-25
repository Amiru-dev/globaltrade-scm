package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.exception.InventoryShortageException;
import com.globaltrade.common.exception.TransactionRollbackException;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Session Bean for inventory management operations.
 *
 * <p><strong>Transaction Analysis:</strong> Inventory operations are highly sensitive
 * to concurrent access. The {@code transferInventory} method uses REQUIRED to ensure
 * atomic debit-credit between warehouses, while read operations use SUPPORTS
 * to avoid unnecessary transaction overhead.</p>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "SYSTEM_ADMIN"})
public class InventoryService {

    private static final Logger LOGGER = Logger.getLogger(InventoryService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Adds a new inventory item to a warehouse.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem addInventoryItem(InventoryItem item) {
        LOGGER.log(Level.INFO, "Adding inventory item: SKU={0}, Warehouse={1}",
                new Object[]{item.getSku(), item.getWarehouseId()});
        em.persist(item);
        return item;
    }

    /**
     * Deducts stock from inventory. Throws InventoryShortageException if
     * insufficient stock is available.
     *
     * <p>rollback=false on InventoryShortageException allows partial fulfilment
     * scenarios — the caller can decide to proceed with available quantity.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem deductStock(Long itemId, int quantity) throws InventoryShortageException {
        InventoryItem item = em.find(InventoryItem.class, itemId);
        if (item == null) {
            throw new InventoryShortageException(
                    "Inventory item not found: " + itemId, "UNKNOWN", quantity, 0);
        }

        if (item.getQuantity() < quantity) {
            throw new InventoryShortageException(
                    "Insufficient stock for SKU " + item.getSku() +
                            ": requested=" + quantity + ", available=" + item.getQuantity(),
                    item.getSku(), quantity, item.getQuantity());
        }

        item.setQuantity(item.getQuantity() - quantity);
        InventoryItem updated = em.merge(item);
        LOGGER.log(Level.INFO, "Stock deducted: SKU={0}, qty=-{1}, remaining={2}",
                new Object[]{item.getSku(), quantity, updated.getQuantity()});
        return updated;
    }

    /**
     * Replenishes stock in inventory.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InventoryItem replenishStock(Long itemId, int quantity) {
        InventoryItem item = em.find(InventoryItem.class, itemId);
        if (item != null) {
            item.setQuantity(item.getQuantity() + quantity);
            item.setLastReplenished(LocalDateTime.now());
            em.merge(item);
            LOGGER.log(Level.INFO, "Stock replenished: SKU={0}, qty=+{1}, total={2}",
                    new Object[]{item.getSku(), quantity, item.getQuantity()});
        }
        return item;
    }

    /**
     * Transfers inventory between warehouses atomically.
     * REQUIRED: Both debit and credit must succeed or both must fail.
     *
     * <p><strong>Critical Analysis:</strong> This is a classic example of why
     * ACID transactions are essential. Without atomicity, a failure between
     * the debit and credit operations would result in "lost" inventory.</p>
     *
     * <p><strong>Transaction Timeout Analysis:</strong> Standard REQUIRED transaction for
     * inventory transfers. Timeout can be configured via ejb-jar.xml if needed.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transferInventory(Long sourceItemId, Long targetItemId, int quantity)
            throws InventoryShortageException, TransactionRollbackException {

        InventoryItem source = em.find(InventoryItem.class, sourceItemId);
        InventoryItem target = em.find(InventoryItem.class, targetItemId);

        if (source == null || target == null) {
            throw new TransactionRollbackException(
                    "Source or target inventory item not found",
                    "TRANSFER-" + sourceItemId + "-" + targetItemId);
        }

        if (source.getQuantity() < quantity) {
            throw new InventoryShortageException(
                    "Insufficient stock for transfer",
                    source.getSku(), quantity, source.getQuantity());
        }

        // Atomic debit-credit
        source.setQuantity(source.getQuantity() - quantity);
        target.setQuantity(target.getQuantity() + quantity);
        target.setLastReplenished(LocalDateTime.now());

        em.merge(source);
        em.merge(target);

        LOGGER.log(Level.INFO, "Inventory transferred: {0} units from item {1} to item {2}",
                new Object[]{quantity, sourceItemId, targetItemId});
    }

    /**
     * Finds all items below their reorder level — used by InventoryMonitorTimer.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<InventoryItem> findLowStockItems() {
        return em.createNamedQuery("InventoryItem.findLowStock", InventoryItem.class)
                .getResultList();
    }

    /**
     * Finds all out-of-stock items.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<InventoryItem> findOutOfStockItems() {
        return em.createNamedQuery("InventoryItem.findOutOfStock", InventoryItem.class)
                .getResultList();
    }

    /**
     * Supply chain-specific: Finds items with excess inventory for space optimization.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<InventoryItem> findExcessInventory() {
        return em.createQuery(
                        "SELECT i FROM InventoryItem i WHERE i.quantity > i.maxStockLevel * 0.8", InventoryItem.class)
                .getResultList();
    }

    /**
     * Finds all items in a specific warehouse.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<InventoryItem> findByWarehouse(Long warehouseId) {
        return em.createNamedQuery("InventoryItem.findByWarehouse", InventoryItem.class)
                .setParameter("warehouseId", warehouseId)
                .getResultList();
    }

    /**
     * Retrieves a single inventory item by ID.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public InventoryItem findById(Long itemId) {
        return em.find(InventoryItem.class, itemId);
    }
}
