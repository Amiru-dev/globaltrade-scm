package com.globaltrade.ejb.timer;

import com.globaltrade.common.entity.CarrierRoute;
import com.globaltrade.ejb.service.RouteOptimizationService;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Programmatic Single-Action Timer for on-demand route optimization.
 *
 * <p><strong>Timer Type: PROGRAMMATIC (Single-Action)</strong> — Uses
 * {@code TimerService.createSingleActionTimer()} because route optimization
 * is triggered on-demand when carrier costs change, rather than on a fixed schedule.</p>
 *
 * <p><strong>Persistence: {@code persistent = false}</strong> — Route optimization
 * is a calculation that can be redone at any time. If the server restarts,
 * the optimization will be triggered by the next cost change event rather
 * than replaying old timer firings.</p>
 *
 * <p><strong>Critical Analysis — Single-Action vs Interval Timer:</strong>
 * Single-action timers fire exactly once and are automatically removed.
 * This prevents unnecessary recalculations. The timer is created reactively
 * when data changes, rather than proactively on a schedule.</p>
 */
@Singleton
public class RouteOptimizationTimer {

    private static final Logger LOGGER = Logger.getLogger(RouteOptimizationTimer.class.getName());
    private static final String TIMER_NAME = "RouteOptimization";

    @Resource
    private TimerService timerService;

    @EJB
    private RouteOptimizationService routeService;

    /**
     * Schedules a one-time route optimization to run after a brief delay.
     * The delay allows batching multiple cost-change events into a single recalculation.
     *
     * @param delayMs delay in milliseconds before optimization runs (default: 5000ms)
     */
    public void scheduleOptimization(long delayMs) {
        // Cancel any pending optimization to debounce rapid changes
        for (Timer timer : timerService.getTimers()) {
            if (TIMER_NAME.equals(timer.getInfo())) {
                timer.cancel();
                LOGGER.log(Level.INFO, "Cancelled pending route optimization — rescheduling");
            }
        }

        TimerConfig config = new TimerConfig();
        config.setInfo(TIMER_NAME);
        config.setPersistent(false); // No need to persist one-shot calculations

        timerService.createSingleActionTimer(delayMs, config);
        LOGGER.log(Level.INFO, "Route optimization scheduled in {0}ms", delayMs);
    }

    /**
     * Convenience method with default 5-second delay.
     */
    public void scheduleOptimization() {
        scheduleOptimization(5000);
    }

    /**
     * Timeout callback — performs the actual route optimization.
     */
    @Timeout
    public void performOptimization(Timer timer) {
        long startTime = System.currentTimeMillis();
        LOGGER.log(Level.INFO, "=== Route Optimization Timer Fired ===");

        try {
            List<CarrierRoute> allRoutes = routeService.findAllRoutes();

            // Group routes by corridor and find optimal for each
            allRoutes.stream()
                    .map(r -> r.getOrigin() + "→" + r.getDestination())
                    .distinct()
                    .forEach(corridor -> {
                        String[] parts = corridor.split("→");
                        CarrierRoute optimal = routeService.findOptimalRoute(parts[0], parts[1]);
                        if (optimal != null) {
                            LOGGER.log(Level.INFO, "Optimal route {0}: {1} via {2} " +
                                            "(${3}/kg, {4} days, reliability={5})",
                                    new Object[]{corridor, optimal.getRouteCode(),
                                            optimal.getCarrier(), optimal.getCostPerKg(),
                                            optimal.getTransitDays(),
                                            optimal.getReliabilityScore()});
                        }
                    });

            long elapsed = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.INFO, "Route optimization completed in {0}ms. " +
                    "Analysed {1} routes.", new Object[]{elapsed, allRoutes.size()});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in route optimization", e);
        }
    }
}
