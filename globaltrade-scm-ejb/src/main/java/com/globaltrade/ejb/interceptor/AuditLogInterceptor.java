package com.globaltrade.ejb.interceptor;

import com.globaltrade.common.entity.AuditLog;
import com.globaltrade.ejb.service.AuditService;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class-level interceptor that creates audit trail entries for all
 * business method invocations on annotated EJB service beans.
 *
 * <p><strong>Interceptor Level: CLASS-LEVEL</strong> — Applied to entire
 * service beans using {@code @Interceptors(AuditLogInterceptor.class)}.
 * This ensures comprehensive audit coverage without annotating each method.</p>
 *
 * <p><strong>Critical Analysis — Audit Interceptor Design:</strong></p>
 * <ul>
 *   <li><strong>Separate EJB for persistence:</strong> Audit logging is delegated
 *       to AuditService EJB which uses REQUIRES_NEW transaction. This ensures
 *       audit logs persist even if the business method's transaction rolls back.</li>
 *   <li><strong>Performance consideration:</strong> Each intercepted call incurs
 *       a database write for audit logging. In high-throughput scenarios,
 *       asynchronous audit logging or batch writes could improve performance.</li>
 *   <li><strong>InvocationContext:</strong> Provides access to the target method,
 *       parameters, and class — enabling rich audit metadata without
 *       coupling the interceptor to specific business logic.</li>
 * </ul>
 */
public class AuditLogInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AuditLogInterceptor.class.getName());

    @EJB
    private AuditService auditService;

    @Resource
    private SessionContext sessionContext;

    /**
     * Intercepts all business method invocations to create audit records.
     *
     * <p>The interceptor captures:
     * <ul>
     *   <li>Who: Current authenticated user (from SessionContext)</li>
     *   <li>What: Method name and target class</li>
     *   <li>When: Timestamp of invocation</li>
     *   <li>Result: SUCCESS or FAILURE with execution time</li>
     * </ul></p>
     */
    @AroundInvoke
    public Object audit(InvocationContext ctx) throws Exception {
        long startTime = System.currentTimeMillis();
        String methodName = ctx.getMethod().getName();
        String className = ctx.getTarget().getClass().getSimpleName();
        String userId = "SYSTEM"; // Default for unauthenticated contexts
        String userRole = "UNKNOWN";

        // Extract authenticated user information
        try {
            if (sessionContext.getCallerPrincipal() != null) {
                userId = sessionContext.getCallerPrincipal().getName();
            }
        } catch (Exception e) {
            // SessionContext may not be available in all contexts
            LOGGER.log(Level.FINE, "Could not retrieve caller principal", e);
        }

        // Determine action type from method name
        String action = determineAction(methodName);

        // Extract entity information from parameters
        String entityId = extractEntityId(ctx.getParameters());

        Object result = null;
        String resultStatus = "SUCCESS";

        try {
            // Proceed with the actual business method
            result = ctx.proceed();
            return result;

        } catch (Exception e) {
            resultStatus = "FAILURE";
            throw e; // Re-throw to preserve exception propagation

        } finally {
            // Record audit log — always executes regardless of success/failure
            long executionTime = System.currentTimeMillis() - startTime;

            try {
                AuditLog auditLog = new AuditLog();
                auditLog.setUserId(userId);
                auditLog.setUserRole(userRole);
                auditLog.setAction(action);
                auditLog.setEntityType(className.replace("Service", ""));
                auditLog.setEntityId(entityId);
                auditLog.setMethodName(className + "." + methodName);
                auditLog.setDetails("Parameters: " + summarizeParams(ctx.getParameters()));
                auditLog.setResultStatus(resultStatus);
                auditLog.setExecutionTimeMs(executionTime);

                auditService.persistAuditLog(auditLog);

                LOGGER.log(Level.FINE,
                        "AUDIT: [{0}] {1}.{2}() by {3} — {4} ({5}ms)",
                        new Object[]{action, className, methodName, userId,
                                resultStatus, executionTime});
            } catch (Exception auditError) {
                // Audit logging failure should NOT affect business operation
                LOGGER.log(Level.WARNING, "Failed to write audit log", auditError);
            }
        }
    }

    /**
     * Determines the action type from the method name prefix.
     */
    private String determineAction(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("add")
                || methodName.startsWith("register")) {
            return "CREATE";
        } else if (methodName.startsWith("update") || methodName.startsWith("set")
                || methodName.startsWith("modify")) {
            return "UPDATE";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "DELETE";
        } else if (methodName.startsWith("find") || methodName.startsWith("get")
                || methodName.startsWith("count")) {
            return "READ";
        } else if (methodName.startsWith("transfer") || methodName.startsWith("deduct")
                || methodName.startsWith("replenish")) {
            return "OPERATION";
        } else {
            return "OTHER";
        }
    }

    /**
     * Extracts an entity identifier from method parameters (first Long or String param).
     */
    private String extractEntityId(Object[] params) {
        if (params == null || params.length == 0) return "N/A";
        for (Object param : params) {
            if (param instanceof Long) return param.toString();
            if (param instanceof String) return (String) param;
        }
        return "N/A";
    }

    /**
     * Creates a truncated summary of method parameters for logging.
     */
    private String summarizeParams(Object[] params) {
        if (params == null || params.length == 0) return "none";
        String summary = Arrays.toString(params);
        return summary.length() > 200 ? summary.substring(0, 200) + "..." : summary;
    }
}
