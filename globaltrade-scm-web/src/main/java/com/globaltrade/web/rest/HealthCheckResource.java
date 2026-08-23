package com.globaltrade.web.rest;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Health check endpoint for application monitoring.
 *
 * <p><strong>Critical Analysis — Health Check Design:</strong></p>
 * <ul>
 *   <li><strong>Database Connectivity:</strong> Tests datasource connection
 *       to verify database availability and configuration.</li>
 *   <li><strong>Application Status:</strong> Returns overall health status
 *       for load balancer health checks and monitoring systems.</li>
 *   <li><strong>Monitoring Integration:</strong> Can be integrated with
 *       Prometheus, Grafana, or other monitoring systems.</li>
 *   <li><strong>Load Balancer Integration:</strong> Simple JSON response
 *       suitable for automated health checks by load balancers.</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Tests database connectivity via JNDI datasource,
 * providing early detection of database connection issues.</p>
 */
@Path("/health")
@Stateless
public class HealthCheckResource {

    private static final Logger LOGGER = Logger.getLogger(HealthCheckResource.class.getName());

    @Resource(lookup = "jdbc/GlobalTradeDS")
    private DataSource dataSource;

    /**
     * Performs health check and returns application status.
     *
     * <p><strong>Critical Analysis:</strong> Tests database connectivity
     * and returns JSON status for monitoring systems. Simple structure
     * enables easy integration with load balancers and monitoring tools.</p>
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        Map<String, Object> healthStatus = new HashMap<>();
        boolean overallHealthy = true;

        // Check database connectivity
        boolean databaseHealthy = checkDatabase();
        healthStatus.put("database", databaseHealthy ? "UP" : "DOWN");

        if (!databaseHealthy) {
            overallHealthy = false;
        }

        // Check JVM memory
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Map<String, Object> memoryInfo = new HashMap<>();
        memoryInfo.put("max", maxMemory / (1024 * 1024) + " MB");
        memoryInfo.put("total", totalMemory / (1024 * 1024) + " MB");
        memoryInfo.put("used", usedMemory / (1024 * 1024) + " MB");
        memoryInfo.put("free", freeMemory / (1024 * 1024) + " MB");
        memoryInfo.put("usage_percent", (usedMemory * 100) / maxMemory);

        healthStatus.put("memory", memoryInfo);

        // Overall status
        healthStatus.put("status", overallHealthy ? "UP" : "DOWN");
        healthStatus.put("timestamp", System.currentTimeMillis());

        int responseStatus = overallHealthy ? Response.Status.OK.getStatusCode() : Response.Status.SERVICE_UNAVAILABLE.getStatusCode();

        return Response.status(responseStatus)
                .entity(healthStatus)
                .build();
    }

    /**
     * Checks database connectivity.
     *
     * <p><strong>Critical Analysis:</strong> Tests datasource connection
     * with timeout to prevent hanging on database issues.</p>
     */
    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) { // 5 second timeout
                LOGGER.log(Level.FINE, "Database health check: UP");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Database health check: DOWN (connection invalid)");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database health check failed", e);
            return false;
        }
    }

    /**
     * Simple health check for load balancers (returns HTTP 200 if healthy).
     *
     * <p><strong>Critical Analysis:</strong> Simple endpoint for load balancer
     * health checks that only needs HTTP status code.</p>
     */
    @GET
    @Path("/simple")
    @Produces(MediaType.TEXT_PLAIN)
    public Response simpleHealthCheck() {
        if (checkDatabase()) {
            return Response.ok("OK").build();
        } else {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("Service Unavailable")
                    .build();
        }
    }
}