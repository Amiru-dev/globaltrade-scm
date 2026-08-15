package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.PurchaseOrder;
import com.globaltrade.common.enums.OrderStatus;
import com.globaltrade.common.exception.SupplyChainException;
import com.globaltrade.common.exception.TransactionRollbackException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Session Bean for purchase order lifecycle management.
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "SYSTEM_ADMIN"})
public class PurchaseOrderService {

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PurchaseOrder createOrder(PurchaseOrder order) {
        LOGGER.log(Level.INFO, "Creating purchase order: {0}", order.getOrderNumber());
        em.persist(order);
        return order;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public PurchaseOrder updateOrderStatus(Long orderId, OrderStatus newStatus)
            throws TransactionRollbackException {

        PurchaseOrder order = em.find(PurchaseOrder.class, orderId);
        if (order == null) {
            throw new TransactionRollbackException(
                "Purchase order not found: " + orderId,
                "PO-UPDATE-" + orderId);
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        PurchaseOrder updated = em.merge(order);

        LOGGER.log(Level.INFO, "Order {0} status: {1} → {2}",
                new Object[]{order.getOrderNumber(), oldStatus, newStatus});
        return updated;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public PurchaseOrder findById(Long orderId) {
        return em.find(PurchaseOrder.class, orderId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<PurchaseOrder> findByVendor(Long vendorId) {
        return em.createNamedQuery("PurchaseOrder.findByVendor", PurchaseOrder.class)
                .setParameter("vendorId", vendorId)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<PurchaseOrder> findByStatus(OrderStatus status) {
        return em.createNamedQuery("PurchaseOrder.findByStatus", PurchaseOrder.class)
                .setParameter("status", status)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<PurchaseOrder> findAllOrders() {
        return em.createNamedQuery("PurchaseOrder.findAll", PurchaseOrder.class)
                .getResultList();
    }

    /**
     * Finds vendor orders — allows vendor reps to see their own orders.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed({"VENDOR_REPRESENTATIVE", "LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public List<PurchaseOrder> findVendorOrders(Long vendorId) {
        return em.createNamedQuery("PurchaseOrder.findByVendor", PurchaseOrder.class)
                .setParameter("vendorId", vendorId)
                .getResultList();
    }
}
