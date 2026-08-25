package com.globaltrade.web.servlet;

import com.globaltrade.common.entity.User;
import com.globaltrade.ejb.service.UserService;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.SecurityContext;
import jakarta.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Login servlet for proper authentication with user credentials.
 *
 * <p><strong>Critical Analysis:</strong> Credential verification itself still happens in
 * {@link UserService#authenticate(String, String)} (BCrypt + account lockout). What changed
 * is that {@code doPost} now drives that check through {@link SecurityContext#authenticate}
 * instead of calling {@code userService.authenticate()} directly. This routes the result
 * through {@code GlobalTradeIdentityStore} / {@code GlobalTradeAuthenticationMechanism},
 * which establishes a real Jakarta EE container security context (caller principal + groups)
 * for the session — not just {@code HttpSession} attributes. That container-level identity is
 * what downstream {@code @EJB}-injected calls (e.g. {@code InventoryService}, which is
 * annotated {@code @RolesAllowed}) are authorized against.</p>
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

    @EJB
    private UserService userService;

    @Inject
    private SecurityContext securityContext;

    // Role descriptions for display purposes
    private static final String ROLE_DESCRIPTIONS =
            "LOGISTICS_COORDINATOR: Shipment management, inventory operations, route optimization\n" +
                    "WAREHOUSE_MANAGER: Inventory management, warehouse operations\n" +
                    "CUSTOMS_AGENT: Customs declarations, compliance reports\n" +
                    "VENDOR_REPRESENTATIVE: View own orders, update delivery status\n" +
                    "SYSTEM_ADMIN: Full system access, user management, emergency overrides";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Check for logout request
        String logout = request.getParameter("logout");
        if ("true".equals(logout)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String username = (String) session.getAttribute("username");
                String roles = (String) session.getAttribute("userRoles");
                LOGGER.log(Level.INFO, "LOGOUT: User '{0}' with roles: {1}", new Object[]{username, roles});
                session.invalidate();
            }
            response.sendRedirect("login");
            return;
        }

        // Check if user is already logged in
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("username") != null) {
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
        String password = request.getParameter("password");

        // Validate input
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            request.setAttribute("error", "Username and password are required");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
            return;
        }

        try {
            // Authenticate against the container security context. Internally this invokes
            // GlobalTradeAuthenticationMechanism -> GlobalTradeIdentityStore ->
            // UserService.authenticate() (BCrypt + account lockout), and on success
            // establishes the real caller principal/groups that @RolesAllowed EJBs check.
            AuthenticationStatus status = securityContext.authenticate(
                    request, response,
                    AuthenticationParameters.withParams()
                            .credential(new UsernamePasswordCredential(username, password)));

            if (status != AuthenticationStatus.SUCCESS) {
                LOGGER.log(Level.WARNING, "LOGIN: Authentication failed for user: {0}", username);
                request.setAttribute("error", "Invalid username or password");
                request.getRequestDispatcher("/index.jsp").forward(request, response);
                return;
            }

            // Credentials were already verified above; this lookup is only for display
            // attributes (fullName, id) and does not re-check the password.
            User user = userService.findByUsername(username);

            // Get user roles
            Set<String> roles = user.getRoles();
            if (roles == null || roles.isEmpty()) {
                request.setAttribute("error", "User has no assigned roles. Please contact administrator.");
                request.getRequestDispatcher("/index.jsp").forward(request, response);
                return;
            }

            // Create session and set user attributes (kept for JSP/UI display purposes;
            // authorization itself is now enforced by the container, not these attributes)
            HttpSession session = request.getSession(true);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("userId", user.getId());
            session.setAttribute("userRoles", String.join(",", roles));
            session.setAttribute("userRole", roles.iterator().next()); // Primary role for compatibility
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("roleDescription", ROLE_DESCRIPTIONS);
            session.setAttribute("loginTime", System.currentTimeMillis());

            // Log successful login
            LOGGER.log(Level.INFO, "LOGIN: User '{0}' authenticated successfully with roles: {1}",
                    new Object[]{username, roles});

            // Redirect to dashboard
            response.sendRedirect("dashboard");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication error for user: " + username, e);
            request.setAttribute("error", "Authentication error occurred. Please try again.");
            request.getRequestDispatcher("/index.jsp").forward(request, response);
        }
    }
}