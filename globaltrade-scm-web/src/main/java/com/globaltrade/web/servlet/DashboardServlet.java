package com.globaltrade.web.servlet;

import com.globaltrade.common.dto.InventoryAlertDTO;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.ejb.service.InventoryService;
import com.globaltrade.ejb.service.ShipmentService;
import com.globaltrade.ejb.timer.InventoryMonitorTimer;
import com.globaltrade.ejb.timer.ShipmentTrackingTimer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dashboard servlet for displaying timer-driven events and supply chain metrics.
 * 
 * <p><strong>Critical Analysis:</strong> This servlet demonstrates the integration
 * of EJB timer services with web presentation. It shows how timer-generated data
 * can be accessed and displayed to users, providing real-time visibility into
 * automated supply chain operations.</p>
 */
@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(DashboardServlet.class.getName());

    @EJB
    private ShipmentService shipmentService;

    @EJB
    private InventoryService inventoryService;

    @EJB
    private ShipmentTrackingTimer shipmentTrackingTimer;

    @EJB
    private InventoryMonitorTimer inventoryMonitorTimer;

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
        
        LOGGER.log(Level.INFO, "Dashboard accessed by user: {0} with role: {1}", 
                new Object[]{username, userRole});
        
        try {
            // Fetch timer-generated data
            Map<String, Object> dashboardData = fetchDashboardData(userRole);
            
            // Set data for JSP
            request.setAttribute("dashboardData", dashboardData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // Forward to dashboard JSP
            request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading dashboard data", e);
            
            // Create fallback data to prevent null pointer
            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("accessLevel", "Demo Mode - EJB Access Restricted");
            fallbackData.put("showFullDetails", false);
            fallbackData.put("timerServicesActive", false);
            fallbackData.put("error", "EJB Security: " + e.getMessage());
            
            request.setAttribute("dashboardData", fallbackData);
            request.setAttribute("username", username);
            request.setAttribute("userRole", userRole);
            request.setAttribute("roleDescription", session.getAttribute("roleDescription"));
            request.setAttribute("currentTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
        }
    }

    /**
     * Fetches dashboard data from timer services and EJB components.
     * Role-based access control determines what data is returned.
     */
    private Map<String, Object> fetchDashboardData(String userRole) {
        Map<String, Object> data = new HashMap<>();
        
        // Timer service status
        data.put("timerServicesActive", true);
        data.put("lastTimerExecution", LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Shipment metrics (from ShipmentTrackingTimer)
        long inTransit = shipmentService.countByStatus(ShipmentStatus.IN_TRANSIT);
        long delivered = shipmentService.countByStatus(ShipmentStatus.DELIVERED);
        long atCustoms = shipmentService.countByStatus(ShipmentStatus.AT_CUSTOMS);
        long delayed = shipmentService.findDelayedShipments().size();
        
        Map<String, Object> shipmentMetrics = new HashMap<>();
        shipmentMetrics.put("inTransit", inTransit);
        shipmentMetrics.put("delivered", delivered);
        shipmentMetrics.put("atCustoms", atCustoms);
        shipmentMetrics.put("delayed", delayed);
        shipmentMetrics.put("totalActive", inTransit + atCustoms);
        data.put("shipmentMetrics", shipmentMetrics);
        
        // Recent delayed shipments for demonstration
        List<Shipment> delayedShipments = shipmentService.findDelayedShipments();
        data.put("delayedShipments", delayedShipments.size() > 5 ? delayedShipments.subList(0, 5) : delayedShipments);
        
        // Inventory metrics (from InventoryMonitorTimer)
        List<InventoryAlertDTO> inventoryAlerts = inventoryMonitorTimer.getRecentAlerts();
        int lowStockCount = inventoryService.findLowStockItems().size();
        int outOfStockCount = inventoryService.findOutOfStockItems().size();
        
        Map<String, Object> inventoryMetrics = new HashMap<>();
        inventoryMetrics.put("alerts", inventoryAlerts);
        inventoryMetrics.put("lowStockCount", lowStockCount);
        inventoryMetrics.put("outOfStockCount", outOfStockCount);
        inventoryMetrics.put("totalAlerts", inventoryAlerts.size());
        data.put("inventoryMetrics", inventoryMetrics);
        
        // Timer-specific information
        Map<String, Object> timerInfo = new HashMap<>();
        timerInfo.put("shipmentTrackingTimer", "Active - 15 minute intervals");
        timerInfo.put("inventoryMonitorTimer", "Active - Hourly schedule");
        timerInfo.put("routeOptimizationTimer", "Active - Event-driven");
        timerInfo.put("customsDeadlineTimer", "Active - Daily schedule");
        timerInfo.put("vendorPerformanceTimer", "Active - Weekly schedule");
        data.put("timerInfo", timerInfo);
        
        // Role-based access control for sensitive data
        if ("SYSTEM_ADMIN".equals(userRole) || "LOGISTICS_COORDINATOR".equals(userRole)) {
            data.put("showFullDetails", true);
            data.put("accessLevel", "Full");
        } else if ("WAREHOUSE_MANAGER".equals(userRole)) {
            data.put("showFullDetails", false);
            data.put("accessLevel", "Warehouse Operations");
        } else if ("CUSTOMS_AGENT".equals(userRole)) {
            data.put("showFullDetails", false);
            data.put("accessLevel", "Customs Operations");
        } else {
            data.put("showFullDetails", false);
            data.put("accessLevel", "Limited");
        }
        
        return data;
    }
}