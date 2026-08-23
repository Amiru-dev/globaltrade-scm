package com.globaltrade.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * CORS (Cross-Origin Resource Sharing) filter for the REST API.
 *
 * <p>Validates incoming Origin headers against a configurable allowlist
 * and sets appropriate CORS response headers.</p>
 *
 * <p><strong>Critical Analysis — Security:</strong></p>
 * <ul>
 *   <li><strong>Origin Validation:</strong> Only allowed origins receive CORS
 *       headers, preventing unauthorized cross-origin access.</li>
 *   <li><strong>Credentials Support:</strong> Echoes back the specific matched
 *       origin (not *) so Access-Control-Allow-Credentials works correctly.</li>
 *   <li><strong>Preflight Handling:</strong> OPTIONS requests are handled with
 *       204 No Content to support CORS preflight checks.</li>
 *   <li><strong>Configuration:</strong> Allowed origins are read from the
 *       CORS_ALLOWED_ORIGINS environment variable (comma-separated),
 *       defaulting to http://localhost:8080 for development.</li>
 * </ul>
 */
@Provider
@PreMatching
public class CORSFilter implements ContainerResponseFilter {

    private static final Set<String> ALLOWED_ORIGINS;

    static {
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (envOrigins != null && !envOrigins.isBlank()) {
            Set<String> origins = new HashSet<>();
            for (String origin : envOrigins.split(",")) {
                origins.add(origin.trim());
            }
            ALLOWED_ORIGINS = Collections.unmodifiableSet(origins);
        } else {
            ALLOWED_ORIGINS = Set.of("http://localhost:8080");
        }
    }

    private static final String ALLOWED_HEADERS =
            "Origin, Content-Type, Accept, Authorization, X-Requested-With, X-CSRF-Token";
    private static final String ALLOWED_METHODS =
            "GET, POST, PUT, DELETE, OPTIONS, HEAD";
    private static final String MAX_AGE = "3600"; // Cache preflight for 1 hour

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        String origin = requestContext.getHeaderString("Origin");

        if (origin == null || origin.isEmpty()) {
            // Not a cross-origin request — no CORS headers needed
            return;
        }

        if (!ALLOWED_ORIGINS.contains(origin)) {
            // Origin not in allowlist — do not add CORS headers
            return;
        }

        // Echo back the specific allowed origin (not *)
        responseContext.getHeaders().add("Access-Control-Allow-Origin", origin);
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Headers", ALLOWED_HEADERS);
        responseContext.getHeaders().add("Access-Control-Allow-Methods", ALLOWED_METHODS);
        responseContext.getHeaders().add("Access-Control-Max-Age", MAX_AGE);
        responseContext.getHeaders().add("Vary", "Origin");
    }
}
