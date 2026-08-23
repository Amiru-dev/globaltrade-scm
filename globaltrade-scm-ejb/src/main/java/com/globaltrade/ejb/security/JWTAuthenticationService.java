package com.globaltrade.ejb.security;

import com.globaltrade.common.dto.TokenPair;
import com.globaltrade.common.entity.RefreshToken;
import com.globaltrade.common.entity.User;
import com.globaltrade.common.util.SystemConstants;
import com.globaltrade.web.security.JWTConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * JWT authentication service for token generation, validation, and refresh.
 *
 * <p><strong>Critical Analysis — JWT Authentication Architecture:</strong></p>
 * <ul>
 *   <li><strong>Centralized Configuration:</strong> Uses JWTConfiguration bean for
 *       secret key management, eliminating duplication and enabling single point of change.</li>
 *   <li><strong>Access Tokens:</strong> Stateless JWTs containing user claims (userId, username,
 *       roles, vendorCode) for API authentication. Short 15-minute expiry limits token theft impact.</li>
 *   <li><strong>Refresh Tokens:</strong> Opaque random values stored as hashes in database,
 *       enabling revocation and "log out everywhere" functionality.</li>
 *   <li><strong>Token Rotation:</strong> Old refresh tokens are revoked on use, preventing
 *       replay attacks and ensuring fresh tokens.</li>
 *   <li><strong>Hybrid Authentication:</strong> JWT for external APIs (stateless, scalable),
 *       JAAS for internal workflows (session-based, container-managed).</li>
 * </ul>
 */
@Stateless
public class JWTAuthenticationService {

    @Inject
    private JWTConfiguration jwtConfig;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates access and refresh tokens for authenticated user.
     *
     * <p><strong>Critical Analysis:</strong> Access token contains user claims for
     * authorization decisions. Refresh token is opaque random value stored in database
     * for revocation support.</p>
     */
    public TokenPair generateTokens(User user, String deviceInfo, String sessionId) {
        // Generate access token using modern jjwt 0.11.5 API
        String accessToken = Jwts.builder()
            .setSubject(user.getUsername())
            .claim("userId", user.getId())
            .claim("roles", user.getRoles())
            .claim("vendorCode", user.getVendorCode())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + SystemConstants.JWT_ACCESS_TOKEN_EXPIRY_MS))
            .signWith(jwtConfig.getSigningKey())
            .compact();

        // Generate refresh token (opaque random value)
        String refreshToken = generateSecureRandomToken();
        String tokenHash = hashToken(refreshToken);

        // Store refresh token in database
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setUser(user);
        tokenEntity.setIssuedAt(Instant.now());
        tokenEntity.setExpiresAt(Instant.now().plus(SystemConstants.JWT_REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS));
        tokenEntity.setDeviceInfo(deviceInfo);
        tokenEntity.setSessionId(sessionId);
        em.persist(tokenEntity);

        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Refreshes access token using valid refresh token.
     *
     * <p><strong>Critical Analysis:</strong> Validates refresh token, revokes old token,
     * and generates new token pair. This rotation prevents replay attacks.</p>
     */
    public TokenPair refreshAccessToken(String refreshToken, String deviceInfo) {
        RefreshToken tokenEntity = validateRefreshToken(refreshToken);

        if (tokenEntity.isRevoked()) {
            throw new SecurityException("Refresh token has been revoked");
        }

        // Revoke old refresh token
        tokenEntity.setRevoked(true);
        em.merge(tokenEntity);

        // Generate new tokens
        return generateTokens(tokenEntity.getUser(), deviceInfo, tokenEntity.getSessionId());
    }

    /**
     * Revokes all refresh tokens for a user (log out everywhere).
     *
     * <p><strong>Critical Analysis:</strong> Sets revoked flag on all user's refresh tokens,
     * forcing re-authentication on all devices. Important for security incidents.</p>
     */
    public void revokeAllRefreshTokens(Long userId) {
        em.createQuery("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId")
            .setParameter("userId", userId)
            .executeUpdate();
    }

    /**
     * Validates refresh token and returns entity if valid.
     *
     * <p><strong>Critical Analysis:</strong> Checks token hash, expiry, and revocation status.
     * Throws SecurityException for invalid tokens.</p>
     */
    private RefreshToken validateRefreshToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        RefreshToken token = em.createQuery(
            "SELECT t FROM RefreshToken t WHERE t.tokenHash = :hash AND t.revoked = false AND t.expiresAt > :now", RefreshToken.class)
            .setParameter("hash", tokenHash)
            .setParameter("now", Instant.now())
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new SecurityException("Invalid refresh token"));

        return token;
    }

    /**
     * Generates cryptographically secure random token.
     *
     * <p><strong>Critical Analysis:</strong> Uses SecureRandom for token generation,
     * ensuring unpredictability and preventing token guessing attacks.</p>
     */
    private String generateSecureRandomToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return bytesToHex(tokenBytes);
    }

    /**
     * Hashes token for secure storage in database.
     *
     * <p><strong>Critical Analysis:</strong> Uses SHA-256 for token hashing.
     * In production, consider using BCrypt for adaptive work factor.</p>
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * Converts byte array to hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Parses and validates JWT access token.
     *
     * <p><strong>Critical Analysis:</strong> Extracts user claims from JWT for
     * authorization decisions. Throws SecurityException for invalid tokens.</p>
     */
    public Claims parseAccessToken(String accessToken) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(jwtConfig.getSigningKey())
                .build()
                .parseClaimsJws(accessToken)
                .getBody();
        } catch (Exception e) {
            throw new SecurityException("Invalid or expired access token", e);
        }
    }
}