package com.globaltrade.web.filter;

import com.globaltrade.web.security.CsrfTokenUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSRF protection filter for state-changing operations.
 *
 * <p><strong>Critical Analysis — CSRF Protection:</strong></p>
 * <ul>
 *   <li><strong>State-Changing Operations:</strong> Applied to POST, PUT, DELETE,
 *       PATCH methods which modify application state.</li>
 *   <li><strong>Safe Methods:</strong> GET, HEAD, OPTIONS, TRACE are considered safe
 *       and bypass CSRF validation (read-only operations).</li>
 *   <li><strong>Excluded Paths:</strong> Authentication endpoints (/auth/*) are excluded
 *       to allow login/logout without CSRF token.</li>
 *   <li><strong>Token Validation:</strong> Validates CSRF token from header or parameter
 *       against session token using constant-time comparison.</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Works with CsrfTokenUtil for token generation and
 * validation. Tokens are generated per session and validated on state-changing requests.</p>
 */
@WebFilter(urlPatterns = "/*")
public class CSRFProtectionFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CSRFProtectionFilter.class.getName());

    // Safe HTTP methods that don't require CSRF protection
    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList(
            "GET", "HEAD", "OPTIONS", "TRACE"
    ));

    // Paths excluded from CSRF protection (authentication endpoints)
    private static final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
            "/auth/login",
            "/auth/refresh",
            "/health"
    ));

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.log(Level.INFO, "CSRF Protection Filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String path = httpRequest.getRequestURI();

        // Skip CSRF validation for safe methods
        if (SAFE_METHODS.contains(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Skip CSRF validation for excluded paths
        for (String excludedPath : EXCLUDED_PATHS) {
            if (path.startsWith(excludedPath)) {
                chain.doFilter(request, response);
                return;
            }
        }

        // Validate CSRF token for state-changing operations
        if (CsrfTokenUtil.validateToken(httpRequest)) {
            // CSRF token valid, proceed with request
            chain.doFilter(request, response);
        } else {
            // CSRF token invalid or missing
            LOGGER.log(Level.WARNING, "CSRF validation failed for {0} {1}",
                    new Object[]{method, path});

            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"CSRF token validation failed\"}");
        }
    }

    @Override
    public void destroy() {
        LOGGER.log(Level.INFO, "CSRF Protection Filter destroyed");
    }
}