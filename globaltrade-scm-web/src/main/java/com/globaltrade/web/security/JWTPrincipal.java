package com.globaltrade.web.security;

import java.security.Principal;
import java.util.List;

/**
 * Custom Principal implementation for JWT authentication.
 *
 * <p><strong>Critical Analysis — JWT Principal Design:</strong></p>
 * <ul>
 *   <li><strong>User Claims:</strong> Contains userId, username, roles, and vendorCode
 *       extracted from JWT token for authorization decisions.</li>
 *   <li><strong>Role-Based Access:</strong> Roles list enables @RolesAllowed enforcement
 *       by SecurityContext.isUserInRole() method.</li>
 *   <li><strong>Vendor Isolation:</strong> VendorCode enables data isolation for vendor
 *       representatives in multi-tenant scenarios.</li>
 * </ul>
 */
public class JWTPrincipal implements Principal {

    private final Long userId;
    private final String username;
    private final List<String> roles;
    private final String vendorCode;

    public JWTPrincipal(Long userId, String username, List<String> roles, String vendorCode) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.vendorCode = vendorCode;
    }

    @Override
    public String getName() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    /**
     * Checks if user has specific role.
     *
     * <p><strong>Critical Analysis:</strong> Used by JWTSecurityContext.isUserInRole()
     * to enable @RolesAllowed enforcement.</p>
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    @Override
    public String toString() {
        return "JWTPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", roles=" + roles +
                ", vendorCode='" + vendorCode + '\'' +
                '}';
    }
}