package com.globaltrade.ejb.security;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates programmatic security using SessionContext for fine-grained
 * authorization beyond what declarative @RolesAllowed can achieve.
 *
 * <p><strong>Critical Analysis — Declarative vs Programmatic Security:</strong></p>
 * <ul>
 *   <li><strong>Declarative (@RolesAllowed):</strong> Simple, clean, covers
 *       static role-based access. But cannot handle dynamic context such as
 *       "a vendor can only see their own data".</li>
 *   <li><strong>Programmatic (SessionContext):</strong> Provides runtime
 *       authorization decisions based on business context. Essential for:
 *       <ul>
 *         <li>Data-level security (vendor sees only own orders)</li>
 *         <li>Emergency override scenarios</li>
 *         <li>Multi-factor authorization checks</li>
 *         <li>Time-based access restrictions</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Stateless
public class SecurityService {

    private static final Logger LOGGER = Logger.getLogger(SecurityService.class.getName());

    @Resource
    private SessionContext ctx;

    /**
     * Checks if the current caller has a specific role.
     * Wraps SessionContext.isCallerInRole() for centralised security checking.
     */
    @PermitAll
    public boolean hasRole(String roleName) {
        return ctx.isCallerInRole(roleName);
    }

    /**
     * Returns the authenticated caller's principal name.
     */
    @PermitAll
    public String getCurrentUser() {
        try {
            return ctx.getCallerPrincipal().getName();
        } catch (Exception e) {
            return "ANONYMOUS";
        }
    }

