package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.common.exception.ShipmentTrackingException;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Session Bean for shipment management operations.
 *
 * <p><strong>EJB Best Practice — Stateless Bean Selection:</strong>
 * Shipment operations are inherently stateless — each request is independent
 * and does not depend on prior interactions. Using {@code @Stateless} enables
 * the container to pool bean instances, reducing object creation overhead
 * and improving throughput under concurrent global operations.</p>
 *
 * <p><strong>Transaction Strategy:</strong> Uses Container-Managed Transactions (CMT)
 * with varying attributes per method based on operation semantics.</p>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "SYSTEM_ADMIN"})
public class ShipmentService {

    private static final Logger LOGGER = Logger.getLogger(ShipmentService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Creates a new shipment record.
     * REQUIRED: Must execute within a transaction (creates or joins existing).
     *
     * <p><strong>Transaction Timeout Analysis:</strong> Standard REQUIRED transaction for shipment
     * creation. Timeout can be configured via ejb-jar.xml if needed.</p>
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment createShipment(Shipment shipment) {
        LOGGER.log(Level.INFO, "Creating shipment: {0}", shipment.getTrackingNumber());
        em.persist(shipment);
        return shipment;
    }

    /**
     * Updates the status of an existing shipment.
     * REQUIRED: Status transitions must be atomic with audit logging.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Shipment updateShipmentStatus(Long shipmentId, ShipmentStatus newStatus)
            throws ShipmentTrackingException {

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentTrackingException(
                    "Shipment not found with ID: " + shipmentId,
                    String.valueOf(shipmentId));
        }

        ShipmentStatus oldStatus = shipment.getStatus();
        shipment.setStatus(newStatus);

        // Auto-set delivery timestamp when status changes to DELIVERED
        if (newStatus == ShipmentStatus.DELIVERED && shipment.getActualDelivery() == null) {
            shipment.setActualDelivery(LocalDateTime.now());
        }

        // Auto-set customs cleared flag
        if (newStatus == ShipmentStatus.CUSTOMS_CLEARED) {
            shipment.setCustomsCleared(true);
        }

        Shipment updated = em.merge(shipment);
        LOGGER.log(Level.INFO, "Shipment {0} status changed: {1} → {2}",
                new Object[]{shipment.getTrackingNumber(), oldStatus, newStatus});

        return updated;
    }

    /**
     * Retrieves shipment by ID.
     * SUPPORTS: Read-only — participates in existing tx but doesn't require one.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_AGENT",
            "VENDOR_REPRESENTATIVE", "SYSTEM_ADMIN"})
    public Shipment findById(Long shipmentId) {
        return em.find(Shipment.class, shipmentId);
    }

    /**
     * Finds shipment by tracking number — used for public tracking portal.
     * SUPPORTS: No transaction needed for reads.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_AGENT",
            "VENDOR_REPRESENTATIVE", "SYSTEM_ADMIN"})
    public Shipment findByTrackingNumber(String trackingNumber) throws ShipmentTrackingException {
        TypedQuery<Shipment> query = em.createQuery(
                "SELECT s FROM Shipment s WHERE s.trackingNumber = :tn", Shipment.class);
        query.setParameter("tn", trackingNumber);
        List<Shipment> results = query.getResultList();

        if (results.isEmpty()) {
            throw new ShipmentTrackingException(
                    "No shipment found with tracking number: " + trackingNumber,
                    trackingNumber);
        }
        return results.get(0);
    }

    /**
     * Retrieves all active (non-delivered, non-cancelled) shipments.
     * SUPPORTS: Read-only query.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<Shipment> findActiveShipments() {
        return em.createNamedQuery("Shipment.findActiveShipments", Shipment.class)
                .getResultList();
    }

    /**
     * Retrieves all shipments with a specific status.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> findByStatus(ShipmentStatus status) {
        return em.createNamedQuery("Shipment.findByStatus", Shipment.class)
                .setParameter("status", status)
                .getResultList();
    }

    /**
     * Retrieves all delayed shipments — used by timer services for alerts.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<Shipment> findDelayedShipments() {
        return em.createNamedQuery("Shipment.findDelayed", Shipment.class)
                .getResultList();
    }

    /**
     * Retrieves shipments pending customs clearance.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<Shipment> findPendingCustomsShipments() {
        return em.createNamedQuery("Shipment.findPendingCustoms", Shipment.class)
                .getResultList();
    }

    /**
     * Supply chain-specific: Find high-value shipments for route optimization.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<Shipment> findHighValueShipments(double threshold) {
        return em.createQuery(
                "SELECT s FROM Shipment s WHERE s.declaredValue >= :threshold", Shipment.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    /**
     * Supply chain-specific: Find shipments by carrier for recovery operations.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<Shipment> findByCarrier(String carrier) {
        return em.createQuery(
                "SELECT s FROM Shipment s WHERE s.carrier = :carrier", Shipment.class)
                .setParameter("carrier", carrier)
                .getResultList();
    }

    /**
     * Supply chain-specific: Find shipments by route for weather recovery operations.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @PermitAll
    public List<Shipment> findByRoute(String route) {
        return em.createQuery(
                "SELECT s FROM Shipment s WHERE s.originCity = :route OR s.destinationCity = :route", Shipment.class)
                .setParameter("route", route)
                .getResultList();
    }

    /**
     * Deletes a shipment — only allowed for CANCELLED shipments.
     * REQUIRED: Deletion must be transactional.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @RolesAllowed({"SYSTEM_ADMIN"})
    public void deleteShipment(Long shipmentId) throws ShipmentTrackingException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new ShipmentTrackingException(
                    "Shipment not found for deletion: " + shipmentId,
                    String.valueOf(shipmentId));
        }
        if (shipment.getStatus() != ShipmentStatus.CANCELLED) {
            throw new ShipmentTrackingException(
                    "Only CANCELLED shipments can be deleted. Current status: " + shipment.getStatus(),
                    shipment.getTrackingNumber());
        }
        em.remove(shipment);
        LOGGER.log(Level.INFO, "Shipment deleted: {0}", shipment.getTrackingNumber());
    }

    /**
     * Counts total shipments by status — for dashboard reporting.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long countByStatus(ShipmentStatus status) {
        return em.createQuery(
                        "SELECT COUNT(s) FROM Shipment s WHERE s.status = :status", Long.class)
                .setParameter("status", status)
                .getSingleResult();
    }
}
