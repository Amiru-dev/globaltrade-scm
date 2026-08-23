package com.globaltrade.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Security headers filter for defense-in-depth security.
 *
 * <p><strong>Critical Analysis — Security Headers:</strong></p>
 * <ul>
 *   <li><strong>X-Content-Type-Options: nosniff</strong> — Prevents MIME-type sniffing
 *       attacks by forcing browser to respect declared content type.</li>
 *   <li><strong>X-Frame-Options: DENY</strong> — Prevents clickjacking attacks by
 *       blocking site from being embedded in frames.</li>
 *   <li><strong>X-XSS-Protection: 1; mode=block</strong> — Enables browser XSS filter
 *       in blocking mode for enhanced cross-site scripting protection.</li>
 *   <li><strong>Content-Security-Policy: default-src 'self'</strong> — Restricts
 *       resource loading to same origin, preventing XSS and data injection attacks.</li>
 *   <li><strong>Strict-Transport-Security</strong> — Enforces HTTPS connections,
 *       preventing protocol downgrade attacks.</li>
 *   <li><strong>Referrer-Policy</strong> — Controls referrer information leakage
 *       in cross-origin requests.</li>
 * </ul>
 *
 * <p><strong>Defense-in-Depth:</strong> These headers provide additional security
 * layers beyond authentication and authorization, protecting against common web
 * vulnerabilities even if other security measures fail.</p>
 */
@WebFilter(urlPatterns = "/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Filter initialization - no setup required
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Security headers
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data:; script-src 'self'");

        // Only set HSTS on secure (HTTPS) connections — sending over HTTP is invalid
        if (request.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Additional security headers
        httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Filter cleanup - no resources to release
    }
}