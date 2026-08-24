<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="com.globaltrade.common.entity.Shipment" %>
<%@ page import="com.globaltrade.common.dto.InventoryAlertDTO" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Dashboard</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f5f7fa;
            min-height: 100vh;
        }

        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px 30px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        }

        .header h1 {
            font-size: 24px;
            font-weight: 600;
        }

        .user-info {
            display: flex;
            align-items: center;
            gap: 20px;
            flex-wrap: wrap;
        }

        .user-info span {
            font-size: 14px;
        }

        .user-info .role-info {
            background: rgba(255, 255, 255, 0.15);
            padding: 8px 12px;
            border-radius: 5px;
            font-size: 12px;
            max-width: 400px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .user-info .username {
            font-weight: 600;
            font-size: 15px;
        }

        .logout-btn {
            background: rgba(255, 255, 255, 0.2);
            color: white;
            border: 1px solid rgba(255, 255, 255, 0.3);
            padding: 8px 16px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.3s;
            text-decoration: none;
            display: inline-block;
        }

        .logout-btn:hover {
            background: rgba(255, 255, 255, 0.3);
        }

        .container {
            max-width: 1200px;
            margin: 30px auto;
            padding: 0 20px;
        }

        .section {
            background: white;
            border-radius: 10px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
            margin-bottom: 20px;
            padding: 25px;
        }

        .section h2 {
            color: #333;
            font-size: 20px;
            margin-bottom: 20px;
            padding-bottom: 10px;
            border-bottom: 2px solid #667eea;
        }

        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }

        .metric-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }

        .metric-card.green {
            background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
        }

        .metric-card.yellow {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        }

        .metric-card.red {
            background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);
        }

        .metric-card h3 {
            font-size: 36px;
            font-weight: 700;
            margin-bottom: 5px;
        }

        .metric-card p {
            font-size: 14px;
            opacity: 0.9;
        }

        .timer-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            border-left: 4px solid #667eea;
        }

        .timer-info h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 10px;
        }

        .timer-list {
            list-style: none;
        }

        .timer-list li {
            padding: 8px 0;
            border-bottom: 1px solid #e0e0e0;
            font-size: 14px;
            color: #666;
        }

        .timer-list li:last-child {
            border-bottom: none;
        }

        .table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }

        .table th,
        .table td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #e0e0e0;
        }

        .table th {
            background: #f8f9fa;
            font-weight: 600;
            color: #333;
            font-size: 14px;
        }

        .table td {
            font-size: 14px;
            color: #666;
        }

        .badge {
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
        }

        .badge-success {
            background: #d4edda;
            color: #155724;
        }

        .badge-warning {
            background: #fff3cd;
            color: #856404;
        }

        .badge-danger {
            background: #f8d7da;
            color: #721c24;
        }

        .badge-info {
            background: #d1ecf1;
            color: #0c5460;
        }

        .nav-bar {
            background: white;
            padding: 15px 30px;
            box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
            margin-bottom: 20px;
        }

        .nav-bar a {
            color: #667eea;
            text-decoration: none;
            margin-right: 20px;
            font-weight: 500;
            font-size: 14px;
            transition: color 0.3s;
        }

        .nav-bar a:hover {
            color: #764ba2;
        }

        .nav-bar a.active {
            color: #764ba2;
            font-weight: 600;
        }

        .access-level {
            background: #e8f4f8;
            color: #2c5f7c;
            padding: 10px 15px;
            border-radius: 5px;
            font-size: 13px;
            border-left: 3px solid #2c5f7c;
        }

        .no-data {
            text-align: center;
            padding: 40px;
            color: #999;
            font-size: 14px;
        }
    </style>
</head>
<body>
<div class="header">
    <div>
        <h1>🚚 GlobalTrade SCM Dashboard</h1>
        <p style="font-size: 14px; opacity: 0.9; margin-top: 5px;">Supply Chain Management System</p>
    </div>
    <div class="user-info">
        <div class="username">👤 <%= request.getAttribute("fullName") != null ? request.getAttribute("fullName") : request.getAttribute("username") %></div>
        <div class="role-info" title="<%= request.getAttribute("userRole") %>">
            <%= request.getAttribute("userRole") %>
        </div>
        <div class="access-level">
            Access Level: <%= ((Map<String, Object>) request.getAttribute("dashboardData")).get("accessLevel") %>
        </div>
        <a href="login?logout=true" class="logout-btn">Logout</a>
    </div>
</div>

<div class="nav-bar">
    <a href="dashboard" class="active">📊 Dashboard</a>
    <a href="performance-dashboard">⚡ Performance</a>
    <a href="auditlog">📋 Audit Log</a>
    <a href="transaction">💳 Transaction Demo</a>
    <a href="admin">⚙️ Admin</a>
</div>

