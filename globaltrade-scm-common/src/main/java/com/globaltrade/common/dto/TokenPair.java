package com.globaltrade.common.dto;

import java.io.Serializable;

/**
 * DTO for JWT access token and refresh token pair.
 *
 * <p><strong>Critical Analysis — Token Pair Design:</strong></p>
 * <ul>
 *   <li><strong>Access Token:</strong> Short-lived JWT (15 minutes) used for API authentication.
 *       Contains user claims and is stateless for horizontal scaling.</li>
 *   <li><strong>Refresh Token:</strong> Long-lived opaque token (30 days) used to obtain new
 *       access tokens without re-authentication. Stored in database for revocation support.</li>
 *   <li><strong>Token Rotation:</strong> Old refresh tokens are revoked on use, preventing
 *       replay attacks and ensuring fresh tokens.</li>
 * </ul>
 */
public class TokenPair implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accessToken;
    private String refreshToken;

    public TokenPair() {
    }

    public TokenPair(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public String toString() {
        return "TokenPair{" +
                "accessToken='" + (accessToken != null ? accessToken.substring(0, 20) + "..." : "null") + '\'' +
                ", refreshToken='" + (refreshToken != null ? refreshToken.substring(0, 20) + "..." : "null") + '\'' +
                '}';
    }
}