package com.globaltrade.ejb.service;

import com.globaltrade.common.dto.ShipmentDTO;
import com.globaltrade.common.entity.Shipment;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateful Session Bean that maintains per-user session state.
 *
 * <p><strong>EJB Best Practice — Stateful Bean Usage:</strong>
 * Stateful beans are used when conversational state must be maintained across
 * multiple method invocations. Here, a user's active tracking list and
 * vendor order assembly are maintained throughout their session.</p>
 *
 * <p><strong>Lifecycle Callbacks Demonstrated:</strong></p>
 * <ul>
 *   <li>{@code @PostConstruct} — Initialize session state</li>
 *   <li>{@code @PrePassivate} — Serialize state before passivation to secondary storage</li>
 *   <li>{@code @PostActivate} — Restore state after activation from secondary storage</li>
 *   <li>{@code @PreDestroy} — Clean up resources before bean removal</li>
 *   <li>{@code @Remove} — Business method that triggers bean destruction</li>
 * </ul>
 *
 * <p><strong>Critical Analysis — Stateful vs Stateless:</strong>
 * Using a Stateful bean here avoids the overhead of re-querying tracked shipments
 * on every request. However, Stateful beans consume more server memory and
 * cannot be pooled like Stateless beans, making them less scalable.
 * For a 50+ country operation, careful session timeout configuration is essential
 * to prevent memory exhaustion.</p>
 */
@Stateful
@RolesAllowed({"LOGISTICS_COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_AGENT",
        "VENDOR_REPRESENTATIVE", "SYSTEM_ADMIN"})
public class UserSessionService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UserSessionService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    private String currentUser;
    private String currentRole;
    private List<String> trackedShipments;
    private List<Long> recentlyViewedVendors;
    private long sessionStartTime;

    /**
     * Initializes session state when the bean is created.
     */
    @PostConstruct
    public void init() {
        this.trackedShipments = new ArrayList<>();
        this.recentlyViewedVendors = new ArrayList<>();
        this.sessionStartTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "User session initialized");
    }

    /**
     * Called before the container passivates (serializes) this bean to secondary storage.
     * Used to release non-serializable resources.
     */
    @PrePassivate
    public void beforePassivation() {
        LOGGER.log(Level.INFO, "Session passivating for user: {0}, " +
                "tracking {1} shipments", new Object[]{currentUser, trackedShipments.size()});
        // EntityManager is automatically handled by the container
    }

    /**
     * Called after the container activates (deserializes) this bean from secondary storage.
     * Used to re-acquire non-serializable resources.
     */
    @PostActivate
    public void afterActivation() {
        LOGGER.log(Level.INFO, "Session activated for user: {0}, " +
                "restoring {1} tracked shipments", new Object[]{currentUser, trackedShipments.size()});
    }

    /**
     * Cleans up resources before bean destruction.
     */
    @PreDestroy
    public void cleanup() {
        long duration = (System.currentTimeMillis() - sessionStartTime) / 1000;
        LOGGER.log(Level.INFO, "Session ending for user: {0}, duration: {1}s",
                new Object[]{currentUser, duration});
    }

    // ======================== Session Management ========================

    public void setUserInfo(String username, String role) {
        this.currentUser = username;
        this.currentRole = role;
        LOGGER.log(Level.INFO, "Session bound to user: {0} ({1})",
                new Object[]{username, role});
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    // ======================== Shipment Tracking ========================

    /**
     * Adds a shipment to the user's active tracking list.
     */
    public void trackShipment(String trackingNumber) {
        if (!trackedShipments.contains(trackingNumber)) {
            trackedShipments.add(trackingNumber);
            LOGGER.log(Level.INFO, "User {0} now tracking shipment: {1}",
                    new Object[]{currentUser, trackingNumber});
        }
    }

    /**
     * Removes a shipment from the tracking list.
     */
    public void untrackShipment(String trackingNumber) {
        trackedShipments.remove(trackingNumber);
    }

    /**
     * Returns the list of currently tracked shipment numbers.
     */
    public List<String> getTrackedShipments() {
        return new ArrayList<>(trackedShipments);
    }

    /**
     * Returns the count of tracked shipments.
     */
    public int getTrackedShipmentCount() {
        return trackedShipments.size();
    }

    // ======================== Recently Viewed ========================

    public void addRecentlyViewedVendor(Long vendorId) {
        recentlyViewedVendors.remove(vendorId); // Remove if already present
        recentlyViewedVendors.add(0, vendorId); // Add to front
        if (recentlyViewedVendors.size() > 10) {
            recentlyViewedVendors.remove(recentlyViewedVendors.size() - 1);
        }
    }

    public List<Long> getRecentlyViewedVendors() {
        return new ArrayList<>(recentlyViewedVendors);
    }

    // ======================== Session Info ========================

    public long getSessionDurationSeconds() {
        return (System.currentTimeMillis() - sessionStartTime) / 1000;
    }

    /**
     * Explicitly ends the user session and triggers bean removal.
     */
    @Remove
    public void endSession() {
        LOGGER.log(Level.INFO, "User {0} explicitly ended session after {1}s",
                new Object[]{currentUser, getSessionDurationSeconds()});
    }
}
