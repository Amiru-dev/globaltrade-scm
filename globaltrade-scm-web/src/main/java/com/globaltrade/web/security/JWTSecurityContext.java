package com.globaltrade.web.security;

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Custom SecurityContext implementation for JWT authentication.
 *
 * <p><strong>Critical Analysis — SecurityContext Design:</strong></p>
 * <ul>
 *   <li><strong>Principal Population:</strong> Contains JWTPrincipal with user claims
 *       extracted from JWT token for authorization decisions.</li>
 *   <li><strong>Role Checking:</strong> isUserInRole() delegates to JWTPrincipal.hasRole()
 *       to enable @RolesAllowed enforcement.</li>
 *   <li><strong>Security Scheme:</strong> Returns "Bearer" to indicate JWT-based authentication.</li>
 *   <li><strong>Secure Flag:</strong> Returns true (assume HTTPS in production).</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Set by JWTAuthenticationFilter for requests with
 * valid JWT tokens, enabling JAX-RS @RolesAllowed annotations to work correctly.</p>
 */
public class JWTSecurityContext implements SecurityContext {

    private final JWTPrincipal principal;

    public JWTSecurityContext(JWTPrincipal principal) {
        this.principal = principal;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return principal != null && principal.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return true; // Assume HTTPS in production
    }

    @Override
    public String getAuthenticationScheme() {
        return "Bearer";
    }
}