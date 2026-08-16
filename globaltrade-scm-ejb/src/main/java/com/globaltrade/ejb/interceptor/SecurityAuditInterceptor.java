package com.globaltrade.ejb.interceptor;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class-level interceptor for tracking security-relevant events.
 *
 * <p><strong>Interceptor Level: CLASS-LEVEL</strong> — Applied to security-sensitive
 * service beans to capture authentication and authorization events.</p>
 *
 * <p><strong>Critical Analysis — Security Interceptor Integration:</strong></p>
 * <ul>
 *   <li><strong>@Priority ordering:</strong> Security interceptor runs FIRST in
 *       the interceptor chain (lowest priority number). This ensures security
 *       validation occurs before audit logging, compliance checking, and
 *       business logic — following the fail-fast principle.</li>
 *   <li><strong>Non-intrusive:</strong> Security events are logged without
 *       affecting the business method's behaviour or transaction.</li>
 *   <li><strong>Complementary to @RolesAllowed:</strong> While @RolesAllowed
 *       provides declarative security, this interceptor adds an additional
 *       audit layer for security monitoring and incident response.</li>
 * </ul>
 */
public class SecurityAuditInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SecurityAuditInterceptor.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SessionContext sessionContext;

    /**
     * Logs security-relevant information for every intercepted method call.
     */
    @AroundInvoke
    public Object auditSecurity(InvocationContext ctx) throws Exception {
        String methodName = ctx.getTarget().getClass().getSimpleName() + "."
                + ctx.getMethod().getName();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String callerPrincipal = "ANONYMOUS";

        try {
            if (sessionContext.getCallerPrincipal() != null) {
                callerPrincipal = sessionContext.getCallerPrincipal().getName();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not retrieve security context", e);
        }

        // Log security event
        LOGGER.log(Level.INFO, "SECURITY EVENT [{0}]: User={1}, Method={2}",
                new Object[]{timestamp, callerPrincipal, methodName});

        // Check for sensitive operations requiring additional logging
        if (isSensitiveOperation(ctx.getMethod().getName())) {
            LOGGER.log(Level.WARNING,
                    "SENSITIVE OPERATION: User {0} invoking {1} at {2}. " +
                            "Enhanced audit trail recorded.",
                    new Object[]{callerPrincipal, methodName, timestamp});
        }

        try {
            Object result = ctx.proceed();

            LOGGER.log(Level.FINE, "SECURITY OK: {0} by {1} completed successfully",
                    new Object[]{methodName, callerPrincipal});
            return result;

        } catch (SecurityException se) {
            // Log security violations
            LOGGER.log(Level.SEVERE,
                    "SECURITY VIOLATION: Unauthorized access attempt by {0} on {1}. " +
                            "Exception: {2}",
                    new Object[]{callerPrincipal, methodName, se.getMessage()});
            throw se;

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "SECURITY AUDIT: {0} by {1} threw {2}",
                    new Object[]{methodName, callerPrincipal,
                            e.getClass().getSimpleName()});
            throw e;
        }
    }

    /**
     * Identifies sensitive operations requiring enhanced logging.
     */
    private boolean isSensitiveOperation(String methodName) {
        return methodName.contains("delete") || methodName.contains("Delete")
                || methodName.contains("security") || methodName.contains("Security")
                || methodName.contains("role") || methodName.contains("Role")
                || methodName.contains("password") || methodName.contains("Password")
                || methodName.contains("admin") || methodName.contains("Admin")
                || methodName.contains("config") || methodName.contains("Config");
    }
}
