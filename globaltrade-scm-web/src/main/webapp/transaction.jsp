<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Transaction Demo</title>
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

        .transaction-info {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 5px;
            margin-bottom: 20px;
            border-left: 4px solid #667eea;
        }

        .transaction-info h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 10px;
        }

        .transaction-info p {
            font-size: 14px;
            color: #666;
            margin-bottom: 8px;
        }

        .transaction-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-bottom: 20px;
        }

        .transaction-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            border: 1px solid #e0e0e0;
        }

        .transaction-card h3 {
            color: #333;
            font-size: 16px;
            margin-bottom: 15px;
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
        .form-group select {
            width: 100%;
            padding: 10px;
            border: 1px solid #e0e0e0;
            border-radius: 5px;
            font-size: 14px;
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

        .attributes-table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 15px;
        }

        .attributes-table th,
        .attributes-table td {
            padding: 10px;
            text-align: left;
            border-bottom: 1px solid #e0e0e0;
        }

        .attributes-table th {
            background: #f8f9fa;
            font-weight: 600;
            color: #333;
            font-size: 14px;
        }

        .attributes-table td {
            font-size: 14px;
            color: #666;
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
        <h1>💳 GlobalTrade SCM Transaction Demo</h1>
        <p style="font-size: 14px; opacity: 0.9; margin-top: 5px;">EJB Transaction Management</p>
    </div>
    <div class="user-info">
        <div class="username">👤 <%= request.getAttribute("username") %></div>
        <div class="role-info" title="<%= request.getAttribute("roleDescription") %>">
            <%= request.getAttribute("roleDescription") %>
        </div>
        <div class="access-level">
            Access Level: <%= ((Map<String, Object>) request.getAttribute("transactionData")).get("accessLevel") %>
        </div>
        <a href="login?logout=true" class="logout-btn">Logout</a>
    </div>
</div>

<div class="nav-bar">
    <a href="dashboard">📊 Dashboard</a>
    <a href="performance-dashboard">⚡ Performance</a>
    <a href="auditlog">📋 Audit Log</a>
    <a href="transaction" class="active">💳 Transaction Demo</a>
    <a href="admin">⚙️ Admin</a>
</div>

<div class="container">
    <%
        Map<String, Object> transactionData = (Map<String, Object>) request.getAttribute("transactionData");
        if (transactionData == null) {
            transactionData = new HashMap<>();
            transactionData.put("accessLevel", "Error - Data not available");
            transactionData.put("canExecuteTransactions", false);
        }

        Map<String, Object> transactionInfo = (Map<String, Object>) transactionData.getOrDefault("transactionInfo", new HashMap<>());
        Map<String, String> transactionAttributes = (Map<String, String>) transactionData.getOrDefault("transactionAttributes", new HashMap<>());
        HttpSession httpSession = request.getSession(false);
        Map<String, Object> transactionResult = (httpSession != null) ? (Map<String, Object>) httpSession.getAttribute("transactionResult") : null;
    %>

    <div class="section">
        <h2>📚 Transaction Management Concepts</h2>
        <div class="transaction-info">
            <h3>Container-Managed Transactions (CMT)</h3>
            <p><%= transactionInfo.get("cmtExplanation") %>
            </p>
            <h3>Bean-Managed Transactions (BMT)</h3>
            <p><%= transactionInfo.get("bmtExplanation") %>
            </p>
            <h3>Default Behavior</h3>
            <p><%= transactionInfo.get("defaultAttribute") %>
            </p>
            <h3>Rollback Conditions</h3>
            <p><%= transactionInfo.get("rollbackConditions") %>
            </p>
        </div>
    </div>

    <div class="section">
        <h2>🎯 Transaction Attributes</h2>
        <table class="attributes-table">
            <thead>
            <tr>
                <th>Attribute</th>
                <th>Description</th>
            </tr>
            </thead>
            <tbody>
            <% for (Map.Entry<String, String> entry : transactionAttributes.entrySet()) { %>
            <tr>
                <td><strong><%= entry.getKey() %>
                </strong></td>
                <td><%= entry.getValue() %>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
    </div>

    <% if (Boolean.TRUE.equals(transactionData.get("canExecuteTransactions"))) { %>

    <div class="section">
        <h2>🔄 Transaction Operations</h2>
        <div class="transaction-grid">
            <div class="transaction-card">
                <h3>📦 Inventory Transfer</h3>
                <form action="transaction" method="post">
                    <input type="hidden" name="operation" value="transfer">
                    <div class="form-group">
                        <label>From Item ID</label>
                        <input type="number" name="fromItemId" value="1" required>
                    </div>
                    <div class="form-group">
                        <label>To Item ID</label>
                        <input type="number" name="toItemId" value="2" required>
                    </div>
                    <div class="form-group">
                        <label>Quantity</label>
                        <input type="number" name="quantity" value="10" required>
                    </div>
                    <button type="submit" class="btn">Execute Transfer</button>
                </form>
            </div>

            <div class="transaction-card">
                <h3>➖ Stock Deduction</h3>
                <form action="transaction" method="post">
                    <input type="hidden" name="operation" value="deduct">
                    <div class="form-group">
                        <label>Item ID</label>
                        <input type="number" name="itemId" value="1" required>
                    </div>
                    <div class="form-group">
                        <label>Quantity</label>
                        <input type="number" name="quantity" value="5" required>
                    </div>
                    <button type="submit" class="btn">Execute Deduct</button>
                </form>
            </div>

            <div class="transaction-card">
                <h3>➕ Stock Replenishment</h3>
                <form action="transaction" method="post">
                    <input type="hidden" name="operation" value="replenish">
                    <div class="form-group">
                        <label>Item ID</label>
                        <input type="number" name="itemId" value="1" required>
                    </div>
                    <div class="form-group">
                        <label>Quantity</label>
                        <input type="number" name="quantity" value="50" required>
                    </div>
                    <button type="submit" class="btn">Execute Replenish</button>
                </form>
            </div>

            <div class="transaction-card">
                <h3>↩️ Rollback Demo</h3>
                <form action="transaction" method="post">
                    <input type="hidden" name="operation" value="rollback">
                    <div class="form-group">
                        <label>Item ID</label>
                        <input type="number" name="itemId" value="1" required>
                    </div>
                    <div class="form-group">
                        <label>Quantity (Large to trigger rollback)</label>
                        <input type="number" name="quantity" value="9999" required>
                    </div>
                    <button type="submit" class="btn btn-danger">Test Rollback</button>
                </form>
            </div>
        </div>
    </div>

    <% if (transactionResult != null) { %>
    <div class="section">
        <h2>📊 Transaction Result</h2>
        <div class="result-box <%= Boolean.TRUE.equals(transactionResult.get("success")) ? "success" : "error" %>">
            <h4>Transaction Status: <%= Boolean.TRUE.equals(transactionResult.get("success")) ? "✅ SUCCESS" : "❌ FAILED" %>
            </h4>
            <p><strong>Operation:</strong> <%= transactionResult.get("operation") %>
            </p>
            <p><strong>Transaction Type:</strong> <%= transactionResult.get("transactionType") %>
            </p>
            <p><strong>Transaction Attribute:</strong> <%= transactionResult.get("transactionAttribute") %>
            </p>
            <p><strong>Outcome:</strong> <span
                    class="badge <%= "COMMIT".equals(transactionResult.get("outcome")) ? "badge-success" : "badge-danger" %>"><%= transactionResult.get("outcome") %></span>
            </p>
            <p><strong>Message:</strong> <%= transactionResult.get("message") %>
            </p>
            <p><strong>Timestamp:</strong> <%= transactionResult.get("timestamp") %>
            </p>
            <% if (transactionResult.containsKey("rollbackReason")) { %>
            <p><strong>Rollback Reason:</strong> <%= transactionResult.get("rollbackReason") %>
            </p>
            <% } %>
        </div>
        <% httpSession.removeAttribute("transactionResult"); %>
    </div>
    <% } %>

    <% } else { %>
    <div class="section">
        <div class="no-access">
            <h3>🔒 Transaction Execution Restricted</h3>
            <p>Your role does not have permission to execute transactions.</p>
            <p>Please contact an administrator or log in with a different role.</p>
        </div>
    </div>
    <% } %>

    <div class="section">
        <h2>ℹ️ System Information</h2>
        <div class="transaction-info">
            <p><strong>Current Time:</strong> <%= request.getAttribute("currentTime") %>
            </p>
            <p><strong>User Role:</strong> <%= request.getAttribute("userRole") %>
            </p>
            <p><strong>Access Level:</strong> <%= transactionData.get("accessLevel") %>
            </p>
            <p><strong>Transaction
                Execution:</strong> <%= Boolean.TRUE.equals(transactionData.get("canExecuteTransactions")) ? "Enabled" : "Disabled" %>
            </p>
        </div>
    </div>
</div>
</body>
</html>