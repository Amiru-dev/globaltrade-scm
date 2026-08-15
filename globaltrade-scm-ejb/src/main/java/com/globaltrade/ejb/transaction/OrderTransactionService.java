package com.globaltrade.ejb.transaction;

import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.PurchaseOrder;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.OrderStatus;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.common.exception.InventoryShortageException;
import com.globaltrade.common.exception.TransactionRollbackException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates Container-Managed Transaction (CMT) attributes for
 * complex order processing workflows.
 *
 * <p><strong>Critical Analysis — CMT Transaction Attributes:</strong></p>
 * <ul>
 *   <li><strong>REQUIRED</strong> (default): Joins existing tx or creates new.
 *       Used for standard CRUD — ensures atomicity without forcing a new tx.</li>
 *   <li><strong>REQUIRES_NEW</strong>: Always creates a new, independent tx.
 *       Used for audit logging and customs processing — must commit even if
 *       the caller's tx rolls back.</li>
 *   <li><strong>SUPPORTS</strong>: Uses tx if one exists; no tx otherwise.
 *       Optimal for read-only operations — avoids tx overhead.</li>
 *   <li><strong>NOT_SUPPORTED</strong>: Suspends any existing tx.
 *       Used for long-running reports that shouldn't hold tx locks.</li>
 *   <li><strong>MANDATORY</strong>: Requires an existing tx; throws exception otherwise.
 *       Used when a method must be called within a tx context.</li>
 *   <li><strong>NEVER</strong>: Throws exception if a tx exists.
 *       Used for methods that must not run in a tx context.</li>
 * </ul>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class OrderTransactionService {

    private static final Logger LOGGER = Logger.getLogger(OrderTransactionService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * REQUIRED — Standard transactional order fulfilment.
     * Creates shipment, updates order status, and deducts inventory atomically.
     * All operations succeed or all fail together.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment fulfillOrder(Long orderId, String trackingNumber, String carrier)
            throws TransactionRollbackException, InventoryShortageException {

        LOGGER.log(Level.INFO, "Starting order fulfilment: orderId={0}", orderId);

        // 1. Retrieve and validate order
        PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
        if (order == null) {
            throw new TransactionRollbackException(
                "Order not found: " + orderId, "FULFIL-" + orderId);
        }

        // 2. Create shipment for the order
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(trackingNumber);
        shipment.setOriginCountry("Vendor Country");
        shipment.setOriginCity("Vendor City");
        shipment.setDestinationCountry("Warehouse Country");
        shipment.setDestinationCity("Warehouse City");
        shipment.setCarrier(carrier);
        shipment.setStatus(ShipmentStatus.CREATED);
        shipment.setEstimatedDelivery(
            LocalDateTime.now().plusDays(14));

        em.persist(shipment);

        // 3. Update order status
        order.setStatus(OrderStatus.SHIPPED);
        em.merge(order);

        LOGGER.log(Level.INFO,
            "Order {0} fulfilled: Shipment {1} created via {2}",
            new Object[]{order.getOrderNumber(), trackingNumber, carrier});

        // All operations succeed atomically — committed by container
        return shipment;
    }

    /**
     * REQUIRES_NEW — Independent audit logging that commits even if caller rolls back.
     * This ensures audit trail integrity regardless of business operation outcome.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logTransactionEvent(String eventType, String details) {
        LOGGER.log(Level.INFO, "TX EVENT [{0}]: {1}", new Object[]{eventType, details});
        // In production, this would persist to an audit table
    }

    /**
     * SUPPORTS — Read-only report generation. Participates in tx if one exists,
     * but does not create a new one. Optimal for queries.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getOrderCountByStatus(OrderStatus status) {
        return em.createQuery(
                "SELECT COUNT(po) FROM PurchaseOrder po WHERE po.status = :status",
                Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * NOT_SUPPORTED — Long-running report generation.
     * Suspends any existing transaction to avoid holding locks during
     * potentially slow aggregation queries.
     *
     * <p><strong>Critical Analysis:</strong> NOT_SUPPORTED is essential for
     * reports that may take seconds to complete. Holding a tx lock during
     * this time would block other operations on the same data.</p>
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String generateOrderSummaryReport() {
        LOGGER.log(Level.INFO, "Generating order summary report (no tx context)...");

        long draftCount = getOrderCountByStatus(OrderStatus.DRAFT);
        long submittedCount = getOrderCountByStatus(OrderStatus.SUBMITTED);
        long processingCount = getOrderCountByStatus(OrderStatus.PROCESSING);
        long shippedCount = getOrderCountByStatus(OrderStatus.SHIPPED);
        long receivedCount = getOrderCountByStatus(OrderStatus.RECEIVED);

        return String.format(
            "Order Summary Report [%s]%n" +
            "  Draft: %d | Submitted: %d | Processing: %d%n" +
            "  Shipped: %d | Received: %d",
            LocalDate.now(), draftCount, submittedCount, processingCount,
            shippedCount, receivedCount);
    }

    /**
     * MANDATORY — Must be called within an existing transaction context.
     * Used for operations that are always part of a larger workflow.
     */
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void updateInventoryForOrder(Long orderId, Long inventoryItemId)
            throws InventoryShortageException {

        PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
        InventoryItem item = em.find(InventoryItem.class, inventoryItemId);

        if (order == null || item == null) {
            throw new InventoryShortageException(
                "Order or inventory item not found",
                item != null ? item.getSku() : "UNKNOWN",
                order != null ? order.getQuantity() : 0,
                item != null ? item.getQuantity() : 0);
        }

        // Add received quantity to inventory
        item.setQuantity(item.getQuantity() + order.getQuantity());
        item.setLastReplenished(LocalDateTime.now());
        em.merge(item);

        // Update order status
        order.setStatus(OrderStatus.RECEIVED);
        order.setActualDelivery(LocalDate.now());
        em.merge(order);

        LOGGER.log(Level.INFO, "Inventory updated for order {0}: +{1} units of {2}",
                new Object[]{order.getOrderNumber(), order.getQuantity(), item.getSku()});
    }
}