<div class="container">
    <%
        Map<String, Object> dashboardData = (Map<String, Object>) request.getAttribute("dashboardData");
        if (dashboardData == null) {
            dashboardData = new HashMap<>();
            dashboardData.put("accessLevel", "Error - Data not available");
            dashboardData.put("showFullDetails", false);
            dashboardData.put("timerServicesActive", false);
        }

        Map<String, Object> shipmentMetrics = (Map<String, Object>) dashboardData.getOrDefault("shipmentMetrics", new HashMap<>());
        Map<String, Object> inventoryMetrics = (Map<String, Object>) dashboardData.getOrDefault("inventoryMetrics", new HashMap<>());
        Map<String, Object> timerInfo = (Map<String, Object>) dashboardData.getOrDefault("timerInfo", new HashMap<>());
    %>

    <div class="section">
        <h2>📊 Supply Chain Metrics</h2>
        <div class="metrics-grid">
            <div class="metric-card">
                <h3><%= shipmentMetrics.get("inTransit") %>
                </h3>
                <p>In Transit</p>
            </div>
            <div class="metric-card green">
                <h3><%= shipmentMetrics.get("delivered") %>
                </h3>
                <p>Delivered</p>
            </div>
            <div class="metric-card yellow">
                <h3><%= shipmentMetrics.get("atCustoms") %>
                </h3>
                <p>At Customs</p>
            </div>
            <div class="metric-card">
                <h3><%= shipmentMetrics.get("delayed") %>
                </h3>
                <p>Delayed</p>
            </div>
            <div class="metric-card yellow">
                <h3><%= inventoryMetrics.get("lowStockCount") %>
                </h3>
                <p>Low Stock Alerts</p>
            </div>
            <div class="metric-card">
                <h3><%= inventoryMetrics.get("outOfStockCount") %>
                </h3>
                <p>Out of Stock</p>
            </div>
        </div>
    </div>

    <div class="section">
        <h2>⏰ Timer Services Status</h2>
        <div class="timer-info">
            <h3>Active EJB Timer Services</h3>
            <ul class="timer-list">
                <li>📦 Shipment Tracking Timer: <%= timerInfo.get("shipmentTrackingTimer") %>
                </li>
                <li>📋 Inventory Monitor Timer: <%= timerInfo.get("inventoryMonitorTimer") %>
                </li>
                <li>🛣️ Route Optimization Timer: <%= timerInfo.get("routeOptimizationTimer") %>
                </li>
                <li>🏛️ Customs Deadline Timer: <%= timerInfo.get("customsDeadlineTimer") %>
                </li>
                <li>👥 Vendor Performance Timer: <%= timerInfo.get("vendorPerformanceTimer") %>
                </li>
            </ul>
            <p style="margin-top: 10px; font-size: 13px; color: #666;">
                Last Timer Execution: <%= dashboardData.get("lastTimerExecution") %>
            </p>
        </div>
    </div>

    <div class="section">
        <h2>🚚 Delayed Shipments (Timer Alert)</h2>
        <%
            List<Shipment> delayedShipments = (List<Shipment>) dashboardData.get("delayedShipments");
            if (delayedShipments != null && !delayedShipments.isEmpty()) {
        %>
        <table class="table">
            <thead>
            <tr>
                <th>Tracking Number</th>
                <th>Origin</th>
                <th>Destination</th>
                <th>Carrier</th>
                <th>Status</th>
            </tr>
            </thead>
            <tbody>
            <% for (Shipment shipment : delayedShipments) { %>
            <tr>
                <td><%= shipment.getTrackingNumber() %>
                </td>
                <td><%= shipment.getOriginCity() %>
                </td>
                <td><%= shipment.getDestinationCity() %>
                </td>
                <td><%= shipment.getCarrier() %>
                </td>
                <td><span class="badge badge-warning"><%= shipment.getStatus() %></span></td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <% } else { %>
        <div class="no-data">No delayed shipments detected by timer service</div>
        <% } %>
    </div>

    <div class="section">
        <h2>📦 Inventory Alerts (Timer Generated)</h2>
        <%
            List<InventoryAlertDTO> alerts = (List<InventoryAlertDTO>) inventoryMetrics.get("alerts");
            if (alerts != null && !alerts.isEmpty()) {
        %>
        <table class="table">
            <thead>
            <tr>
                <th>SKU</th>
                <th>Product</th>
                <th>Warehouse</th>
                <th>Current Qty</th>
                <th>Alert Type</th>
                <th>Severity</th>
            </tr>
            </thead>
            <tbody>
            <% for (InventoryAlertDTO alert : alerts) { %>
            <tr>
                <td><%= alert.getSku() %>
                </td>
                <td><%= alert.getProductName() %>
                </td>
                <td><%= alert.getWarehouseId() %>
                </td>
                <td><%= alert.getCurrentQuantity() %>
                </td>
                <td><span
                        class="badge <%= "OUT_OF_STOCK".equals(alert.getAlertType()) ? "badge-danger" : "badge-warning" %>"><%= alert.getAlertType() %></span>
                </td>
                <td><span
                        class="badge <%= "CRITICAL".equals(alert.getSeverity()) ? "badge-danger" : "badge-warning" %>"><%= alert.getSeverity() %></span>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <% } else { %>
        <div class="no-data">No inventory alerts generated by timer service</div>
        <% } %>
    </div>

    <div class="section">
        <h2>ℹ️ System Information</h2>
        <div class="timer-info">
            <p><strong>Current Time:</strong> <%= request.getAttribute("currentTime") %>
            </p>
            <p><strong>User Role:</strong> <%= request.getAttribute("userRole") %>
            </p>
            <p><strong>Access Level:</strong> <%= dashboardData.get("accessLevel") %>
            </p>
            <p><strong>Timer Services Active:</strong> <%= dashboardData.get("timerServicesActive") %>
            </p>
        </div>
    </div>
</div>
</body>
</html>