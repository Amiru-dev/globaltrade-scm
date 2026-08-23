package com.globaltrade.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF token utility for generating and validating CSRF tokens.
 *
 * <p><strong>Critical Analysis — CSRF Protection:</strong></p>
 * <ul>
 *   <li><strong>CSRF Attack:</strong> Cross-Site Request Forgery tricks users into
 *       executing unwanted actions on web applications where they're authenticated.</li>
 *   <li><strong>Token-Based Protection:</strong> Generates unique tokens per session
 *       and validates them on state-changing requests.</li>
 *   <li><strong>SecureRandom:</strong> Uses cryptographically secure random number
 *       generator for token generation to prevent token prediction.</li>
 *   <li><strong>Session Storage:</strong> Tokens stored in HTTP session for server-side
 *       validation, preventing client-side tampering.</li>
 * </ul>
 *
 * <p><strong>Implementation:</strong> Applied to state-changing operations (POST, PUT,
 * DELETE) to prevent CSRF attacks while allowing safe GET requests.</p>
 */
public class CsrfTokenUtil {

    private static final String CSRF_TOKEN_SESSION_KEY = "csrf_token";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final String CSRF_PARAM_NAME = "csrf_token";
    private static final int TOKEN_LENGTH = 32; // 256 bits
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new CSRF token and stores it in the session.
     *
     * <p><strong>Critical Analysis:</strong> Token is generated once per session
     * and stored in server-side session for validation. Client must include this
     * token in state-changing requests.</p>
     */
    public static String generateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        session.setAttribute(CSRF_TOKEN_SESSION_KEY, token);
        return token;
    }

    /**
     * Retrieves the current CSRF token from the session.
     *
     * <p><strong>Critical Analysis:</strong> Returns existing token or generates
     * new one if not present. Ensures token availability for client requests.</p>
     */
    public static String getToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return generateToken(request);
        }
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        if (token == null) {
            return generateToken(request);
        }
        return token;
    }

    /**
     * Validates CSRF token from request against session token.
     *
     * <p><strong>Critical Analysis:</strong> Checks token from header or parameter
     * against session token. Uses constant-time comparison to prevent timing attacks.</p>
     */
    public static boolean validateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        if (sessionToken == null) {
            return false;
        }

        // Check header first, then parameter
        String requestToken = request.getHeader(CSRF_HEADER_NAME);
        if (requestToken == null) {
            requestToken = request.getParameter(CSRF_PARAM_NAME);
        }

        if (requestToken == null) {
            return false;
        }

        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(sessionToken, requestToken);
    }

    /**
     * Constant-time string comparison to prevent timing side-channel attacks.
     *
     * <p><strong>Security Note:</strong> Standard {@code String.equals()} returns
     * early on the first mismatched character, leaking information about how many
     * characters matched. This method always compares all characters.</p>
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Returns the CSRF header name for client-side implementation.
     */
    public static String getHeaderName() {
        return CSRF_HEADER_NAME;
    }

    /**
     * Returns the CSRF parameter name for form-based submissions.
     */
    public static String getParamName() {
        return CSRF_PARAM_NAME;
    }

    /**
     * Clears the CSRF token from the session (e.g., on logout).
     */
    public static void clearToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(CSRF_TOKEN_SESSION_KEY);
        }
    }
}