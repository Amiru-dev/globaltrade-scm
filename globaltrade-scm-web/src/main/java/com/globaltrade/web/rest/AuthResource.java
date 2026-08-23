package com.globaltrade.web.rest;

import com.globaltrade.common.dto.TokenPair;
import com.globaltrade.common.entity.User;
import com.globaltrade.common.util.SystemConstants;
import com.globaltrade.ejb.security.JWTAuthenticationService;
import com.globaltrade.ejb.security.SecurityService;
import com.globaltrade.web.security.CsrfTokenUtil;
import com.globaltrade.common.security.JWTPrincipal;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

/**
 * REST authentication endpoints for JWT-based authentication.
 *
 * <p><strong>Critical Analysis — Authentication Resource Design:</strong></p>
 * <ul>
 *   <li><strong>Login Endpoint:</strong> Authenticates user credentials and returns
 *       JWT access token and refresh token for API access.</li>
 *   <li><strong>Refresh Endpoint:</strong> Uses refresh token to obtain new access
 *       token without re-authentication. Implements token rotation for security.</li>
 *   <li><strong>Logout Endpoint:</strong> Revokes all refresh tokens for user (log out
 *       everywhere). Uses @Context SecurityContext to extract authenticated user.</li>
 *   <li><strong>Security Context Injection:</strong> @Context SecurityContext injection
 *       works because JWTAuthenticationFilter populates the SecurityContext with JWTPrincipal.</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Works with JWTAuthenticationFilter for SecurityContext
 * population and JWTAuthenticationService for token management.</p>
 */
@Path("/auth")
public class AuthResource {

    @EJB
    private JWTAuthenticationService jwtAuthService;

    @EJB
    private SecurityService authService;

    @Context
    private SecurityContext securityContext;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest servletRequest;

    /**
     * Login endpoint - authenticates user and returns JWT tokens.
     *
     * <p><strong>Critical Analysis:</strong> Uses existing SecurityService for
     * authentication, then generates JWT tokens for API access. Captures device
     * info for security monitoring.</p>
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String username,
                          @FormParam("password") String password,
                          @HeaderParam("User-Agent") String userAgent) {

        // Validate required fields
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        try {
            // Authenticate user using existing SecurityService
            User user = authService.authenticate(username.trim(), password);

            // Generate tokens
            TokenPair tokens = jwtAuthService.generateTokens(user, userAgent, null);

            // Generate CSRF token for session
            String csrfToken = CsrfTokenUtil.generateToken(servletRequest);

            return Response.ok(tokens)
                    .header("X-Auth-Token-Expiry", String.valueOf(SystemConstants.JWT_ACCESS_TOKEN_EXPIRY_MINUTES * 60))
                    .header("X-CSRF-Token", csrfToken)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid username or password\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Refresh endpoint - uses refresh token to obtain new access token.
     *
     * <p><strong>Critical Analysis:</strong> Validates refresh token, revokes old token,
     * and generates new token pair. Implements token rotation to prevent replay attacks.</p>
     */
    @POST
    @Path("/refresh")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@FormParam("refreshToken") String refreshToken,
                            @HeaderParam("User-Agent") String userAgent) {

        // Validate required field
        if (refreshToken == null || refreshToken.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Refresh token is required\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        try {
            TokenPair tokens = jwtAuthService.refreshAccessToken(refreshToken.trim(), userAgent);

            return Response.ok(tokens)
                    .header("X-Auth-Token-Expiry", String.valueOf(SystemConstants.JWT_ACCESS_TOKEN_EXPIRY_MINUTES * 60))
                    .build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid or expired refresh token\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Logout endpoint - revokes all refresh tokens for authenticated user.
     *
     * <p><strong>Critical Analysis:</strong> Uses @Context SecurityContext to extract
     * authenticated user (JWTPrincipal). This works because JWTAuthenticationFilter
     * populates SecurityContext with JWTPrincipal containing userId.</p>
     *
     * <p><strong>Security:</strong> Requires valid JWT token. Revokes all refresh tokens,
     * forcing re-authentication on all devices ("log out everywhere").</p>
     */
    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("Authorization") String authHeader) {

        try {
            // Extract user from SecurityContext (populated by JWTAuthenticationFilter)
            JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

            if (principal == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"No authenticated user found\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Revoke all refresh tokens for user
            jwtAuthService.revokeAllRefreshTokens(principal.getUserId());

            // Clear CSRF token
            CsrfTokenUtil.clearToken(servletRequest);

            return Response.ok("{\"message\":\"Logged out successfully\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Logout failed\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Token validation endpoint - checks if access token is valid.
     *
     * <p><strong>Critical Analysis:</strong> Useful for client-side token validation
     * without making business API calls. Returns user information if token is valid.</p>
     */
    @GET
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateToken() {

        try {
            JWTPrincipal principal = (JWTPrincipal) securityContext.getUserPrincipal();

            if (principal == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"No authenticated user found\"}")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Return user information from token
            String userInfo = String.format(
                    "{\"userId\":%d,\"username\":\"%s\",\"roles\":%s,\"vendorCode\":\"%s\"}",
                    principal.getUserId(),
                    principal.getName(),
                    principal.getRoles().toString(),
                    principal.getVendorCode() != null ? principal.getVendorCode() : ""
            );

            return Response.ok(userInfo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Token validation failed\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * CSRF token endpoint - returns current CSRF token for client.
     *
     * <p><strong>Critical Analysis:</strong> Provides CSRF token for client-side
     * applications to include in state-changing requests. Token is generated per
     * session and validated by CSRFProtectionFilter.</p>
     */
    @GET
    @Path("/csrf-token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCsrfToken() {

        try {
            String csrfToken = CsrfTokenUtil.getToken(servletRequest);

            String response = String.format(
                    "{\"csrfToken\":\"%s\",\"headerName\":\"%s\",\"paramName\":\"%s\"}",
                    csrfToken,
                    CsrfTokenUtil.getHeaderName(),
                    CsrfTokenUtil.getParamName()
            );

            return Response.ok(response)
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"CSRF token generation failed\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}