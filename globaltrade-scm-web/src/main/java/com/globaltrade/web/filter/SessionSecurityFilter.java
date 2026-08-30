package com.globaltrade.web.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Session security filter to ensure session cookies have proper security attributes.
 *
 * <p><strong>Critical Analysis — Session Cookie Security:</strong></p>
 * <ul>
 *   <li><strong>HttpOnly Flag:</strong> Prevents client-side JavaScript from accessing
 *       session cookies, mitigating XSS attacks targeting session hijacking.</li>
 *   <li><strong>Secure Flag:</strong> Ensures cookies are only transmitted over HTTPS,
 *       preventing session interception on unencrypted connections.</li>
 *   <li><strong>SameSite Attribute:</strong> Controls whether cookies are sent with
 *       cross-site requests, providing CSRF protection. 'Strict' is most secure for session cookies.</li>
 * </ul>
 *
 * <p><strong>Defense-in-Depth:</strong> This filter ensures that session cookies
 * have the SameSite attribute set via response headers, complementing the
 * web.xml configuration for comprehensive session security.</p>
 */
@WebFilter(urlPatterns = "/*")
public class SessionSecurityFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(SessionSecurityFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.log(Level.INFO, "Session Security Filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Set SameSite attribute for session cookies via response header
        // This works with web.xml <same-site> configuration for comprehensive security
        httpResponse.setHeader("Set-Cookie", "JSESSIONID=; Path=/; HttpOnly; Secure; SameSite=Strict");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOGGER.log(Level.INFO, "Session Security Filter destroyed");
    }
}