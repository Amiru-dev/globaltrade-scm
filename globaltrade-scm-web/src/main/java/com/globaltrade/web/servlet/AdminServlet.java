package com.globaltrade.web.servlet;

import com.globaltrade.ejb.security.SecurityService;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin servlet for demonstrating advanced security features and emergency access.
 *
 * <p><strong>Critical Analysis:</strong> This servlet demonstrates programmatic
 * authorization and emergency access override scenarios that are critical for
 * supply chain resilience during disruptions.</p>
 */
@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminServlet.class.getName());

    @EJB
    private SecurityService securityService;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            response.sendRedirect("login");
            return;
        }

        String userRole = (String) session.getAttribute("userRole");
        String username = (String) session.getAttribute("username");

        LOGGER.log(Level.INFO, "Admin panel accessed by user: {0} with role: {1}",
                new Object[]{username, userRole});

        try {
            // Fetch admin data
            Map<String, Object> adminData = fetchAdminData(userRole, username);

            // Set data for JSP
            request.setAttribute("adminData", adminData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Forward to admin JSP
            request.getRequestDispatcher("/admin.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading admin data", e);

            // Create fallback data
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("accessLevel", "Demo Mode - EJB Access Restricted");
            fallbackData.put("hasAdminAccess", false);
            fallbackData.put("canPerformEmergencyAccess", false);
            fallbackData.put("canPerformHighValueOperations", false);
            fallbackData.put("canScheduleMaintenance", false);
            fallbackData.put("error", "EJB Security: " + e.getMessage());

            request.setAttribute("adminData", fallbackData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            request.getRequestDispatcher("/admin.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userRole") == null) {
            response.sendRedirect("login");
            return;
        }

        String userRole = (String) session.getAttribute("userRole");
        String username = (String) session.getAttribute("username");
        String operation = request.getParameter("operation");

        Map<String, Object> result = new HashMap<>();

        try {
            switch (operation) {
                case "emergency_access":
                    result = performEmergencyAccess(request, username);
                    break;
                case "high_value_operation":
                    result = performHighValueOperation(request, username);
                    break;
                case "maintenance_window":
                    result = performMaintenanceWindow(request, username);
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "Invalid operation");
            }

            // Store result in session for display
            session.setAttribute("adminResult", result);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error performing admin operation", e);
            result.put("success", false);
            result.put("message", "Admin operation error: " + e.getMessage());
            session.setAttribute("adminResult", result);
        }

        // Redirect back to admin page
        response.sendRedirect("admin");
    }

    /**
     * Fetches admin data for display.
     */
    private Map<String, Object> fetchAdminData(String userRole, String username) {
        Map<String, Object> data = new HashMap<>();

        // Security features information
        Map<String, Object> securityFeatures = new HashMap<>();
        securityFeatures.put("emergencyAccess", "SYSTEM_ADMIN can override normal authorization during logistics emergencies");
        securityFeatures.put("highValueAuthorization", "Operations over $100,000 require admin approval");
        securityFeatures.put("maintenanceWindows", "Critical operations restricted to approved maintenance windows (2 AM - 4 AM)");
        securityFeatures.put("programmaticAuthorization", "Runtime authorization decisions based on business context");
        securityFeatures.put("dataLevelSecurity", "Vendor representatives can only access their own data");
        data.put("securityFeatures", securityFeatures);

        // Role permissions matrix
        Map<String, List<String>> rolePermissions = new HashMap<>();
        rolePermissions.put("SYSTEM_ADMIN", List.of("Full system access", "Emergency overrides", "User management", "System configuration", "All audit logs"));
        rolePermissions.put("LOGISTICS_COORDINATOR", List.of("Shipment management", "Inventory operations", "Route optimization", "Vendor management", "Performance metrics"));
        rolePermissions.put("WAREHOUSE_MANAGER", List.of("Inventory management", "Warehouse operations", "Stock transfers", "Replenishment orders"));
        rolePermissions.put("CUSTOMS_AGENT", List.of("Customs declarations", "Compliance reports", "Trade documentation", "Regulatory checking"));
        rolePermissions.put("VENDOR_REPRESENTATIVE", List.of("View own orders", "Update delivery status", "View own contracts", "Limited inventory access"));
        data.put("rolePermissions", rolePermissions);

        // Recent emergency access attempts (sample data)
        List<Map<String, String>> emergencyAttempts = new ArrayList<>();
        emergencyAttempts.add(createEmergencyAttempt("admin", "SHIPMENT", "Port closure - Hurricane Alpha", true));
        emergencyAttempts.add(createEmergencyAttempt("coordinator", "INVENTORY", "Warehouse fire emergency", false));
        emergencyAttempts.add(createEmergencyAttempt("admin", "CUSTOMS", "System disruption", true));
        data.put("emergencyAttempts", emergencyAttempts);

        // Role-based access control
        if ("SYSTEM_ADMIN".equals(userRole)) {
            data.put("hasAdminAccess", true);
            data.put("canPerformEmergencyAccess", true);
            data.put("canPerformHighValueOperations", true);
            data.put("canScheduleMaintenance", true);
            data.put("accessLevel", "Full Administrator Access");
        } else if ("LOGISTICS_COORDINATOR".equals(userRole)) {
            data.put("hasAdminAccess", false);
            data.put("canPerformEmergencyAccess", false);
            data.put("canPerformHighValueOperations", true);
            data.put("canScheduleMaintenance", false);
            data.put("accessLevel", "Coordinator Access");
        } else {
            data.put("hasAdminAccess", false);
            data.put("canPerformEmergencyAccess", false);
            data.put("canPerformHighValueOperations", false);
            data.put("canScheduleMaintenance", false);
            data.put("accessLevel", "Limited Access");
        }

        return data;
    }

    /**
     * Performs emergency access override.
     */
    private Map<String, Object> performEmergencyAccess(HttpServletRequest request, String username) {
        Map<String, Object> result = new HashMap<>();

        try {
            String resourceType = request.getParameter("resourceType");
            String reason = request.getParameter("reason");

            LOGGER.log(Level.INFO, "Emergency access attempt by {0} for resource {1}: {2}",
                    new Object[]{username, resourceType, reason});

            // This calls the SecurityService.authorizeEmergencyAccess method
            boolean authorized = securityService.authorizeEmergencyAccess(resourceType, reason);

            result.put("success", authorized);
            result.put("message", authorized ? "Emergency access granted" : "Emergency access denied");
            result.put("operation", "EMERGENCY_ACCESS");
            result.put("resourceType", resourceType);
            result.put("reason", reason);
            result.put("authorized", authorized);
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Emergency access failed", e);
            result.put("success", false);
            result.put("message", "Emergency access error: " + e.getMessage());
            result.put("operation", "EMERGENCY_ACCESS");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        return result;
    }

    /**
     * Performs high-value operation authorization.
     */
    private Map<String, Object> performHighValueOperation(HttpServletRequest request, String username) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operationType = request.getParameter("operationType");
            double declaredValue = Double.parseDouble(request.getParameter("declaredValue"));

            LOGGER.log(Level.INFO, "High-value operation attempt by {0}: {1} for value ${2}",
                    new Object[]{username, operationType, declaredValue});

            // This calls the SecurityService.authorizeHighValueOperation method
            boolean authorized = securityService.authorizeHighValueOperation(operationType, declaredValue);

            result.put("success", authorized);
            result.put("message", authorized ? "High-value operation authorized" : "High-value operation denied - requires admin approval");
            result.put("operation", "HIGH_VALUE_OPERATION");
            result.put("operationType", operationType);
            result.put("declaredValue", declaredValue);
            result.put("authorized", authorized);
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "High-value operation failed", e);
            result.put("success", false);
            result.put("message", "High-value operation error: " + e.getMessage());
            result.put("operation", "HIGH_VALUE_OPERATION");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        return result;
    }

    /**
     * Performs maintenance window authorization.
     */
    private Map<String, Object> performMaintenanceWindow(HttpServletRequest request, String username) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operationType = request.getParameter("operationType");
            LocalDateTime scheduledTime = LocalDateTime.parse(request.getParameter("scheduledTime"));

            LOGGER.log(Level.INFO, "Maintenance window check by {0}: {1} at {2}",
                    new Object[]{username, operationType, scheduledTime});

            // This calls the SecurityService.authorizeMaintenanceWindow method
            boolean authorized = securityService.authorizeMaintenanceWindow(operationType, scheduledTime);

            result.put("success", authorized);
            result.put("message", authorized ? "Operation within approved maintenance window" : "Operation outside approved maintenance window");
            result.put("operation", "MAINTENANCE_WINDOW");
            result.put("operationType", operationType);
            result.put("scheduledTime", scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("authorized", authorized);
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Maintenance window check failed", e);
            result.put("success", false);
            result.put("message", "Maintenance window error: " + e.getMessage());
            result.put("operation", "MAINTENANCE_WINDOW");
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        return result;
    }

    /**
     * Creates a sample emergency access attempt record.
     */
    private Map<String, String> createEmergencyAttempt(String user, String resource, String reason, boolean granted) {
        Map<String, String> attempt = new HashMap<>();
        attempt.put("user", user);
        attempt.put("resource", resource);
        attempt.put("reason", reason);
        attempt.put("granted", granted ? "GRANTED" : "DENIED");
        attempt.put("timestamp", LocalDateTime.now().minusHours((long) (Math.random() * 24)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return attempt;
    }
}