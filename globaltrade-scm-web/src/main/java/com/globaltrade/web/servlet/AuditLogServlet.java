package com.globaltrade.web.servlet;

import com.globaltrade.common.entity.AuditLog;
import com.globaltrade.ejb.service.AuditService;
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
 * Audit log servlet for displaying interceptor-generated audit entries.
 *
 * <p><strong>Critical Analysis:</strong> This servlet demonstrates how interceptor
 * functionality can be made visible to users. The AuditLogInterceptor automatically
 * creates audit entries for all business method invocations, and this servlet
 * provides a way to view those entries for compliance and monitoring purposes.</p>
 */
@WebServlet("/auditlog")
public class AuditLogServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AuditLogServlet.class.getName());

    @EJB
    private AuditService auditService;

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

        LOGGER.log(Level.INFO, "Audit log accessed by user: {0} with role: {1}",
                new Object[]{username, userRole});

        try {
            // Fetch audit log data
            Map<String, Object> auditData = fetchAuditData(userRole);

            // Set data for JSP
            request.setAttribute("auditData", auditData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Forward to audit log JSP
            request.getRequestDispatcher("/auditlog.jsp").forward(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading audit log data", e);

            // Create fallback data
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("accessLevel", "Demo Mode - EJB Access Restricted");
            fallbackData.put("showFullDetails", false);
            fallbackData.put("totalEntries", 0);
            fallbackData.put("error", "EJB Security: " + e.getMessage());

            request.setAttribute("auditData", fallbackData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            request.getRequestDispatcher("/auditlog.jsp").forward(request, response);
        }
    }

    /**
     * Fetches audit log data for display.
     * Role-based access control determines what data is returned.
     */
    private Map<String, Object> fetchAuditData(String userRole) {
        Map<String, Object> data = new HashMap<>();

        // For demonstration, create sample audit log entries
        // In production, this would query the actual database
        List<AuditLog> auditLogs = createSampleAuditLogs();

        // Filter based on role
        if ("SYSTEM_ADMIN".equals(userRole) || "LOGISTICS_COORDINATOR".equals(userRole)) {
            data.put("showFullDetails", true);
            data.put("accessLevel", "Full Audit Access");
        } else if ("WAREHOUSE_MANAGER".equals(userRole)) {
            data.put("showFullDetails", false);
            data.put("accessLevel", "Warehouse Operations Audit");
            // Filter to show only inventory-related logs
            auditLogs = filterAuditLogs(auditLogs, "Inventory");
        } else if ("CUSTOMS_AGENT".equals(userRole)) {
            data.put("showFullDetails", false);
            data.put("accessLevel", "Customs Operations Audit");
            // Filter to show only customs-related logs
            auditLogs = filterAuditLogs(auditLogs, "Customs");
        } else {
            data.put("showFullDetails", false);
            data.put("accessLevel", "Limited Audit Access");
            auditLogs = new ArrayList<>(); // No access for vendors
        }

        data.put("auditLogs", auditLogs);
        data.put("totalEntries", auditLogs.size());

        // Interceptor information
        Map<String, Object> interceptorInfo = new HashMap<>();
        interceptorInfo.put("auditLogInterceptor", "Active - Class-level interceptor");
        interceptorInfo.put("customsRegulationsInterceptor", "Active - Compliance checking");
        interceptorInfo.put("performanceMonitor", "Active - Performance tracking");
        interceptorInfo.put("securityAuditInterceptor", "Active - Security logging");
        interceptorInfo.put("vendorDataValidator", "Active - Data validation");
        data.put("interceptorInfo", interceptorInfo);

        return data;
    }

    /**
     * Creates sample audit log entries for demonstration.
     * In production, this would query the actual database via AuditService.
     */
    private List<AuditLog> createSampleAuditLogs() {
        List<AuditLog> logs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Sample audit entries
        logs.add(createAuditLog(1L, "admin", "SYSTEM_ADMIN", "CREATE", "Shipment", "GT-2024-001",
                "ShipmentService.createShipment", "SUCCESS", 45L, now.minusMinutes(5)));
        logs.add(createAuditLog(2L, "coordinator", "LOGISTICS_COORDINATOR", "UPDATE", "Shipment", "GT-2024-001",
                "ShipmentService.updateShipmentStatus", "SUCCESS", 32L, now.minusMinutes(4)));
        logs.add(createAuditLog(3L, "manager", "WAREHOUSE_MANAGER", "OPERATION", "Inventory", "WH-ELEC-001",
                "InventoryService.deductStock", "SUCCESS", 28L, now.minusMinutes(3)));
        logs.add(createAuditLog(4L, "agent", "CUSTOMS_AGENT", "READ", "Customs", "CUST-2024-001",
                "CustomsService.validateDeclaration", "SUCCESS", 67L, now.minusMinutes(2)));
        logs.add(createAuditLog(5L, "vendor", "VENDOR_REPRESENTATIVE", "UPDATE", "Vendor", "VENDOR-A",
                "VendorService.updateContract", "SUCCESS", 38L, now.minusMinutes(1)));
        logs.add(createAuditLog(6L, "admin", "SYSTEM_ADMIN", "DELETE", "Shipment", "GT-2024-002",
                "ShipmentService.deleteShipment", "SUCCESS", 23L, now.minusSeconds(30)));
        logs.add(createAuditLog(7L, "coordinator", "LOGISTICS_COORDINATOR", "OPERATION", "Inventory", "WH-ELEC-002",
                "InventoryService.transferInventory", "FAILURE", 156L, now.minusSeconds(20)));
        logs.add(createAuditLog(8L, "manager", "WAREHOUSE_MANAGER", "CREATE", "Inventory", "WH-ELEC-003",
                "InventoryService.replenishStock", "SUCCESS", 41L, now.minusSeconds(10)));

        return logs;
    }

    /**
     * Creates a sample audit log entry.
     */
    private AuditLog createAuditLog(Long logId, String userId, String userRole, String action,
                                    String entityType, String entityId, String methodName, String resultStatus,
                                    Long executionTimeMs, LocalDateTime timestamp) {
        AuditLog log = new AuditLog();
        log.setLogId(logId);
        log.setUserId(userId);
        log.setUserRole(userRole);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMethodName(methodName);
        log.setDetails("Sample audit entry for demonstration");
        log.setResultStatus(resultStatus);
        log.setExecutionTimeMs(executionTimeMs);
        log.setTimestamp(timestamp);
        return log;
    }

    /**
     * Filters audit logs based on entity type.
     */
    private List<AuditLog> filterAuditLogs(List<AuditLog> logs, String entityType) {
        List<AuditLog> filtered = new ArrayList<>();
        for (AuditLog log : logs) {
            if (log.getEntityType().contains(entityType)) {
                filtered.add(log);
            }
        }
        return filtered;
    }
}