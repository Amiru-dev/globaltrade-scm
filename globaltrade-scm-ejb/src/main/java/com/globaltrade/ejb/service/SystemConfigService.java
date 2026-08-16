package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.CarrierRoute;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton Session Bean for global system configuration and in-memory caching.
 *
 * <p><strong>EJB Best Practice — Singleton Pattern:</strong>
 * {@code @Singleton} ensures exactly one instance exists per application.
 * {@code @Startup} initialises the bean eagerly at deployment time.
 * {@code @ConcurrencyManagement(CONTAINER)} lets the container manage
 * concurrent access using {@code @Lock(READ)} and {@code @Lock(WRITE)}.</p>
 *
 * <p><strong>Critical Analysis — Concurrency Control:</strong>
 * READ locks allow concurrent access for cache reads (high throughput),
 * while WRITE locks provide exclusive access for cache updates (data consistency).
 * This is crucial for a globally distributed system where multiple concurrent
 * users may read configuration simultaneously, but updates must be serialised.</p>
 */
@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@RolesAllowed({"SYSTEM_ADMIN", "LOGISTICS_COORDINATOR"})
public class SystemConfigService {

    private static final Logger LOGGER = Logger.getLogger(SystemConfigService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    // In-memory configuration cache
    private Map<String, String> systemConfig;

    // In-memory carrier route cache for fast lookups
    private Map<String, List<CarrierRoute>> routeCache;

    // Performance metrics
    private final Map<String, Long> methodExecutionCounts = new ConcurrentHashMap<>();

    /**
     * Initialises the singleton at application startup.
     * Loads configuration and populates caches.
     */
    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initialising GlobalTrade System Configuration...");

        // Initialise system configuration with defaults
        systemConfig = new HashMap<>();
        systemConfig.put("app.name", "GlobalTrade SCM");
        systemConfig.put("app.version", "1.0.0");
        systemConfig.put("timer.shipment.interval.minutes", "15");
        systemConfig.put("timer.inventory.interval.hours", "1");
        systemConfig.put("timer.vendor.cron", "0 2 * * *");
        systemConfig.put("session.timeout.minutes", "30");
        systemConfig.put("security.max.login.attempts", "5");
        systemConfig.put("alert.email.enabled", "true");
        systemConfig.put("customs.deadline.warning.days", "3");
        systemConfig.put("vendor.low.performance.threshold", "3.0");

        // Initialise route cache
        routeCache = new HashMap<>();
        loadRouteCache();

        LOGGER.log(Level.INFO, "System configuration initialised with {0} properties",
                systemConfig.size());
    }

    /**
     * Loads carrier routes into the in-memory cache.
     */
    private void loadRouteCache() {
        try {
            List<CarrierRoute> allRoutes = em.createNamedQuery(
                    "CarrierRoute.findAll", CarrierRoute.class).getResultList();

            routeCache.clear();
            for (CarrierRoute route : allRoutes) {
                String key = route.getOrigin() + "→" + route.getDestination();
                routeCache.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(route);
            }

            LOGGER.log(Level.INFO, "Route cache loaded: {0} routes across {1} corridors",
                    new Object[]{allRoutes.size(), routeCache.size()});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load route cache — will retry on next access", e);
        }
    }

    // ======================== Configuration Access (READ) ========================

    /**
     * Retrieves a configuration value. READ lock allows concurrent access.
     */
    @Lock(LockType.READ)
    public String getConfig(String key) {
        return systemConfig.getOrDefault(key, null);
    }

    /**
     * Retrieves a configuration value with a default fallback.
     */
    @Lock(LockType.READ)
    public String getConfig(String key, String defaultValue) {
        return systemConfig.getOrDefault(key, defaultValue);
    }

    /**
     * Returns an integer configuration value.
     */
    @Lock(LockType.READ)
    public int getConfigInt(String key, int defaultValue) {
        String value = systemConfig.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns all configuration as an unmodifiable map.
     */
    @Lock(LockType.READ)
    public Map<String, String> getAllConfig() {
        return Collections.unmodifiableMap(systemConfig);
    }

    // ======================== Configuration Updates (WRITE) ========================

    /**
     * Updates a configuration value. WRITE lock provides exclusive access.
     */
    @Lock(LockType.WRITE)
    @RolesAllowed({"SYSTEM_ADMIN"})
    public void setConfig(String key, String value) {
        String oldValue = systemConfig.put(key, value);
        LOGGER.log(Level.INFO, "Config updated: {0} = {1} (was: {2})",
                new Object[]{key, value, oldValue});
    }

    // ======================== Route Cache Access ========================

    /**
     * Retrieves cached routes for a given corridor. READ lock for concurrent access.
     */
    @Lock(LockType.READ)
    public List<CarrierRoute> getCachedRoutes(String origin, String destination) {
        String key = origin + "→" + destination;
        return routeCache.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Forces a reload of the route cache. WRITE lock for exclusive access.
     */
    @Lock(LockType.WRITE)
    @RolesAllowed({"SYSTEM_ADMIN"})
    public void refreshRouteCache() {
        LOGGER.log(Level.INFO, "Refreshing route cache...");
        loadRouteCache();
    }

    // ======================== Metrics ========================

    @Lock(LockType.READ)
    public Map<String, Long> getMethodExecutionCounts() {
        return Collections.unmodifiableMap(methodExecutionCounts);
    }

    @Lock(LockType.WRITE)
    public void recordMethodExecution(String methodName) {
        methodExecutionCounts.merge(methodName, 1L, Long::sum);
    }
}
