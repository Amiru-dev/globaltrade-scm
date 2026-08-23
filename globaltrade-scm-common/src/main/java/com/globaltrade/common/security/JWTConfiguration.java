package com.globaltrade.common.security;

import com.globaltrade.common.util.SystemConstants;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * Centralized JWT configuration and secret key management.
 *
 * <p><strong>Critical Analysis — Centralized Configuration:</strong></p>
 * <ul>
 *   <li><strong>Secret Key Management:</strong> Centralized in JWTConfiguration class
 *       to eliminate duplication across JWTAuthenticationService and JWTAuthenticationFilter.
 *       This prevents configuration drift and enables single point of change.</li>
 *   <li><strong>Environment Variable:</strong> Uses JWT_SECRET_KEY environment variable
 *       with fallback for development. Production should use secure key management service
 *       (AWS KMS, HashiCorp Vault) and implement key rotation strategy.</li>
 *   <li><strong>Key Strength:</strong> 256-bit secret key provides adequate security for
 *       JWT signing. Secret key length should match cryptographic best practices.</li>
 *   <li><strong>Token Expiry:</strong> Uses SystemConstants for configurable token expiry,
 *       balancing security (short lifespan) with usability (reduced re-authentication).</li>
 * </ul>
 *
 * <p><strong>Risk Mitigation:</strong> Environment variable approach is a known risk
 * mitigated by documentation. Production deployment requires proper key management service
 * integration and key rotation strategy.</p>
 */
public class JWTConfiguration {

    // Secret key from environment variable with fallback for development
    private static final String SECRET_KEY =
            System.getenv().getOrDefault("JWT_SECRET_KEY", "732065811df984204b3b9ec003a228ec50aa5230ec25abe0b4991b1d67f7a0e3");

    // Convert string to Key object for jjwt 0.11.5 API
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    /**
     * Returns the signing key for JWT token generation and validation.
     *
     * <p><strong>Critical Analysis:</strong> Key object is generated once at startup
     * and reused for all JWT operations, providing consistent cryptographic operations.</p>
     */
    public static Key getSigningKey() {
        return SIGNING_KEY;
    }

    /**
     * Returns the raw secret key string (for logging/migration purposes only).
     *
     * <p><strong>Security Note:</strong> Use with caution. Never log in production
     * or expose in error messages.</p>
     */
    public static String getSecretKey() {
        return SECRET_KEY;
    }

    /**
     * Returns access token expiry time in milliseconds.
     */
    public static long getAccessTokenExpiryMs() {
        return SystemConstants.JWT_ACCESS_TOKEN_EXPIRY_MS;
    }

    /**
     * Returns refresh token expiry time in days.
     */
    public static long getRefreshTokenExpiryDays() {
        return SystemConstants.JWT_REFRESH_TOKEN_EXPIRY_DAYS;
    }
}