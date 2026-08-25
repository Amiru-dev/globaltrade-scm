package com.globaltrade.web.security;

import com.globaltrade.common.security.JWTConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JWT token validation and claim extraction.
 *
 * <p><strong>Purpose:</strong> This utility provides JWT validation functionality
 * for integration with Jakarta Security. It validates JWT tokens and converts
 * the claims into Jakarta Security CredentialValidationResult objects.</p>
 *
 * <p><strong>Integration Point:</strong> Used by GlobalTradeAuthenticationMechanism
 * to validate Bearer tokens and establish container security context for REST API
 * calls that use JWT authentication.</p>
 *
 * <p><strong>Critical Analysis — JWT Integration:</strong></p>
 * <ul>
 *   <li><strong>Token Validation:</strong> Validates signature, expiry, and structure
 *       using the same JWTConfiguration as the existing JWTAuthenticationFilter.</li>
 *   <li><strong>Claim Extraction:</strong> Extracts username (subject), userId, and roles
 *       from JWT claims for conversion to Jakarta Security groups.</li>
 *   <li><strong>Role Mapping:</strong> Converts JWT role claims to Jakarta Security groups
 *       to enable @RolesAllowed authorization for EJB calls.</li>
 *   <li><strong>Error Handling:</strong> Returns INVALID_RESULT for any validation failure,
 *       consistent with Jakarta Security conventions.</li>
 * </ul>
 */
public class JWTValidationUtil {

    /**
     * Validates a JWT token and extracts user claims for Jakarta Security integration.
     *
     * <p><strong>Process:</strong></p>
     * <ol>
     *   <li>Parses and validates JWT signature and expiry</li>
     *   <li>Extracts username (subject), userId, and roles from claims</li>
     *   <li>Converts to CredentialValidationResult with caller principal and groups</li>
     * </ol>
     *
     * <p><strong>Security Note:</strong> Uses the same signing key as JWTAuthenticationFilter
     * to ensure consistency across the application. Token validation failures return
     * INVALID_RESULT without leaking specific error details.</p>
     *
     * @param token the JWT bearer token (without "Bearer " prefix)
     * @return CredentialValidationResult with caller principal and groups, or INVALID_RESULT if validation fails
     */
    public static CredentialValidationResult validateToken(String token) {
        try {
            // Parse and validate JWT token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(JWTConfiguration.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extract user information from claims
            String username = claims.getSubject();
            if (username == null || username.isEmpty()) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            // Extract roles from claims
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            if (roles == null || roles.isEmpty()) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            // Convert roles to Set<String> for Jakarta Security
            var roleSet = roles.stream()
                    .filter(role -> role != null && !role.isEmpty())
                    .collect(Collectors.toSet());

            if (roleSet.isEmpty()) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            // Return successful validation result with caller principal and groups
            return new CredentialValidationResult(username, roleSet);

        } catch (JwtException e) {
            // Invalid token (signature, expiry, format, etc.)
            // Return INVALID_RESULT without leaking specific error details
            return CredentialValidationResult.INVALID_RESULT;
        } catch (Exception e) {
            // Unexpected error during validation
            // Return INVALID_RESULT for security
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    /**
     * Extracts the bearer token from an Authorization header.
     *
     * @param authHeader the Authorization header value
     * @return the token without "Bearer " prefix, or null if header is invalid
     */
    public static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7); // Remove "Bearer " prefix
    }
}