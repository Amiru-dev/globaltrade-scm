<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="com.globaltrade.common.entity.AuditLog" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.stream.Collectors" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Audit Log</title>
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
        }
        
        .user-info span {
            font-size: 14px;
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
        
        .interceptor-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            border-left: 4px solid #667eea;
        }
        
        .interceptor-info h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 10px;
        }
        
        .interceptor-list {
            list-style: none;
        }
        
        .interceptor-list li {
            padding: 8px 0;
            border-bottom: 1px solid #e0e0e0;
            font-size: 14px;
            color: #666;
        }
        
        .interceptor-list li:last-child {
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
        
        .badge-danger {
            background: #f8d7da;
            color: #721c24;
        }
        
        .badge-warning {
            background: #fff3cd;
            color: #856404;
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
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }
        
        .stat-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
        }
        
        .stat-card h3 {
            font-size: 24px;
            font-weight: 700;
            margin-bottom: 5px;
        }
        
        .stat-card p {
            font-size: 12px;
            opacity: 0.9;
        }
    </style>
</head>
<body>
    <div class="header">
        <div>
            <h1>📋 GlobalTrade SCM Audit Log</h1>
            <p style="font-size: 14px; opacity: 0.9; margin-top: 5px;">Interceptor-Generated Audit Trail</p>
        </div>
        <div class="user-info">
            <div>
                <span style="font-weight: 600;">👤 <%= request.getAttribute("username") %></span>
                <span style="opacity: 0.8;">|</span>
                <span><%= request.getAttribute("roleDescription") %></span>
            </div>
            <div class="access-level">
                Access Level: <%= ((Map<String, Object>) request.getAttribute("auditData")).get("accessLevel") %>
            </div>
            <a href="login?logout=true" class="logout-btn">Logout</a>
        </div>
    </div>
    
    <div class="nav-bar">
        <a href="dashboard">📊 Dashboard</a>
        <a href="auditlog" class="active">📋 Audit Log</a>
        <a href="transaction">💳 Transaction Demo</a>
        <a href="admin">⚙️ Admin</a>
    </div>
    
    <div class="container">
        <% 
            Map<String, Object> auditData = (Map<String, Object>) request.getAttribute("auditData");
            if (auditData == null) {
                auditData = new HashMap<>();
                auditData.put("accessLevel", "Error - Data not available");
                auditData.put("totalEntries", 0);
            }
            
            Map<String, Object> interceptorInfo = (Map<String, Object>) auditData.getOrDefault("interceptorInfo", new HashMap<>());
            List<AuditLog> auditLogs = (List<AuditLog>) auditData.getOrDefault("auditLogs", new ArrayList<>());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        %>
        
        <div class="section">
            <h2>🔍 Active Interceptors</h2>
            <div class="interceptor-info">
                <h3>EJB Interceptor Chain</h3>
                <ul class="interceptor-list">
                    <li>📝 AuditLogInterceptor: <%= interceptorInfo.get("auditLogInterceptor") %></li>
                    <li>🏛️ CustomsRegulationsInterceptor: <%= interceptorInfo.get("customsRegulationsInterceptor") %></li>
                    <li>⚡ PerformanceMonitor: <%= interceptorInfo.get("performanceMonitor") %></li>
                    <li>🔒 SecurityAuditInterceptor: <%= interceptorInfo.get("securityAuditInterceptor") %></li>
                    <li>✅ VendorDataValidator: <%= interceptorInfo.get("vendorDataValidator") %></li>
                </ul>
                <p style="margin-top: 10px; font-size: 13px; color: #666;">
                    These interceptors automatically generate audit entries for all business method invocations.
                </p>
            </div>
        </div>
        
        <div class="section">
            <h2>📊 Audit Statistics</h2>
            <div class="stats-grid">
                <div class="stat-card">
                    <h3><%= auditData.get("totalEntries") %></h3>
                    <p>Total Entries</p>
                </div>
                <div class="stat-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                    <h3><%= auditLogs.stream().filter(l -> "SUCCESS".equals(l.getResultStatus())).count() %></h3>
                    <p>Successful</p>
                </div>
                <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                    <h3><%= auditLogs.stream().filter(l -> "FAILURE".equals(l.getResultStatus())).count() %></h3>
                    <p>Failed</p>
                </div>
                <div class="stat-card" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
                    <h3><%= Math.round(auditLogs.stream().mapToLong(l -> l.getExecutionTimeMs() != null ? l.getExecutionTimeMs() : 0L).average().orElse(0)) %></h3>
                    <p>Avg Time (ms)</p>
                </div>
            </div>
        </div>
        
        <div class="section">
            <h2>📋 Audit Log Entries</h2>
            <% if (auditLogs != null && !auditLogs.isEmpty()) { %>
            <table class="table">
                <thead>
                    <tr>
                        <th>Timestamp</th>
                        <th>User</th>
                        <th>Role</th>
                        <th>Action</th>
                        <th>Entity</th>
                        <th>Entity ID</th>
                        <th>Method</th>
                        <th>Status</th>
                        <th>Time (ms)</th>
                    </tr>
                </thead>
                <tbody>
                    <% for (AuditLog log : auditLogs) { %>
                    <tr>
                        <td><%= log.getTimestamp() != null ? log.getTimestamp().format(formatter) : "N/A" %></td>
                        <td><%= log.getUserId() %></td>
                        <td><span class="badge badge-info"><%= log.getUserRole() %></span></td>
                        <td><span class="badge <%= "CREATE".equals(log.getAction()) || "DELETE".equals(log.getAction()) ? "badge-warning" : "badge-info" %>"><%= log.getAction() %></span></td>
                        <td><%= log.getEntityType() %></td>
                        <td><%= log.getEntityId() %></td>
                        <td><%= log.getMethodName() %></td>
                        <td><span class="badge <%= "SUCCESS".equals(log.getResultStatus()) ? "badge-success" : "badge-danger" %>"><%= log.getResultStatus() %></span></td>
                        <td><%= log.getExecutionTimeMs() %></td>
                    </tr>
                    <% } %>
                </tbody>
            </table>
            <% } else { %>
            <div class="no-data">No audit log entries available for your access level</div>
            <% } %>
        </div>
        
        <div class="section">
            <h2>ℹ️ System Information</h2>
            <div class="interceptor-info">
                <p><strong>Current Time:</strong> <%= request.getAttribute("currentTime") %></p>
                <p><strong>User Role:</strong> <%= request.getAttribute("userRole") %></p>
                <p><strong>Access Level:</strong> <%= auditData.get("accessLevel") %></p>
                <p><strong>Interceptor Status:</strong> All interceptors active and generating audit entries</p>
            </div>
        </div>
    </div>
</body>
</html>