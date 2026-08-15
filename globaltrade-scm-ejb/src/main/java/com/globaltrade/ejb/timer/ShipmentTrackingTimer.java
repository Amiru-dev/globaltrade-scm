package com.globaltrade.ejb.timer;

import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.ejb.service.ShipmentService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Programmatic Timer for automated shipment status tracking.
 *
 * <p><strong>Timer Type: PROGRAMMATIC</strong> — Created using
 * {@code TimerService.createIntervalTimer()} because the tracking interval
 * may need to be adjusted dynamically based on system load or peak seasons.</p>
 *
 * <p><strong>Critical Analysis — Programmatic vs Declarative:</strong></p>
 * <ul>
 *   <li><strong>Programmatic</strong> (chosen here): Allows runtime interval adjustment,
 *       dynamic creation/cancellation, and passing serializable info via TimerConfig.
 *       Essential for shipment tracking where polling frequency might change
 *       during peak shipping seasons (e.g., Q4 holiday season).</li>
 *   <li><strong>Declarative</strong> (@Schedule): Fixed schedule defined at compile time.
 *       Simpler but inflexible — suitable for predictable operations like daily reports.</li>
 * </ul>
 *
 * <p><strong>Persistence: {@code persistent = true}</strong> — Timer survives server
 * restart, ensuring shipment tracking continues even after maintenance windows.
 * This is critical for 99.9% uptime requirements in global logistics.</p>
 */
@Singleton
@Startup
public class ShipmentTrackingTimer {

    private static final Logger LOGGER = Logger.getLogger(ShipmentTrackingTimer.class.getName());
    private static final String TIMER_NAME = "ShipmentTrackingTimer";
    private static final long DEFAULT_INTERVAL_MS = 15 * 60 * 1000; // 15 minutes

    @Resource
    private TimerService timerService;

    @EJB
    private ShipmentService shipmentService;

    /**
     * Creates the programmatic interval timer at startup.
     */
    @PostConstruct
    public void init() {
        // Cancel any existing timer to avoid duplicates on redeploy
        for (Timer timer : timerService.getTimers()) {
            if (TIMER_NAME.equals(timer.getInfo())) {
                timer.cancel();
                LOGGER.log(Level.INFO, "Cancelled existing shipment tracking timer");
            }
        }

        // Create persistent interval timer
        TimerConfig config = new TimerConfig();
        config.setInfo(TIMER_NAME);
        config.setPersistent(true); // Survives server restart

        timerService.createIntervalTimer(
            10000,                  // Initial delay: 10 seconds
            DEFAULT_INTERVAL_MS,    // Interval: 15 minutes
            config
        );

        LOGGER.log(Level.INFO, "Shipment tracking timer created: interval={0}ms, persistent=true",
                DEFAULT_INTERVAL_MS);
    }

    /**
     * Timeout callback — executed every 15 minutes.
     *
     * <p><strong>Performance Optimization:</strong> This method delegates heavy
     * processing to the ShipmentService rather than executing long-running
     * queries directly. This prevents timer thread pool exhaustion and ensures
     * the @Timeout method completes quickly.</p>
     */
    @Timeout
    public void checkShipmentStatuses(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "=== Shipment Tracking Timer Fired ===");

        try {
            // 1. Check for delayed shipments
            List<Shipment> delayedShipments = shipmentService.findDelayedShipments();
            if (!delayedShipments.isEmpty()) {
                LOGGER.log(Level.WARNING, "ALERT: {0} delayed shipments detected!",
                        delayedShipments.size());
                for (Shipment s : delayedShipments) {
                    LOGGER.log(Level.WARNING, "  - Delayed: {0} ({1} → {2}), carrier: {3}",
                            new Object[]{s.getTrackingNumber(), s.getOriginCity(),
                                    s.getDestinationCity(), s.getCarrier()});
                }
            }

            // 2. Check for shipments pending customs
            List<Shipment> pendingCustoms = shipmentService.findPendingCustomsShipments();
            if (!pendingCustoms.isEmpty()) {
                LOGGER.log(Level.INFO, "Shipments at customs: {0}", pendingCustoms.size());
            }

            // 3. Count active shipments for monitoring
            List<Shipment> activeShipments = shipmentService.findActiveShipments();
            LOGGER.log(Level.INFO, "Active shipments: {0}, Delayed: {1}, At Customs: {2}",
                    new Object[]{activeShipments.size(), delayedShipments.size(),
                            pendingCustoms.size()});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in shipment tracking timer", e);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        LOGGER.log(Level.INFO, "Shipment tracking completed in {0}ms. Next: {1}",
                new Object[]{elapsed, timer.getNextTimeout()});
    }

    /**
     * Cancels the timer — useful for maintenance windows.
     */
    public void cancelTimer() {
        for (Timer timer : timerService.getTimers()) {
            if (TIMER_NAME.equals(timer.getInfo())) {
                timer.cancel();
                LOGGER.log(Level.INFO, "Shipment tracking timer cancelled");
            }
        }
    }

    /**
     * Reschedules the timer with a new interval.
     * Demonstrates dynamic interval adjustment capability.
     */
    public void reschedule(long newIntervalMs) {
        cancelTimer();

        TimerConfig config = new TimerConfig();
        config.setInfo(TIMER_NAME);
        config.setPersistent(true);

        timerService.createIntervalTimer(0, newIntervalMs, config);
        LOGGER.log(Level.INFO, "Shipment tracking timer rescheduled: interval={0}ms",
                newIntervalMs);
    }
}
