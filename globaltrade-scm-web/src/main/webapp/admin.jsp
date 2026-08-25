<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.ArrayList" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Admin Panel</title>
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

        .security-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            border-left: 4px solid #667eea;
        }

        .security-info h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 10px;
        }

        .security-info p {
            font-size: 14px;
            color: #666;
            margin-bottom: 8px;
        }

        .permissions-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }

        .permission-card {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            border: 1px solid #e0e0e0;
        }

        .permission-card h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 10px;
        }

        .permission-card ul {
            list-style: none;
            padding: 0;
        }

        .permission-card li {
            font-size: 13px;
            color: #666;
            padding: 4px 0;
            border-bottom: 1px solid #e0e0e0;
        }

        .permission-card li:last-child {
            border-bottom: none;
        }

        .form-group {
            margin-bottom: 15px;
        }

        .form-group label {
            display: block;
            margin-bottom: 5px;
            color: #333;
            font-weight: 600;
            font-size: 14px;
        }

        .form-group input,
        .form-group select,
        .form-group textarea {
            width: 100%;
            padding: 10px;
            border: 1px solid #e0e0e0;
            border-radius: 5px;
            font-size: 14px;
        }

        .form-group textarea {
            resize: vertical;
            min-height: 80px;
        }

        .btn {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }

        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }

        .btn-danger {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        }

        .btn:disabled {
            background: #ccc;
            cursor: not-allowed;
            transform: none;
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

        .result-box {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-top: 20px;
            border-left: 4px solid #667eea;
        }

        .result-box.success {
            border-left-color: #28a745;
            background: #d4edda;
        }

        .result-box.error {
            border-left-color: #dc3545;
            background: #f8d7da;
        }

        .result-box h4 {
            color: #333;
            font-size: 16px;
            margin-bottom: 10px;
        }

        .result-box p {
            font-size: 14px;
            color: #666;
            margin-bottom: 5px;
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

        .admin-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }

        .admin-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            border: 1px solid #e0e0e0;
        }

        .admin-card h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 15px;
        }

        .no-access {
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
        <h1>⚙️ GlobalTrade SCM Admin Panel</h1>
        <p style="font-size: 14px; opacity: 0.9; margin-top: 5px;">Advanced Security & Emergency Access</p>
    </div>
    <div class="user-info">
        <div class="username">👤 <%= request.getAttribute("username") %></div>
        <div class="role-info" title="<%= request.getAttribute("roleDescription") %>">
            <%= request.getAttribute("roleDescription") %>
        </div>
        <div class="access-level">
            Access Level: <%= ((Map<String, Object>) request.getAttribute("adminData")).get("accessLevel") %>
        </div>
        <a href="login?logout=true" class="logout-btn">Logout</a>
    </div>
</div>

<div class="nav-bar">
    <a href="dashboard">📊 Dashboard</a>
    <a href="performance-dashboard">⚡ Performance</a>
    <a href="auditlog">📋 Audit Log</a>
    <a href="transaction">💳 Transaction Demo</a>
    <a href="admin" class="active">⚙️ Admin</a>
</div>

<div class="container">
    <%
        Map<String, Object> adminData = (Map<String, Object>) request.getAttribute("adminData");
        if (adminData == null) {
            adminData = new HashMap<>();
            adminData.put("accessLevel", "Error - Data not available");
            adminData.put("hasAdminAccess", false);
            adminData.put("canPerformEmergencyAccess", false);
            adminData.put("canPerformHighValueOperations", false);
            adminData.put("canScheduleMaintenance", false);
        }

        Map<String, Object> securityFeatures = (Map<String, Object>) adminData.getOrDefault("securityFeatures", new HashMap<>());
        Map<String, List<String>> rolePermissions = (Map<String, List<String>>) adminData.getOrDefault("rolePermissions", new HashMap<>());
        List<Map<String, String>> emergencyAttempts = (List<Map<String, String>>) adminData.getOrDefault("emergencyAttempts", new ArrayList<>());
        HttpSession httpSession = request.getSession(false);
        Map<String, Object> adminResult = (httpSession != null) ? (Map<String, Object>) httpSession.getAttribute("adminResult") : null;
    %>

    <div class="section">
        <h2>🔒 Advanced Security Features</h2>
        <div class="security-info">
            <h3>Programmatic Authorization</h3>
            <p><%= securityFeatures.get("programmaticAuthorization") %>
            </p>
            <h3>Emergency Access Override</h3>
            <p><%= securityFeatures.get("emergencyAccess") %>
            </p>
            <h3>High-Value Authorization</h3>
            <p><%= securityFeatures.get("highValueAuthorization") %>
            </p>
            <h3>Maintenance Windows</h3>
            <p><%= securityFeatures.get("maintenanceWindows") %>
            </p>
            <h3>Data-Level Security</h3>
            <p><%= securityFeatures.get("dataLevelSecurity") %>
            </p>
        </div>
    </div>

    <div class="section">
        <h2>👥 Role Permissions Matrix</h2>
        <div class="permissions-grid">
            <% for (Map.Entry<String, List<String>> entry : rolePermissions.entrySet()) { %>
            <div class="permission-card">
                <h3><%= entry.getKey() %>
                </h3>
                <ul>
                    <% for (String permission : entry.getValue()) { %>
                    <li>✓ <%= permission %>
                    </li>
                    <% } %>
                </ul>
            </div>
            <% } %>
        </div>
    </div>

    <% if (Boolean.TRUE.equals(adminData.get("hasAdminAccess"))) { %>

    <div class="section">
        <h2>🚨 Admin Operations</h2>
        <div class="admin-grid">
            <div class="admin-card">
                <h3>🚨 Emergency Access Override</h3>
                <form action="admin" method="post">
                    <input type="hidden" name="operation" value="emergency_access">
                    <input type="hidden" name="<%= request.getAttribute("csrfParamName") %>" value="<%= request.getAttribute("csrfToken") %>">
                    <div class="form-group">
                        <label>Resource Type</label>
                        <select name="resourceType" required>
                            <option value="SHIPMENT">Shipment</option>
                            <option value="INVENTORY">Inventory</option>
                            <option value="CUSTOMS">Customs</option>
                            <option value="VENDOR">Vendor</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Emergency Reason</label>
                        <textarea name="reason" placeholder="Describe the emergency situation" required></textarea>
                    </div>
                    <button type="submit"
                            class="btn btn-danger" <%= !Boolean.TRUE.equals(adminData.get("canPerformEmergencyAccess")) ? "disabled" : "" %>>
                        <%= Boolean.TRUE.equals(adminData.get("canPerformEmergencyAccess")) ? "Request Emergency Access" : "Admin Only" %>
                    </button>
                </form>
            </div>

            <div class="admin-card">
                <h3>💰 High-Value Operation Authorization</h3>
                <form action="admin" method="post">
                    <input type="hidden" name="operation" value="high_value_operation">
                    <input type="hidden" name="<%= request.getAttribute("csrfParamName") %>" value="<%= request.getAttribute("csrfToken") %>">
                    <div class="form-group">
                        <label>Operation Type</label>
                        <select name="operationType" required>
                            <option value="SHIPMENT">Shipment Processing</option>
                            <option value="CONTRACT">Contract Approval</option>
                            <option value="PAYMENT">Payment Processing</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Declared Value ($)</label>
                        <input type="number" name="declaredValue" value="50000" required>
                    </div>
                    <button type="submit"
                            class="btn" <%= !Boolean.TRUE.equals(adminData.get("canPerformHighValueOperations")) ? "disabled" : "" %>>
                        <%= Boolean.TRUE.equals(adminData.get("canPerformHighValueOperations")) ? "Authorize Operation" : "Admin/Coordinator Only" %>
                    </button>
                </form>
            </div>

            <div class="admin-card">
                <h3>🕐 Maintenance Window Check</h3>
                <form action="admin" method="post">
                    <input type="hidden" name="operation" value="maintenance_window">
                    <input type="hidden" name="<%= request.getAttribute("csrfParamName") %>" value="<%= request.getAttribute("csrfToken") %>">
                    <div class="form-group">
                        <label>Operation Type</label>
                        <select name="operationType" required>
                            <option value="SYSTEM_UPDATE">System Update</option>
                            <option value="DATABASE_MAINTENANCE">Database Maintenance</option>
                            <option value="CONFIGURATION_CHANGE">Configuration Change</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Scheduled Time</label>
                        <input type="datetime-local" name="scheduledTime" required>
                    </div>
                    <button type="submit"
                            class="btn" <%= !Boolean.TRUE.equals(adminData.get("canScheduleMaintenance")) ? "disabled" : "" %>>
                        <%= Boolean.TRUE.equals(adminData.get("canScheduleMaintenance")) ? "Check Maintenance Window" : "Admin Only" %>
                    </button>
                </form>
            </div>
        </div>
    </div>

    <% if (adminResult != null) { %>
    <div class="section">
        <h2>📊 Admin Operation Result</h2>
        <div class="result-box <%= Boolean.TRUE.equals(adminResult.get("success")) ? "success" : "error" %>">
            <h4>Operation Status: <%= Boolean.TRUE.equals(adminResult.get("success")) ? "✅ SUCCESS" : "❌ FAILED" %>
            </h4>
            <p><strong>Operation:</strong> <%= adminResult.get("operation") %>
            </p>
            <% if (adminResult.containsKey("resourceType")) { %>
            <p><strong>Resource Type:</strong> <%= adminResult.get("resourceType") %>
            </p>
            <% } %>
            <% if (adminResult.containsKey("reason")) { %>
            <p><strong>Reason:</strong> <%= adminResult.get("reason") %>
            </p>
            <% } %>
            <% if (adminResult.containsKey("authorized")) { %>
            <p><strong>Authorized:</strong> <span
                    class="badge <%= Boolean.TRUE.equals(adminResult.get("authorized")) ? "badge-success" : "badge-danger" %>"><%= adminResult.get("authorized") %></span>
            </p>
            <% } %>
            <p><strong>Message:</strong> <%= adminResult.get("message") %>
            </p>
            <p><strong>Timestamp:</strong> <%= adminResult.get("timestamp") %>
            </p>
        </div>
        <% httpSession.removeAttribute("adminResult"); %>
    </div>
    <% } %>

    <div class="section">
        <h2>📋 Recent Emergency Access Attempts</h2>
        <table class="table">
            <thead>
            <tr>
                <th>User</th>
                <th>Resource</th>
                <th>Reason</th>
                <th>Status</th>
                <th>Timestamp</th>
            </tr>
            </thead>
            <tbody>
            <% for (Map<String, String> attempt : emergencyAttempts) { %>
            <tr>
                <td><%= attempt.get("user") %>
                </td>
                <td><%= attempt.get("resource") %>
                </td>
                <td><%= attempt.get("reason") %>
                </td>
                <td><span
                        class="badge <%= "GRANTED".equals(attempt.get("granted")) ? "badge-success" : "badge-danger" %>"><%= attempt.get("granted") %></span>
                </td>
                <td><%= attempt.get("timestamp") %>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
    </div>

    <% } else { %>
    <div class="section">
        <div class="no-access">
            <h3>🔒 Admin Access Restricted</h3>
            <p>Your role does not have administrator privileges.</p>
            <p>Please contact a system administrator or log in with administrative credentials.</p>
        </div>
    </div>
    <% } %>

    <div class="section">
        <h2>ℹ️ System Information</h2>
        <div class="security-info">
            <p><strong>Current Time:</strong> <%= request.getAttribute("currentTime") %>
            </p>
            <p><strong>User Role:</strong> <%= request.getAttribute("userRole") %>
            </p>
            <p><strong>Access Level:</strong> <%= adminData.get("accessLevel") %>
            </p>
            <p><strong>Admin Access:</strong> <%= ((Boolean) adminData.get("hasAdminAccess")) ? "Granted" : "Denied" %>
            </p>
        </div>
    </div>
</div>
</body>
</html>