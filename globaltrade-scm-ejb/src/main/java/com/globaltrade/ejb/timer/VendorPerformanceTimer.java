package com.globaltrade.ejb.timer;

import com.globaltrade.common.entity.PurchaseOrder;
import com.globaltrade.common.entity.Vendor;
import com.globaltrade.common.enums.OrderStatus;
import com.globaltrade.ejb.service.PurchaseOrderService;
import com.globaltrade.ejb.service.VendorManagementService;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Declarative Timer for daily vendor performance assessment.
 *
 * <p><strong>Timer Type: DECLARATIVE (@Schedule)</strong> — Runs daily at 02:00 AM
 * (off-peak hours) to calculate vendor performance ratings based on order
 * fulfilment data. Fixed schedule is appropriate because vendor assessment
 * is a batch operation with a predictable frequency.</p>
 *
 * <p><strong>Persistence: {@code persistent = false}</strong> — Daily assessment
 * can safely be skipped during restarts. The next scheduled execution will
 * recalculate ratings with complete data.</p>
 */
@Singleton
public class VendorPerformanceTimer {

    private static final Logger LOGGER = Logger.getLogger(VendorPerformanceTimer.class.getName());

    @EJB
    private VendorManagementService vendorService;

    @EJB
    private PurchaseOrderService orderService;

    /**
     * Daily vendor performance evaluation at 02:00 AM.
     * Calculates performance scores based on order fulfilment metrics.
     */
    @Schedule(hour = "2", minute = "0", second = "0",
            persistent = false,
            info = "DailyVendorPerformanceAssessment")
    public void evaluateVendorPerformance(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "=== Vendor Performance Timer Fired ===");

        try {
            List<Vendor> allVendors = vendorService.findAllVendors();

            for (Vendor vendor : allVendors) {
                BigDecimal rating = calculatePerformanceRating(vendor);
                vendorService.updatePerformanceRating(vendor.getVendorId(), rating);

                // Supply chain-specific: Flag low-performing vendors for supply chain optimization
                if (rating.compareTo(new BigDecimal("3.0")) < 0) {
                    LOGGER.log(Level.WARNING,
                            "SUPPLY CHAIN RISK: Low-performance vendor {0} ({1}) — Rating: {2}/5.00 — Consider alternative suppliers",
                            new Object[]{vendor.getName(), vendor.getVendorCode(), rating});
                    // Supply chain-specific: Track for contract renewal decisions
                    logContractRenewalConsideration(vendor, rating);
                } else {
                    // Supply chain-specific: High-performing vendors for strategic partnerships
                    LOGGER.log(Level.INFO, "SUPPLY CHAIN OPTIMIZATION: High-performance vendor {0} ({1}) — Rating: {2}/5.00 — Consider partnership expansion",
                            new Object[]{vendor.getName(), vendor.getVendorCode(), rating});
                }
            }

            // Supply chain-specific: Regional vendor performance analysis for global logistics
            analyzeRegionalVendorPerformance(allVendors);

            LOGGER.log(Level.INFO, "Vendor performance evaluation complete: {0} vendors assessed for supply chain optimization",
                    allVendors.size());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in vendor performance evaluation", e);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO, "Vendor performance assessment completed in {0}ms", elapsed);
    }

    /**
     * Calculates a performance rating (0.00 - 5.00) based on order fulfilment metrics.
     *
     * <p>Rating formula considers:
     * <ul>
     *   <li>On-time delivery rate (40% weight)</li>
     *   <li>Order completion rate (35% weight)</li>
     *   <li>Compliance status bonus (25% weight)</li>
     * </ul></p>
     */
    private BigDecimal calculatePerformanceRating(Vendor vendor) {
        List<PurchaseOrder> vendorOrders = orderService.findByVendor(vendor.getVendorId());

        if (vendorOrders.isEmpty()) {
            return vendor.getPerformanceRating() != null
                    ? vendor.getPerformanceRating()
                    : new BigDecimal("3.00");
        }

        // Calculate on-time delivery rate
        long totalOrders = vendorOrders.size();
        long completedOnTime = vendorOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.RECEIVED || o.getStatus() == OrderStatus.PAID)
                .filter(o -> o.getActualDelivery() != null && o.getExpectedDelivery() != null
                        && !o.getActualDelivery().isAfter(o.getExpectedDelivery()))
                .count();

        // Calculate completion rate
        long completedOrders = vendorOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.RECEIVED || o.getStatus() == OrderStatus.PAID)
                .count();

        double onTimeRate = totalOrders > 0 ? (double) completedOnTime / totalOrders : 0.5;
        double completionRate = totalOrders > 0 ? (double) completedOrders / totalOrders : 0.5;

        // Compliance bonus
        double complianceBonus = switch (vendor.getComplianceStatus()) {
            case COMPLIANT -> 1.0;
            case CONDITIONALLY_APPROVED -> 0.7;
            case UNDER_REVIEW -> 0.5;
            case PENDING -> 0.4;
            default -> 0.2;
        };

        // Weighted score (max 5.0)
        double weightedScore = (onTimeRate * 0.40 + completionRate * 0.35
                + complianceBonus * 0.25) * 5.0;

        return BigDecimal.valueOf(weightedScore).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Supply chain-specific: Log contract renewal consideration for vendor management.
     */
    private void logContractRenewalConsideration(Vendor vendor, BigDecimal rating) {
        LOGGER.log(Level.INFO, "CONTRACT RENEWAL CONSIDERATION: Vendor {0} — Rating: {1} — Review contract terms for performance improvement",
                new Object[]{vendor.getName(), rating});
    }

    /**
     * Supply chain-specific: Analyze regional vendor performance for global logistics optimization.
     */
    private void analyzeRegionalVendorPerformance(List<Vendor> vendors) {
        // Group vendors by country and analyze regional performance patterns
        vendors.stream()
            .collect(java.util.stream.Collectors.groupingBy(Vendor::getCountry))
            .forEach((country, countryVendors) -> {
                double avgRating = countryVendors.stream()
                        .map(v -> v.getPerformanceRating() != null ? v.getPerformanceRating().doubleValue() : 3.0)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(3.0);
                LOGGER.log(Level.INFO, "REGIONAL VENDOR PERFORMANCE: {0} — Average rating: {1}/5.00 — {2} vendors",
                        new Object[]{country, avgRating, countryVendors.size()});
            });
    }
}
