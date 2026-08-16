package com.globaltrade.web.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * CORS (Cross-Origin Resource Sharing) filter for the REST API.
 *
 * <p>Allows browser-based clients from different origins to access the API.
 * In production, the allowed origins should be restricted to known frontend
 * domains rather than using a wildcard.</p>
 *
 * <p><strong>Critical Analysis — Security Trade-off:</strong>
 * Using {@code *} for allowed origins is convenient for development but
 * creates a security risk in production. A production deployment should
 * restrict this to specific frontend domain(s) and configure the Payara
 * server-level CORS filter instead.</p>
 */
@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {

        responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}
