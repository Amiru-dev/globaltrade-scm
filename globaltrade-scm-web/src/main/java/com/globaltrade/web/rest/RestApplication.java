package com.globaltrade.web.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS Application configuration for the GlobalTrade SCM REST API.
 *
 * <p>Configures the base URI path for all REST endpoints as {@code /api}.
 * All resource classes annotated with {@code @Path} are automatically
 * discovered by the JAX-RS runtime.</p>
 *
 * <p><strong>Critical Analysis:</strong> Using {@code @ApplicationPath}
 * eliminates the need for servlet mapping in {@code web.xml}, following
 * the convention-over-configuration principle. However, the web.xml
 * security constraints still reference the {@code /api/*} path pattern.</p>
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    // No override needed — JAX-RS auto-discovers @Path annotated resources
}
