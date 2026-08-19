package com.globaltrade.web.rest;

import com.globaltrade.ejb.interceptor.PerformanceMonitor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST endpoint for accessing performance monitoring metrics.
 *
 * <p><strong>Performance Monitoring Dashboard:</strong></p>
 * <ul>
 *   <li>Real-time performance metrics for all business methods</li>
 *   <li>Historical performance trends and analysis</li>
 *   <li>Performance threshold violation alerts</li>
 *   <li>Method execution time distribution analysis</li>
 * </ul>
 *
 * <p><strong>Security:</strong> Admin-only access to sensitive performance data.
 */
@Path("/performance")
@RolesAllowed({"SYSTEM_ADMIN"})
public class PerformanceResource {

    private static final Logger LOGGER = Logger.getLogger(PerformanceResource.class.getName());

    /**
     * Returns all collected performance metrics.
     *
     * <p><strong>Response Format:</strong></p>
     * <pre>
     * {
     *   "invocationCounts": {
     *     "VendorManagementService.findAllVendors": 150,
     *     "ShipmentService.createShipment": 45
     *   },
     *   "averageExecutionTimes": {
     *     "VendorManagementService.findAllVendors": 125.5,
     *     "ShipmentService.createShipment": 89.2
     *   },
     *   "maxExecutionTimes": {
     *     "VendorManagementService.findAllVendors": 2500.0,
     *     "ShipmentService.createShipment": 450.0
     *   }
     * }
     * </pre>
     */
    @GET
    @Path("/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMetrics() {
        LOGGER.log(Level.INFO, "PERFORMANCE: Admin requested all performance metrics");

        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Convert AtomicLong maps to regular Long maps for JSON serialization
            Map<String, Long> invocationCounts = new HashMap<>();
            PerformanceMonitor.getAllInvocationCounts().forEach((key, value) -> {
                invocationCounts.put(key, value.get());
            });

            Map<String, Double> averageTimes = new HashMap<>();
            for (String key : invocationCounts.keySet()) {
                averageTimes.put(key, PerformanceMonitor.getAverageExecutionTime(key));
            }

            Map<String, Long> maxTimes = new HashMap<>();
            for (String key : invocationCounts.keySet()) {
                maxTimes.put(key, PerformanceMonitor.getMaxExecutionTime(key));
            }

            metrics.put("invocationCounts", invocationCounts);
            metrics.put("averageExecutionTimes", averageTimes);
            metrics.put("maxExecutionTimes", maxTimes);
            metrics.put("timestamp", System.currentTimeMillis());

            return Response.ok(metrics).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PERFORMANCE: Error retrieving metrics", e);
            return Response.serverError()
                    .entity("{\"error\": \"Failed to retrieve performance metrics\"}")
                    .build();
        }
    }

    /**
     * Returns performance metrics for a specific method.
     *
     * @param methodKey The method key (e.g., "VendorManagementService.findAllVendors")
     */
    @GET
    @Path("/method/{methodKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMethodMetrics(@PathParam("methodKey") String methodKey) {
        LOGGER.log(Level.INFO, "PERFORMANCE: Admin requested metrics for method: {0}", methodKey);

        try {
            Map<String, Object> methodMetrics = new HashMap<>();
            
            methodMetrics.put("methodKey", methodKey);
            methodMetrics.put("invocationCount", PerformanceMonitor.getInvocationCount(methodKey));
            methodMetrics.put("averageExecutionTime", PerformanceMonitor.getAverageExecutionTime(methodKey));
            methodMetrics.put("maxExecutionTime", PerformanceMonitor.getMaxExecutionTime(methodKey));
            methodMetrics.put("timestamp", System.currentTimeMillis());

            // Performance assessment
            double avgTime = PerformanceMonitor.getAverageExecutionTime(methodKey);
            String performanceAssessment;
            if (avgTime < 100) {
                performanceAssessment = "EXCELLENT";
            } else if (avgTime < 500) {
                performanceAssessment = "GOOD";
            } else if (avgTime < 1000) {
                performanceAssessment = "WARNING";
            } else {
                performanceAssessment = "CRITICAL";
            }
            methodMetrics.put("performanceAssessment", performanceAssessment);

            return Response.ok(methodMetrics).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PERFORMANCE: Error retrieving method metrics", e);
            return Response.serverError()
                    .entity("{\"error\": \"Failed to retrieve method metrics\"}")
                    .build();
        }
    }

    /**
     * Resets all performance metrics (admin only).
     *
     * <p><strong>Use Case:</strong> Clear metrics after performance tuning or
     * to start fresh monitoring period.</p>
     */
    @GET
    @Path("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetMetrics() {
        LOGGER.log(Level.INFO, "PERFORMANCE: Admin requested metrics reset");

        try {
            PerformanceMonitor.resetMetrics();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All performance metrics have been reset");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));

            return Response.ok(response).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PERFORMANCE: Error resetting metrics", e);
            return Response.serverError()
                    .entity("{\"error\": \"Failed to reset performance metrics\"}")
                    .build();
        }
    }

    /**
     * Returns performance summary report.
     *
     * <p><strong>Summary Includes:</strong></p>
 * <ul>
     *   <li>Total method invocations</li>
     *   <li>Average execution time across all methods</li>
     *   <li>Performance threshold violations</li>
     *   <li>Top 5 slowest methods</li>
     * </ul>
     */
    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformanceSummary() {
        LOGGER.log(Level.INFO, "PERFORMANCE: Admin requested performance summary");

        try {
            Map<String, Object> summary = new HashMap<>();
            
            Map<String, Long> invocationCounts = new HashMap<>();
            PerformanceMonitor.getAllInvocationCounts().forEach((key, value) -> {
                invocationCounts.put(key, value.get());
            });

            // Calculate totals
            long totalInvocations = invocationCounts.values().stream().mapToLong(Long::longValue).sum();
            double totalAvgTime = 0.0;
            int methodCount = 0;

            for (String key : invocationCounts.keySet()) {
                double avgTime = PerformanceMonitor.getAverageExecutionTime(key);
                totalAvgTime += avgTime;
                methodCount++;
            }

            if (methodCount > 0) {
                totalAvgTime = totalAvgTime / methodCount;
            }

            // Find slowest methods
            Map<String, Long> maxTimes = new HashMap<>();
            for (String key : invocationCounts.keySet()) {
                maxTimes.put(key, PerformanceMonitor.getMaxExecutionTime(key));
            }

            // Top 5 slowest methods
            Map<String, Long> slowestMethods = maxTimes.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);

            summary.put("totalInvocations", totalInvocations);
            summary.put("totalMethods", methodCount);
            summary.put("averageExecutionTime", totalAvgTime);
            summary.put("slowestMethods", slowestMethods);
            summary.put("timestamp", System.currentTimeMillis());

            return Response.ok(summary).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PERFORMANCE: Error generating summary", e);
            return Response.serverError()
                    .entity("{\"error\": \"Failed to generate performance summary\"}")
                    .build();
        }
    }

    /**
     * Returns real-time performance status for dashboard display.
     *
     * <p><strong>Status Indicators:</strong></p>
 * <ul>
     *   <li>Overall system health (GREEN/YELLOW/RED)</li>
     *   *   <li>Active method count</li>
     *   <li>Recent performance trends</li>
     *   <li>Threshold violation count</li>
     * </ul>
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPerformanceStatus() {
        LOGGER.log(Level.FINE, "PERFORMANCE: Admin requested performance status");

        try {
            Map<String, Object> status = new HashMap<>();
            
            Map<String, Long> invocationCounts = new HashMap<>();
            PerformanceMonitor.getAllInvocationCounts().forEach((key, value) -> {
                invocationCounts.put(key, value.get());
            });

            // Calculate health status
            int criticalCount = 0;
            int warningCount = 0;

            for (String key : invocationCounts.keySet()) {
                double avgTime = PerformanceMonitor.getAverageExecutionTime(key);
                if (avgTime > 5000) {
                    criticalCount++;
                } else if (avgTime > 1000) {
                    warningCount++;
                }
            }

            String healthStatus;
            if (criticalCount > 0) {
                healthStatus = "RED";
            } else if (warningCount > 0) {
                healthStatus = "YELLOW";
            } else {
                healthStatus = "GREEN";
            }

            status.put("healthStatus", healthStatus);
            status.put("activeMethods", invocationCounts.size());
            status.put("criticalViolations", criticalCount);
            status.put("warningViolations", warningCount);
            status.put("timestamp", System.currentTimeMillis());

            return Response.ok(status).build();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PERFORMANCE: Error getting status", e);
            return Response.serverError()
                    .entity("{\"error\": \"Failed to get performance status\"}")
                    .build();
        }
    }
}