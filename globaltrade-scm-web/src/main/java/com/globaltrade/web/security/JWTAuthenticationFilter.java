package com.globaltrade.web.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter that parses JWT tokens and populates SecurityContext.
 *
 * <p><strong>CRITICAL COMPONENT:</strong> This filter is essential for @RolesAllowed
 * annotations to work. Without it, the SecurityContext would not contain the JWT
 * principal, and role-based authorization would fail.</p>
 *
 * <p><strong>Critical Analysis — SecurityContext Population:</strong></p>
 * <ul>
 *   <li><strong>Filter Priority:</strong> AUTHENTICATION priority ensures this filter
 *       runs before authorization checks. This is critical for @RolesAllowed to work.</li>
 *   <li><strong>Token Parsing:</strong> Extracts Bearer token from Authorization header,
 *       validates signature and expiry, and extracts user claims.</li>
 *   <li><strong>SecurityContext Population:</strong> Creates custom JWTPrincipal with
 *       user info and sets JWTSecurityContext, enabling @RolesAllowed enforcement.</li>
 *   <li><strong>Error Handling:</strong> Returns 401 Unauthorized for invalid tokens,
 *       preventing unauthorized access.</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Must be registered as a provider in JAX-RS application
 * or via @Provider annotation. Filter runs on all requests with Authorization header.</p>
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTAuthenticationFilter implements ContainerRequestFilter {

    @Inject
    private JWTConfiguration jwtConfig;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Get Authorization header
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No JWT token - skip authentication (some endpoints are public)
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        try {
            // Parse and validate JWT token using modern jjwt 0.11.5 API
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtConfig.getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

            // Extract user information
            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            List<String> roles = claims.get("roles", List.class);
            String vendorCode = claims.get("vendorCode", String.class);

            // Create custom Principal with user info
            JWTPrincipal principal = new JWTPrincipal(userId, username, roles, vendorCode);

            // Populate SecurityContext
            requestContext.setSecurityContext(new JWTSecurityContext(principal));

        } catch (JwtException e) {
            // Invalid token - return 401 Unauthorized
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid or expired JWT token")
                    .build()
            );
        }
    }
}