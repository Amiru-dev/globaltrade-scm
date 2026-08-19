package com.globaltrade.ejb.timer;

import com.globaltrade.common.dto.InventoryAlertDTO;
import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.ejb.service.InventoryService;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Declarative Timer for automated inventory level monitoring.
 *
 * <p><strong>Timer Type: DECLARATIVE (@Schedule)</strong> — Uses the {@code @Schedule}
 * annotation because inventory monitoring runs on a fixed hourly schedule that
 * does not need to change at runtime.</p>
 *
 * <p><strong>Critical Analysis — Declarative Timer Advantages:</strong></p>
 * <ul>
 *   <li>No boilerplate code for timer creation — annotation-driven</li>
 *   <li>Automatically created by the container at deployment</li>
 *   <li>{@code persistent = false} chosen here because inventory checks are
 *       idempotent — missing one check during a restart is acceptable as
 *       the next scheduled check will catch any issues</li>
 *   <li>Fixed schedule aligns with warehouse operational rhythms</li>
 * </ul>
 */
@Singleton
@Startup
public class InventoryMonitorTimer {

    private static final Logger LOGGER = Logger.getLogger(InventoryMonitorTimer.class.getName());

    @EJB
    private InventoryService inventoryService;

    // Stores recent alerts for dashboard display
    private final List<InventoryAlertDTO> recentAlerts = new ArrayList<>();

    /**
     * Runs every hour to check inventory levels across all warehouses.
     *
     * <p>{@code persistent = false}: Timer is recreated on server restart.
     * Inventory checks are idempotent, so missing one during restart is safe.</p>
     */
    @Schedule(hour = "*", minute = "0", second = "0",
            persistent = false,
            info = "HourlyInventoryMonitor")
    public void monitorInventoryLevels(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "=== Inventory Monitor Timer Fired ===");

        try {
            // Clear previous alerts
            recentAlerts.clear();

            // Check for low-stock items with supply chain-specific vendor analysis
            List<InventoryItem> lowStockItems = inventoryService.findLowStockItems();
            for (InventoryItem item : lowStockItems) {
                InventoryAlertDTO alert = new InventoryAlertDTO();
                alert.setItemId(item.getItemId());
                alert.setSku(item.getSku());
                alert.setProductName(item.getProductName());
                alert.setWarehouseId(item.getWarehouseId());
                alert.setCurrentQuantity(item.getQuantity());
                alert.setReorderLevel(item.getReorderLevel());
                alert.setAlertTimestamp(LocalDateTime.now());

                if (item.isOutOfStock()) {
                    alert.setAlertType("OUT_OF_STOCK");
                    alert.setSeverity("CRITICAL");
                    alert.setSuggestedAction("Immediate reorder required — production may halt");
                    LOGGER.log(Level.SEVERE, "CRITICAL: OUT OF STOCK — SKU={0}, Warehouse={1}",
                            new Object[]{item.getSku(), item.getWarehouseId()});
                    // Supply chain-specific: Alert for vendor performance evaluation
                    logVendorPerformanceImpact(item.getSku(), "STOCKOUT_RISK");
                } else {
                    alert.setAlertType("LOW_STOCK");
                    alert.setSeverity("WARNING");
                    alert.setSuggestedAction("Initiate replenishment order with primary vendor");
                    LOGGER.log(Level.WARNING, "LOW STOCK — SKU={0}, qty={1}/{2}, Warehouse={3}",
                            new Object[]{item.getSku(), item.getQuantity(),
                                    item.getReorderLevel(), item.getWarehouseId()});
                    // Supply chain-specific: Track for vendor performance analysis
                    logVendorPerformanceImpact(item.getSku(), "LOW_STOCK");
                }

                recentAlerts.add(alert);
            }

            // Supply chain-specific: Check for excess inventory for space optimization
            List<InventoryItem> excessInventory = inventoryService.findExcessInventory();
            for (InventoryItem item : excessInventory) {
                LOGGER.log(Level.INFO, "INVENTORY OPTIMIZATION: Excess stock detected — SKU={0}, qty={1}, consider reducing order quantities",
                        new Object[]{item.getSku(), item.getQuantity()});
            }

            // Summary logging
            long outOfStock = recentAlerts.stream()
                    .filter(a -> "OUT_OF_STOCK".equals(a.getAlertType())).count();
            long lowStock = recentAlerts.stream()
                    .filter(a -> "LOW_STOCK".equals(a.getAlertType())).count();

            LOGGER.log(Level.INFO,
                    "Inventory check complete: {0} alerts (Critical: {1}, Warning: {2}), Excess: {3}",
                    new Object[]{recentAlerts.size(), outOfStock, lowStock, excessInventory.size()});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in inventory monitoring timer", e);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO, "Inventory monitoring completed in {0}ms", elapsed);
    }

    /**
     * Returns the most recent inventory alerts for dashboard display.
     */
    public List<InventoryAlertDTO> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }

    /**
     * Supply chain-specific: Log vendor performance impact for evaluation.
     */
    private void logVendorPerformanceImpact(String sku, String impactType) {
        LOGGER.log(Level.INFO, "VENDOR PERFORMANCE IMPACT: SKU={0} — Type: {1}, Tracking for supply chain analysis",
                new Object[]{sku, impactType});
    }
}