    /**
     * Programmatic vendor data access control.
     * Vendor representatives can only access data for their own vendor.
     *
     * <p><strong>Critical Analysis:</strong> This cannot be achieved with
     *
     * @RolesAllowed alone because the access rule depends on the DATA CONTENT
     * (vendor ID), not just the user's role. Programmatic security bridges
     * this gap by combining role checks with business logic.</p>
     */
    @RolesAllowed({"VENDOR_REPRESENTATIVE", "LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public boolean canAccessVendorData(String requestedVendorCode, String callerVendorCode) {
        String caller = getCurrentUser();

        // SYSTEM_ADMIN and LOGISTICS_COORDINATOR have unrestricted access
        if (ctx.isCallerInRole("SYSTEM_ADMIN") || ctx.isCallerInRole("LOGISTICS_COORDINATOR")) {
            LOGGER.log(Level.FINE,
                    "Admin/Coordinator {0} granted access to vendor {1}",
                    new Object[]{caller, requestedVendorCode});
            return true;
        }

        // VENDOR_REPRESENTATIVE can only access their own data
        if (ctx.isCallerInRole("VENDOR_REPRESENTATIVE")) {
            boolean allowed = requestedVendorCode.equals(callerVendorCode);
            if (!allowed) {
                LOGGER.log(Level.WARNING,
                        "SECURITY: Vendor rep {0} attempted to access vendor {1} " +
                                "(own vendor: {2}) — DENIED",
                        new Object[]{caller, requestedVendorCode, callerVendorCode});
            }
            return allowed;
        }

        LOGGER.log(Level.WARNING,
                "SECURITY: Unknown role for user {0} — access DENIED", caller);
        return false;
    }

    /**
     * Emergency access override for SYSTEM_ADMIN during logistics emergencies.
     *
     * <p><strong>Critical Analysis:</strong> Emergency scenarios (natural disasters,
     * port closures) may require bypassing normal authorization rules.
     * This method provides controlled emergency access with enhanced audit logging.</p>
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    public boolean authorizeEmergencyAccess(String resourceType, String reason) {
        String caller = getCurrentUser();

        if (!ctx.isCallerInRole("SYSTEM_ADMIN")) {
            LOGGER.log(Level.SEVERE,
                    "SECURITY VIOLATION: Non-admin {0} attempted emergency access", caller);
            return false;
        }

        LOGGER.log(Level.WARNING,
                "EMERGENCY ACCESS GRANTED: Admin {0} accessing {1}. Reason: {2}",
                new Object[]{caller, resourceType, reason});

        return true;
    }

    /**
     * Validates that a customs operation is performed by an authorised agent.
     */
    @RolesAllowed({"CUSTOMS_AGENT", "SYSTEM_ADMIN"})
    public boolean authorizeCustomsOperation(String operationType) {
        String caller = getCurrentUser();

        boolean authorized = ctx.isCallerInRole("CUSTOMS_AGENT")
                || ctx.isCallerInRole("SYSTEM_ADMIN");

        LOGGER.log(Level.INFO,
                "Customs operation {0} by {1}: {2}",
                new Object[]{operationType, caller, authorized ? "AUTHORIZED" : "DENIED"});

        return authorized;
    }

    /**
     * Checks multiple roles for compound authorization.
     */
    @PermitAll
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (ctx.isCallerInRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Supply chain-specific: Warehouse access control for inventory operations.
     * Warehouse managers can only access their assigned warehouses.
     */
    @RolesAllowed({"WAREHOUSE_MANAGER", "LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public boolean canAccessWarehouse(String requestedWarehouseId, String assignedWarehouseId) {
        String caller = getCurrentUser();

        // SYSTEM_ADMIN and LOGISTICS_COORDINATOR have unrestricted warehouse access
        if (ctx.isCallerInRole("SYSTEM_ADMIN") || ctx.isCallerInRole("LOGISTICS_COORDINATOR")) {
            LOGGER.log(Level.INFO, "WAREHOUSE ACCESS: Admin/Coordinator {0} granted access to warehouse {1}",
                    new Object[]{caller, requestedWarehouseId});
            return true;
        }

        // WAREHOUSE_MANAGER can only access their assigned warehouse
        if (ctx.isCallerInRole("WAREHOUSE_MANAGER")) {
            boolean allowed = requestedWarehouseId.equals(assignedWarehouseId);
            if (!allowed) {
                LOGGER.log(Level.WARNING,
                        "WAREHOUSE ACCESS DENIED: Manager {0} attempted to access warehouse {1} (assigned: {2})",
                        new Object[]{caller, requestedWarehouseId, assignedWarehouseId});
            } else {
                LOGGER.log(Level.INFO, "WAREHOUSE ACCESS GRANTED: Manager {0} accessing assigned warehouse {1}",
                        new Object[]{caller, requestedWarehouseId});
            }
            return allowed;
        }

        LOGGER.log(Level.WARNING, "WAREHOUSE ACCESS DENIED: Unknown role for user {0}", caller);
        return false;
    }

    /**
     * Supply chain-specific: Entity-level authorization for supply chain data.
     * Used to control access to shipments, inventory, vendors, and customs data.
     */
    @PermitAll
    public boolean canAccessEntity(String entityType, String userRole) {
        String caller = getCurrentUser();
        
        // Define entity access matrix for supply chain roles
        switch (entityType) {
            case "SHIPMENT":
                return canAccessShipmentData();
            case "INVENTORY":
                return canAccessInventoryData();
            case "VENDOR":
                return canAccessVendorData();
            case "CUSTOMS":
                return canAccessCustomsData();
            default:
                LOGGER.log(Level.WARNING, "ENTITY ACCESS: Unknown entity type {0} for user {1}",
                        new Object[]{entityType, caller});
                return false;
        }
    }

    /**
     * Supply chain-specific: Shipment data access control.
     */
    private boolean canAccessShipmentData() {
        return ctx.isCallerInRole("LOGISTICS_COORDINATOR") ||
               ctx.isCallerInRole("WAREHOUSE_MANAGER") ||
               ctx.isCallerInRole("CUSTOMS_AGENT") ||
               ctx.isCallerInRole("SYSTEM_ADMIN");
    }

    /**
     * Supply chain-specific: Inventory data access control.
     */
    private boolean canAccessInventoryData() {
        return ctx.isCallerInRole("WAREHOUSE_MANAGER") ||
               ctx.isCallerInRole("LOGISTICS_COORDINATOR") ||
               ctx.isCallerInRole("SYSTEM_ADMIN");
    }

    /**
     * Supply chain-specific: Vendor data access control.
     */
    private boolean canAccessVendorData() {
        return ctx.isCallerInRole("LOGISTICS_COORDINATOR") ||
               ctx.isCallerInRole("VENDOR_REPRESENTATIVE") ||
               ctx.isCallerInRole("SYSTEM_ADMIN");
    }

    /**
     * Supply chain-specific: Customs data access control.
     */
    private boolean canAccessCustomsData() {
        return ctx.isCallerInRole("CUSTOMS_AGENT") ||
               ctx.isCallerInRole("LOGISTICS_COORDINATOR") ||
               ctx.isCallerInRole("SYSTEM_ADMIN");
    }

    /**
     * Supply chain-specific: Multi-factor authorization for high-value operations.
     * Requires both role-based and contextual authorization.
     */
    @RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public boolean authorizeHighValueOperation(String operationType, double declaredValue) {
        String caller = getCurrentUser();
        
        // Basic role check
        if (!ctx.isCallerInRole("LOGISTICS_COORDINATOR") && !ctx.isCallerInRole("SYSTEM_ADMIN")) {
            LOGGER.log(Level.WARNING, "HIGH-VALUE OPERATION DENIED: Insufficient role for user {0}", caller);
            return false;
        }

        // High-value threshold check (operations over $100,000 require admin approval)
        if (declaredValue > 100000 && !ctx.isCallerInRole("SYSTEM_ADMIN")) {
            LOGGER.log(Level.WARNING, "HIGH-VALUE OPERATION DENIED: Value ${0} requires admin approval for user {1}",
                    new Object[]{declaredValue, caller});
            return false;
        }

        LOGGER.log(Level.INFO, "HIGH-VALUE OPERATION AUTHORIZED: {0} by {1} for value ${2}",
                new Object[]{operationType, caller, declaredValue});
        return true;
    }

    /**
     * Supply chain-specific: Time-based access control for maintenance windows.
     * Restricts certain operations to approved maintenance windows.
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    public boolean authorizeMaintenanceWindow(String operationType, java.time.LocalDateTime scheduledTime) {
        String caller = getCurrentUser();
        
        if (!ctx.isCallerInRole("SYSTEM_ADMIN")) {
            LOGGER.log(Level.WARNING, "MAINTENANCE ACCESS DENIED: Non-admin {0} attempted maintenance operation", caller);
            return false;
        }

        // Check if operation is within approved maintenance window (2 AM - 4 AM)
        java.time.LocalTime operationTime = scheduledTime.toLocalTime();
        java.time.LocalTime windowStart = java.time.LocalTime.of(2, 0);
        java.time.LocalTime windowEnd = java.time.LocalTime.of(4, 0);
        
        boolean inWindow = !operationTime.isBefore(windowStart) && !operationTime.isAfter(windowEnd);
        
        if (inWindow) {
            LOGGER.log(Level.INFO, "MAINTENANCE ACCESS GRANTED: Operation {0} by {1} within approved window",
                    new Object[]{operationType, caller});
            return true;
        } else {
            LOGGER.log(Level.WARNING, "MAINTENANCE ACCESS DENIED: Operation {0} by {1} outside approved window",
                    new Object[]{operationType, caller});
            return false;
        }
    }
}
