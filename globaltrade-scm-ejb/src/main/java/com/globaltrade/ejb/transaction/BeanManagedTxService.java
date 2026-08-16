package com.globaltrade.ejb.transaction;

import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.PurchaseOrder;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.OrderStatus;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.common.exception.InventoryShortageException;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates Bean-Managed Transaction (BMT) for complex multi-step workflows
 * requiring custom compensation logic.
 *
 * <p><strong>Critical Analysis — BMT vs CMT:</strong></p>
 * <ul>
 *   <li><strong>CMT (Container-Managed)</strong>: Simpler, less error-prone.
 *       The container handles begin/commit/rollback automatically based on
 *       annotations. Preferred for 90% of use cases.</li>
 *   <li><strong>BMT (Bean-Managed)</strong>: Gives explicit control over
 *       transaction boundaries. Necessary when:
 *       <ul>
 *         <li>Multiple independent transactions must be coordinated</li>
 *         <li>Partial success requires custom compensation logic</li>
 *         <li>Transaction boundaries don't align with method boundaries</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Use Case:</strong> Multi-step order fulfilment where:
 * <ol>
 *   <li>Step 1 (Tx 1): Create shipment — must commit to get tracking number</li>
 *   <li>Step 2 (Tx 2): Deduct inventory — may fail due to shortage</li>
 *   <li>Step 3 (Tx 3): Update order status — depends on Step 2 outcome</li>
 * </ol>
 * If Step 2 fails, Step 1's shipment should remain (it was already communicated
 * to the carrier), but Step 3 should mark the order as "PARTIALLY_RECEIVED"
 * rather than rolling back everything.</p>
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
@RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class BeanManagedTxService {

    private static final Logger LOGGER = Logger.getLogger(BeanManagedTxService.class.getName());

    @Resource
    private UserTransaction utx;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Multi-step order fulfilment with explicit transaction management.
     * Demonstrates manual begin(), commit(), and rollback() with
     * compensation logic for partial failures.
     */
    public String processComplexOrderFulfilment(Long orderId, Long inventoryItemId,
                                                String trackingNumber, String carrier)
            throws InventoryShortageException {

        LOGGER.log(Level.INFO, "BMT: Starting complex order fulfilment for order {0}", orderId);

        Shipment shipment = null;
        boolean inventoryDeducted = false;
        StringBuilder result = new StringBuilder();

        // ===== STEP 1: Create Shipment (Transaction 1) =====
        try {
            utx.begin();
            LOGGER.log(Level.INFO, "BMT: Transaction 1 — Creating shipment");

            shipment = new Shipment();
            shipment.setTrackingNumber(trackingNumber);
            shipment.setOriginCountry("Supplier");
            shipment.setOriginCity("Supplier City");
            shipment.setDestinationCountry("Warehouse");
            shipment.setDestinationCity("Warehouse City");
            shipment.setCarrier(carrier);
            shipment.setStatus(ShipmentStatus.CREATED);
            shipment.setEstimatedDelivery(LocalDateTime.now().plusDays(14));

            em.persist(shipment);
            utx.commit(); // Commit shipment creation independently

            result.append("Step 1: Shipment created (").append(trackingNumber).append("). ");
            LOGGER.log(Level.INFO, "BMT: Transaction 1 committed — Shipment created");

        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (Exception re) { /* Log and continue */ }
            LOGGER.log(Level.SEVERE, "BMT: Transaction 1 failed — Shipment creation", e);
            return "FAILED: Shipment creation failed — " + e.getMessage();
        }

        // ===== STEP 2: Deduct Inventory (Transaction 2) =====
        try {
            utx.begin();
            LOGGER.log(Level.INFO, "BMT: Transaction 2 — Deducting inventory");

            PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
            InventoryItem item = em.find(InventoryItem.class, inventoryItemId);

            if (order == null || item == null) {
                utx.rollback();
                result.append("Step 2: Order/Item not found — skipped. ");
            } else if (item.getQuantity() < order.getQuantity()) {
                utx.rollback();
                inventoryDeducted = false;
                result.append("Step 2: Insufficient inventory (need ")
                        .append(order.getQuantity()).append(", have ")
                        .append(item.getQuantity()).append(") — partial fulfilment. ");

                LOGGER.log(Level.WARNING,
                        "BMT: Inventory shortage — proceeding with partial fulfilment");
            } else {
                item.setQuantity(item.getQuantity() - order.getQuantity());
                item.setLastReplenished(LocalDateTime.now());
                em.merge(item);
                utx.commit();
                inventoryDeducted = true;
                result.append("Step 2: Inventory deducted (")
                        .append(order.getQuantity()).append(" units). ");
                LOGGER.log(Level.INFO, "BMT: Transaction 2 committed — Inventory deducted");
            }

        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (Exception re) { /* Log and continue */ }
            LOGGER.log(Level.WARNING, "BMT: Transaction 2 failed — Inventory deduction", e);
            result.append("Step 2: Inventory deduction failed. ");
        }

        // ===== STEP 3: Update Order Status (Transaction 3) =====
        try {
            utx.begin();
            LOGGER.log(Level.INFO, "BMT: Transaction 3 — Updating order status");

            PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
            if (order != null) {
                if (inventoryDeducted) {
                    order.setStatus(OrderStatus.SHIPPED);
                    result.append("Step 3: Order status → SHIPPED.");
                } else {
                    // Compensation: partial fulfilment status
                    order.setStatus(OrderStatus.PARTIALLY_RECEIVED);
                    result.append("Step 3: Order status → PARTIALLY_RECEIVED (compensation).");
                }
                em.merge(order);
                utx.commit();
                LOGGER.log(Level.INFO, "BMT: Transaction 3 committed — Order status updated");
            } else {
                utx.rollback();
                result.append("Step 3: Order not found.");
            }

        } catch (Exception e) {
            try {
                utx.rollback();
            } catch (Exception re) { /* Log and continue */ }
            LOGGER.log(Level.SEVERE, "BMT: Transaction 3 failed — Order status update", e);
            result.append("Step 3: Order status update failed.");
        }

        String summary = result.toString();
        LOGGER.log(Level.INFO, "BMT: Complex fulfilment complete — {0}", summary);
        return summary;
    }
}
