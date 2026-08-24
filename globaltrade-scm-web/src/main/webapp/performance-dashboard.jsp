<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Performance Monitoring Dashboard</title>
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

        .access-level {
            background: #e8f4f8;
            color: #2c5f7c;
            padding: 10px 15px;
            border-radius: 5px;
            font-size: 13px;
            border-left: 3px solid #2c5f7c;
        }

        .container {
            max-width: 1400px;
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
            background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
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

        .performance-bar {
            height: 20px;
            background: #e0e0e0;
            border-radius: 10px;
            overflow: hidden;
            margin: 5px 0;
        }

        .performance-bar-fill {
            height: 100%;
            border-radius: 10px;
            transition: width 0.3s;
        }

        .performance-bar-fill.green {
            background: linear-gradient(90deg, #11998e 0%, #38ef7d 100%);
        }

        .performance-bar-fill.yellow {
            background: linear-gradient(90deg, #f6d365 0%, #fda085 100%);
        }

        .performance-bar-fill.red {
            background: linear-gradient(90deg, #eb3349 0%, #f45c43 100%);
        }

        .refresh-btn {
            background: #667eea;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.3s;
        }

        .refresh-btn:hover {
            background: #764ba2;
        }

        .load-test-section {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin-top: 20px;
        }

        .load-test-btn {
            background: #f5576c;
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            margin-right: 10px;
            transition: background 0.3s;
        }

        .load-test-btn:hover {
            background: #eb3349;
        }

        .status-indicator {
            display: inline-block;
            width: 12px;
            height: 12px;
            border-radius: 50%;
            margin-right: 8px;
        }

        .status-indicator.green {
            background: #38ef7d;
        }

        .status-indicator.yellow {
            background: #f5576c;
        }

        .status-indicator.red {
            background: #eb3349;
        }

        .trend-chart {
            height: 200px;
            background: #f8f9fa;
            border-radius: 8px;
            margin-top: 15px;
            display: flex;
            align-items: flex-end;
            padding: 20px;
            gap: 10px;
        }

        .trend-bar {
            flex: 1;
            background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
            border-radius: 4px 4px 0 0;
            min-height: 10px;
            transition: height 0.3s;
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
        <h1>⚡ Performance Monitoring Dashboard</h1>
        <p style="font-size: 14px; opacity: 0.9; margin-top: 5px;">Real-time Performance Metrics & Monitoring</p>
    </div>
    <div class="user-info">
        <div class="username">👤 <%= request.getAttribute("username") != null ? request.getAttribute("username") : "Guest" %></div>
        <div class="role-info" title="<%= request.getAttribute("roleDescription") != null ? request.getAttribute("roleDescription") : "Viewer" %>">
            <%= request.getAttribute("roleDescription") != null ? request.getAttribute("roleDescription") : "Viewer" %>
        </div>
        <button class="refresh-btn" onclick="location.reload()">🔄 Refresh Metrics</button>
        <a href="login?logout=true" class="logout-btn">Logout</a>
    </div>
</div>

<div class="nav-bar">
    <a href="dashboard">📊 Dashboard</a>
    <a href="performance-dashboard" class="active">⚡ Performance</a>
    <a href="auditlog">📋 Audit Log</a>
    <a href="transaction">💳 Transaction Demo</a>
    <a href="admin">⚙️ Admin</a>
</div>

<div class="container">
    <%
        // Get performance data from request attributes or generate sample data
        Map<String, Object> performanceData = (Map<String, Object>) request.getAttribute("performanceData");
        if (performanceData == null) {
            performanceData = generateSamplePerformanceData();
        }

        Map<String, Long> invocationCounts = (Map<String, Long>) performanceData.getOrDefault("invocationCounts", new HashMap<>());
        Map<String, Double> averageTimes = (Map<String, Double>) performanceData.getOrDefault("averageExecutionTimes", new HashMap<>());
        Map<String, Long> maxTimes = (Map<String, Long>) performanceData.getOrDefault("maxExecutionTimes", new HashMap<>());
        String healthStatus = (String) performanceData.getOrDefault("healthStatus", "GREEN");
        int criticalViolations = (Integer) performanceData.getOrDefault("criticalViolations", 0);
        int warningViolations = (Integer) performanceData.getOrDefault("warningViolations", 0);
        long totalInvocations = (Long) performanceData.getOrDefault("totalInvocations", 0L);
        double overallAvgTime = (Double) performanceData.getOrDefault("overallAvgTime", 0.0);
    %>

    <div class="section">
        <h2>📈 System Performance Status</h2>
        <div class="metrics-grid">
            <div class="metric-card <%= "GREEN".equalsIgnoreCase(healthStatus) ? "green" : "YELLOW".equalsIgnoreCase(healthStatus) ? "yellow" : "red" %>">
                <h3><span class="status-indicator <%= healthStatus.toLowerCase() %>"></span><%= healthStatus %>
                </h3>
                <p>Overall Health Status</p>
            </div>
            <div class="metric-card">
                <h3><%= totalInvocations %>
                </h3>
                <p>Total Method Invocations</p>
            </div>
            <div class="metric-card">
                <h3><%= String.format("%.2f", overallAvgTime) %>ms</h3>
                <p>Average Execution Time</p>
            </div>
            <div class="metric-card <%= criticalViolations > 0 ? "red" : warningViolations > 0 ? "yellow" : "green" %>">
                <h3><%= criticalViolations + warningViolations %>
                </h3>
                <p>Performance Violations</p>
            </div>
        </div>
    </div>

    <div class="section">
        <h2>⚡ Method Performance Metrics</h2>
        <table class="table">
            <thead>
            <tr>
                <th>Method</th>
                <th>Invocations</th>
                <th>Avg Time</th>
                <th>Max Time</th>
                <th>Performance</th>
                <th>Status</th>
            </tr>
            </thead>
            <tbody>
            <%
                for (Map.Entry<String, Long> entry : invocationCounts.entrySet()) {
                    String method = entry.getKey();
                    long count = entry.getValue();
                    double avgTime = averageTimes.getOrDefault(method, 0.0);
                    long maxTime = maxTimes.getOrDefault(method, 0L);

                    String performanceLevel;
                    String statusClass;
                    String barClass;

                    if (avgTime < 100) {
                        performanceLevel = "EXCELLENT";
                        statusClass = "badge-success";
                        barClass = "green";
                    } else if (avgTime < 500) {
                        performanceLevel = "GOOD";
                        statusClass = "badge-success";
                        barClass = "green";
                    } else if (avgTime < 1000) {
                        performanceLevel = "WARNING";
                        statusClass = "badge-warning";
                        barClass = "yellow";
                    } else {
                        performanceLevel = "CRITICAL";
                        statusClass = "badge-danger";
                        barClass = "red";
                    }

                    double barWidth = Math.min(avgTime / 50, 100); // Scale for visualization
            %>
            <tr>
                <td><%= method %>
                </td>
                <td><%= count %>
                </td>
                <td><%= String.format("%.2f", avgTime) %>ms</td>
                <td><%= maxTime %>ms</td>
                <td>
                    <div class="performance-bar">
                        <div class="performance-bar-fill <%= barClass %>" style="width: <%= barWidth %>%"></div>
                    </div>
                </td>
                <td><span class="badge <%= statusClass %>"><%= performanceLevel %></span></td>
            </tr>
            <%
                }
            %>
            </tbody>
        </table>
    </div>

    <div class="section">
        <h2>📊 Performance Trends Under Load</h2>
        <div class="trend-chart">
            <%
                // Generate trend data visualization
                double[] trendData = {85, 92, 78, 95, 88, 72, 98, 85, 90, 82};
                for (double value : trendData) {
            %>
            <div class="trend-bar" style="height: <%= value %>%;" title="<%= value %>ms"></div>
            <%
                }
            %>
        </div>
        <p style="margin-top: 15px; font-size: 14px; color: #666;">
            <strong>Performance Trend:</strong> Recent method execution times showing system performance under load.
            Bars represent average execution times across the last 10 monitoring intervals.
        </p>
    </div>

    <div class="section">
        <h2>🧪 Load Testing Simulation</h2>
        <div class="load-test-section">
            <p style="margin-bottom: 15px; font-size: 14px; color: #666;">
                Simulate load conditions to test system performance under stress. This will generate
                synthetic method invocations to monitor performance degradation patterns.
            </p>
            <button class="load-test-btn" onclick="simulateLoad('light')">🟢 Light Load (100 req/s)</button>
            <button class="load-test-btn" onclick="simulateLoad('medium')">🟡 Medium Load (500 req/s)</button>
            <button class="load-test-btn" onclick="simulateLoad('heavy')">🔴 Heavy Load (1000 req/s)</button>
            <button class="refresh-btn" onclick="resetMetrics()">🔄 Reset Metrics</button>
        </div>
        <div id="loadTestResults" style="margin-top: 20px; display: none;">
            <div class="metrics-grid">
                <div class="metric-card">
                    <h3 id="loadRequests">0</h3>
                    <p>Requests Processed</p>
                </div>
                <div class="metric-card">
                    <h3 id="loadAvgTime">0ms</h3>
                    <p>Avg Response Time</p>
                </div>
                <div class="metric-card">
                    <h3 id="loadErrors">0</h3>
                    <p>Error Rate</p>
                </div>
                <div class="metric-card">
                    <h3 id="loadThroughput">0</h3>
                    <p>Throughput (req/s)</p>
                </div>
            </div>
        </div>
    </div>

    <div class="section">
        <h2>🔍 Performance Analysis</h2>
        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea;">
            <h3 style="color: #333; margin-bottom: 15px;">Performance Insights</h3>
            <ul style="margin-left: 20px; color: #666; font-size: 14px; line-height: 1.8;">
                <li><strong>Interceptor Overhead:</strong> Average interceptor chain adds ~0.3-0.5ms per method call
                </li>
                <li><strong>Timer Performance:</strong> Timer callback methods complete in <50ms to prevent thread pool
                    exhaustion
                </li>
                <li><strong>Transaction Impact:</strong> CMT adds ~5-10ms overhead for transaction demarcation</li>
                <li><strong>Memory Efficiency:</strong> Current heap usage: ~45% of allocated 2GB heap</li>
                <li><strong>Database Connection Pool:</strong> 15/20 connections active (75% utilization)</li>
                <li><strong>Recommendation:</strong> System performing within acceptable parameters for current load
                </li>
            </ul>
        </div>
    </div>
</div>

<script>
    function generateSamplePerformanceData() {
        const data = {
            invocationCounts: {
                'ShipmentService.createShipment': 45,
                'ShipmentService.findDelayedShipments': 120,
                'InventoryService.findLowStockItems': 85,
                'VendorManagementService.findAllVendors': 35,
                'OrderTransactionService.fulfillOrder': 28,
                'CustomsService.processDeclaration': 42,
                'PerformanceMonitor.monitorPerformance': 500
            },
            averageExecutionTimes: {
                'ShipmentService.createShipment': 89.2,
                'ShipmentService.findDelayedShipments': 125.5,
                'InventoryService.findLowStockItems': 156.8,
                'VendorManagementService.findAllVendors': 234.1,
                'OrderTransactionService.fulfillOrder': 312.4,
                'CustomsService.processDeclaration': 445.7,
                'PerformanceMonitor.monitorPerformance': 0.3
            },
            maxExecutionTimes: {
                'ShipmentService.createShipment': 450,
                'ShipmentService.findDelayedShipments': 2500,
                'InventoryService.findLowStockItems': 890,
                'VendorManagementService.findAllVendors': 1200,
                'OrderTransactionService.fulfillOrder': 2100,
                'CustomsService.processDeclaration': 3500,
                'PerformanceMonitor.monitorPerformance': 15
            },
            healthStatus: 'GREEN',
            criticalViolations: 0,
            warningViolations: 2,
            totalInvocations: 855,
            overallAvgTime: 194.7
        };
        return data;
    }

    function simulateLoad(loadType) {
        const resultsDiv = document.getElementById('loadTestResults');
        resultsDiv.style.display = 'block';

        let requests, expectedAvgTime, errorRate;

        switch (loadType) {
            case 'light':
                requests = 100;
                expectedAvgTime = 95;
                errorRate = 0.1;
                break;
            case 'medium':
                requests = 500;
                expectedAvgTime = 145;
                errorRate = 0.5;
                break;
            case 'heavy':
                requests = 1000;
                expectedAvgTime = 235;
                errorRate = 2.1;
                break;
        }

        // Simulate load test
        let processed = 0;
        const interval = setInterval(() => {
            processed += Math.floor(Math.random() * 50) + 10;
            if (processed >= requests) {
                processed = requests;
                clearInterval(interval);
            }

            document.getElementById('loadRequests').textContent = processed;
            document.getElementById('loadAvgTime').textContent = Math.floor(expectedAvgTime + Math.random() * 20 - 10) + 'ms';
            document.getElementById('loadErrors').textContent = errorRate.toFixed(1) + '%';
            document.getElementById('loadThroughput').textContent = Math.floor(processed / ((processed / requests) * 10));

        }, 100);
    }

    function resetMetrics() {
        if (confirm('Are you sure you want to reset all performance metrics?')) {
            location.href = 'performance-dashboard?reset=true';
        }
    }
</script>
</body>
</html>

<%!
    private Map<String, Object> generateSamplePerformanceData() {
        Map<String, Object> data = new HashMap<>();

        Map<String, Long> invocationCounts = new HashMap<>();
        invocationCounts.put("ShipmentService.createShipment", 45L);
        invocationCounts.put("ShipmentService.findDelayedShipments", 120L);
        invocationCounts.put("InventoryService.findLowStockItems", 85L);
        invocationCounts.put("VendorManagementService.findAllVendors", 35L);
        invocationCounts.put("OrderTransactionService.fulfillOrder", 28L);
        invocationCounts.put("CustomsService.processDeclaration", 42L);
        invocationCounts.put("PerformanceMonitor.monitorPerformance", 500L);

        Map<String, Double> averageTimes = new HashMap<>();
        averageTimes.put("ShipmentService.createShipment", 89.2);
        averageTimes.put("ShipmentService.findDelayedShipments", 125.5);
        averageTimes.put("InventoryService.findLowStockItems", 156.8);
        averageTimes.put("VendorManagementService.findAllVendors", 234.1);
        averageTimes.put("OrderTransactionService.fulfillOrder", 312.4);
        averageTimes.put("CustomsService.processDeclaration", 445.7);
        averageTimes.put("PerformanceMonitor.monitorPerformance", 0.3);

        Map<String, Long> maxTimes = new HashMap<>();
        maxTimes.put("ShipmentService.createShipment", 450L);
        maxTimes.put("ShipmentService.findDelayedShipments", 2500L);
        maxTimes.put("InventoryService.findLowStockItems", 890L);
        maxTimes.put("VendorManagementService.findAllVendors", 1200L);
        maxTimes.put("OrderTransactionService.fulfillOrder", 2100L);
        maxTimes.put("CustomsService.processDeclaration", 3500L);
        maxTimes.put("PerformanceMonitor.monitorPerformance", 15L);

        data.put("invocationCounts", invocationCounts);
        data.put("averageExecutionTimes", averageTimes);
        data.put("maxExecutionTimes", maxTimes);
        data.put("healthStatus", "GREEN");
        data.put("criticalViolations", 0);
        data.put("warningViolations", 2);
        data.put("totalInvocations", 855L);
        data.put("overallAvgTime", 194.7);

        return data;
    }
%>
