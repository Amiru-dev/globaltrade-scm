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
import java.util.HashMap;
import java.util.Map;

/**
 * Login servlet for role-based authentication demonstration.
 * 
 * <p><strong>Critical Analysis:</strong> This servlet provides a simplified
 * authentication mechanism for demonstration purposes. In a production environment,
 * this would integrate with JAAS realm configuration and proper authentication
 * mechanisms. For assignment demonstration, we use role selection to show
 * different access patterns.</p>
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @EJB
    private SecurityService securityService;

    // Role definitions for demonstration
    private static final Map<String, String> ROLE_DESCRIPTIONS = new HashMap<>();
    
    static {
        ROLE_DESCRIPTIONS.put("LOGISTICS_COORDINATOR", "Shipment management, inventory operations, route optimization");
        ROLE_DESCRIPTIONS.put("WAREHOUSE_MANAGER", "Inventory management, warehouse operations");
        ROLE_DESCRIPTIONS.put("CUSTOMS_AGENT", "Customs declarations, compliance reports");
        ROLE_DESCRIPTIONS.put("VENDOR_REPRESENTATIVE", "View own orders, update delivery status");
        ROLE_DESCRIPTIONS.put("SYSTEM_ADMIN", "Full system access, user management, emergency overrides");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Check for logout request
        String logout = request.getParameter("logout");
        if ("true".equals(logout)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String username = (String) session.getAttribute("username");
                String role = (String) session.getAttribute("userRole");
                System.out.println("LOGOUT: User '" + username + "' with role: " + role);
                session.invalidate();
            }
            response.sendRedirect("login");
            return;
        }
        
        // Check if user is already logged in
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userRole") != null) {
            response.sendRedirect("dashboard");
            return;
        }
        
        // Forward to login page
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String username = request.getParameter("username");
        String role = request.getParameter("role");
        
        // Validate role selection
        if (role == null || role.isEmpty() || !ROLE_DESCRIPTIONS.containsKey(role)) {
            request.setAttribute("error", "Invalid role selection");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
            return;
        }
        
        // Create session and set user attributes
        HttpSession session = request.getSession(true);
        session.setAttribute("username", username != null ? username : "demo_user");
        session.setAttribute("userRole", role);
        session.setAttribute("roleDescription", ROLE_DESCRIPTIONS.get(role));
        session.setAttribute("loginTime", System.currentTimeMillis());
        
        // Log the login for demonstration
        System.out.println("LOGIN: User '" + username + "' logged in as role: " + role);
        
        // Redirect to dashboard
        response.sendRedirect("dashboard");
    }
}