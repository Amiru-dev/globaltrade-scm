<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GlobalTrade SCM - Login</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .login-container {
            background: white;
            border-radius: 10px;
            box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
            padding: 40px;
            width: 100%;
            max-width: 450px;
        }
        
        .header {
            text-align: center;
            margin-bottom: 30px;
        }
        
        .header h1 {
            color: #333;
            font-size: 28px;
            margin-bottom: 10px;
        }
        
        .header p {
            color: #666;
            font-size: 14px;
        }
        
        .form-group {
            margin-bottom: 20px;
        }
        
        .form-group label {
            display: block;
            margin-bottom: 8px;
            color: #333;
            font-weight: 600;
            font-size: 14px;
        }
        
        .form-group input,
        .form-group select {
            width: 100%;
            padding: 12px;
            border: 2px solid #e0e0e0;
            border-radius: 5px;
            font-size: 14px;
            transition: border-color 0.3s;
        }
        
        .form-group input:focus,
        .form-group select:focus {
            outline: none;
            border-color: #667eea;
        }
        
        .role-description {
            background: #f8f9fa;
            padding: 10px;
            border-radius: 5px;
            font-size: 12px;
            color: #666;
            margin-top: 8px;
            border-left: 3px solid #667eea;
        }
        
        .btn {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        
        .btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        
        .error {
            background: #fee;
            color: #c33;
            padding: 10px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 14px;
            border-left: 3px solid #c33;
        }
        
        .info {
            background: #e8f4f8;
            color: #2c5f7c;
            padding: 15px;
            border-radius: 5px;
            margin-top: 20px;
            font-size: 12px;
            border-left: 3px solid #2c5f7c;
        }
        
        .role-icons {
            display: flex;
            gap: 10px;
            margin-top: 20px;
            justify-content: center;
        }
        
        .role-icon {
            width: 40px;
            height: 40px;
            background: #f0f0f0;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="header">
            <h1>GlobalTrade SCM</h1>
            <p>Supply Chain Management System</p>
        </div>
        
        <% if (request.getAttribute("error") != null) { %>
            <div class="error">
                <%= request.getAttribute("error") %>
            </div>
        <% } %>
        
        <form action="login" method="post">
            <div class="form-group">
                <label for="username">Username</label>
                <input type="text" id="username" name="username" placeholder="Enter your username" required>
            </div>
            
            <div class="form-group">
                <label for="role">Select Your Role</label>
                <select id="role" name="role" required onchange="updateRoleDescription()">
                    <option value="">-- Select Role --</option>
                    <option value="LOGISTICS_COORDINATOR">Logistics Coordinator</option>
                    <option value="WAREHOUSE_MANAGER">Warehouse Manager</option>
                    <option value="CUSTOMS_AGENT">Customs Agent</option>
                    <option value="VENDOR_REPRESENTATIVE">Vendor Representative</option>
                    <option value="SYSTEM_ADMIN">System Administrator</option>
                </select>
                <div id="roleDescription" class="role-description" style="display: none;">
                    Select a role to see permissions
                </div>
            </div>
            
            <button type="submit" class="btn">Login to Dashboard</button>
        </form>
        
        <div class="info">
            <strong>Demonstration Mode:</strong> This login simulates role-based access control for EJB security demonstration. Different roles will see different dashboard features and permissions.
        </div>
        
        <div class="role-icons">
            <div class="role-icon">📦</div>
            <div class="role-icon">🚛</div>
            <div class="role-icon">📋</div>
            <div class="role-icon">🔐</div>
        </div>
    </div>
    
    <script>
        const roleDescriptions = {
            'LOGISTICS_COORDINATOR': 'Shipment management, inventory operations, route optimization',
            'WAREHOUSE_MANAGER': 'Inventory management, warehouse operations',
            'CUSTOMS_AGENT': 'Customs declarations, compliance reports',
            'VENDOR_REPRESENTATIVE': 'View own orders, update delivery status',
            'SYSTEM_ADMIN': 'Full system access, user management, emergency overrides'
        };
        
        function updateRoleDescription() {
            const roleSelect = document.getElementById('role');
            const descriptionDiv = document.getElementById('roleDescription');
            const selectedRole = roleSelect.value;
            
            if (selectedRole && roleDescriptions[selectedRole]) {
                descriptionDiv.style.display = 'block';
                descriptionDiv.textContent = roleDescriptions[selectedRole];
            } else {
                descriptionDiv.style.display = 'none';
            }
        }
    </script>
</body>
</html>