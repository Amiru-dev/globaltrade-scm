package com.globaltrade.web.servlet;

import com.globaltrade.ejb.interceptor.PerformanceMonitor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet for handling performance monitoring dashboard requests.
 *
 * <p><strong>Performance Dashboard Features:</strong></p>
 * <ul>
 *   <li>Real-time performance metrics from PerformanceMonitor interceptor</li>
 *   <li>Load testing simulation for performance validation</li>
 *   <li>Performance trend visualization</li>
 *   <li>System health status monitoring</li>
 * </ul>
 *
 * <p><strong>Security:</strong> Admin-only access to sensitive performance data.
 */
@WebServlet("/performance-dashboard")
@RolesAllowed({"SYSTEM_ADMIN"})
public class PerformanceDashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PerformanceDashboardServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            // Check for reset parameter
            String resetParam = request.getParameter("reset");
            if ("true".equals(resetParam)) {
                PerformanceMonitor.resetMetrics();
                LOGGER.log(Level.INFO, "PERFORMANCE DASHBOARD: Metrics reset by admin");
            }

            // Collect performance data from PerformanceMonitor
            Map<String, Object> performanceData = collectPerformanceData();

            request.setAttribute("performanceData", performanceData);
            request.setAttribute("currentTime", java.time.LocalDateTime.now().toString());
            request.setAttribute("username", request.getRemoteUser());
            request.setAttribute("userRole", getUserRole(request));

            request.getRequestDispatcher("/performance-dashboard.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PERFORMANCE DASHBOARD: Error loading dashboard", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Failed to load performance dashboard");
        }
    }

    /**
     * Collects performance data from the PerformanceMonitor interceptor.
     *
     * <p><strong>Data Collected:</strong></p>
     * <ul>
     *   <li>Method invocation counts</li>
     *   <li>Average execution times per method</li>
     *   <li>Maximum execution times per method</li>
     *   <li>Overall system health status</li>
     *   <li>Performance violation counts</li>
     * </ul>
     */
    private Map<String, Object> collectPerformanceData() {
        Map<String, Object> data = new HashMap<>();

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

        // Calculate overall statistics
        long totalInvocations = invocationCounts.values().stream().mapToLong(Long::longValue).sum();
        double overallAvgTime = 0.0;
        int methodCount = 0;

        for (String key : invocationCounts.keySet()) {
            double avgTime = PerformanceMonitor.getAverageExecutionTime(key);
            overallAvgTime += avgTime;
            methodCount++;
        }

        if (methodCount > 0) {
            overallAvgTime = overallAvgTime / methodCount;
        }

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

        data.put("invocationCounts", invocationCounts);
        data.put("averageExecutionTimes", averageTimes);
        data.put("maxExecutionTimes", maxTimes);
        data.put("healthStatus", healthStatus);
        data.put("criticalViolations", criticalCount);
        data.put("warningViolations", warningCount);
        data.put("totalInvocations", totalInvocations);
        data.put("overallAvgTime", overallAvgTime);

        LOGGER.log(Level.INFO, "PERFORMANCE DASHBOARD: Collected metrics for {0} methods, " +
                "total invocations: {1}, health: {2}", 
                new Object[]{methodCount, totalInvocations, healthStatus});

        return data;
    }

    /**
     * Gets the user role from the request context.
     */
    private String getUserRole(HttpServletRequest request) {
        // In a real implementation, this would extract roles from the security context
        // For now, return a default role
        return "SYSTEM_ADMIN";
    }
}
