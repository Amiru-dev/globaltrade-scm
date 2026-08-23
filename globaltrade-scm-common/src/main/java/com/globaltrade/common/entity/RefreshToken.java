package com.globaltrade.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Refresh token entity for JWT token rotation and session management.
 *
 * <p><strong>Critical Analysis — Refresh Token Security:</strong></p>
 * <ul>
 *   <li><strong>Opaque Tokens:</strong> Refresh tokens are opaque random values (not JWTs)
 *       stored as hashes in the database. This prevents token theft and enables revocation.</li>
 *   <li><strong>Token Rotation:</strong> Old refresh tokens are revoked on use, forcing
 *       rotation and preventing replay attacks.</li>
 *   <li><strong>Device Tracking:</strong> Device info and session ID enable "log out everywhere"
 *       functionality and security monitoring.</li>
 *   <li><strong>Expiry Management:</strong> 30-day expiry balances security (shorter than
 *       permanent sessions) with usability (reduced re-authentication).</li>
 * </ul>
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(length = 255)
    private String deviceInfo;

    @Column(length = 50)
    private String sessionId; // For "log out everywhere" functionality

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Default constructor required by JPA
    public RefreshToken() {
    }

    // Constructor for token creation
    public RefreshToken(String tokenHash, User user, Instant issuedAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Checks if the refresh token is expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if the refresh token is valid (not expired and not revoked).
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", tokenHash='" + (tokenHash != null ? tokenHash.substring(0, 8) + "..." : "null") + '\'' +
                ", userId=" + (user != null ? user.getId() : null) +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}