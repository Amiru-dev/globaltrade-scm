package com.globaltrade.web.rest;

import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.ejb.service.ShipmentService;
import com.globaltrade.ejb.service.InventoryService;
import com.globaltrade.ejb.service.VendorManagementService;
import com.globaltrade.ejb.service.SystemConfigService;

import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * RESTful resource providing a consolidated dashboard view of key
 * supply chain metrics for management reporting.
 *
 * <p><strong>Critical Analysis:</strong> This resource aggregates data from
 * multiple EJB services to build a unified view. Each EJB call uses
 * {@code SUPPORTS} transaction attribute (read-only), so no transaction
 * overhead is incurred for this reporting endpoint.</p>
 */
@Path("/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @EJB
    private ShipmentService shipmentService;

    @EJB
    private InventoryService inventoryService;

    @EJB
    private VendorManagementService vendorService;

    @EJB
    private SystemConfigService configService;

    /**
     * Returns a summary of key supply chain metrics.
     */
    @GET
    @Path("/summary")
    public Response getDashboardSummary() {
        long inTransit = shipmentService.countByStatus(ShipmentStatus.IN_TRANSIT);
        long delivered = shipmentService.countByStatus(ShipmentStatus.DELIVERED);
        long atCustoms = shipmentService.countByStatus(ShipmentStatus.AT_CUSTOMS);
        long delayed = shipmentService.findDelayedShipments().size();
        int lowStockCount = inventoryService.findLowStockItems().size();
        int outOfStockCount = inventoryService.findOutOfStockItems().size();
        int expiredContracts = vendorService.findExpiredContracts().size();

        JsonObject summary = Json.createObjectBuilder()
                .add("shipments", Json.createObjectBuilder()
                        .add("inTransit", inTransit)
                        .add("delivered", delivered)
                        .add("pendingCustoms", atCustoms)
                        .add("delayed", delayed))
                .add("inventory", Json.createObjectBuilder()
                        .add("lowStockAlerts", lowStockCount)
                        .add("outOfStock", outOfStockCount))
                .add("vendors", Json.createObjectBuilder()
                        .add("expiredContracts", expiredContracts))
                .add("system", Json.createObjectBuilder()
                        .add("status", "OPERATIONAL")
                        .add("maintenanceMode",
                                configService.getConfig("maintenance.mode", "false")))
                .build();

        return Response.ok(summary).build();
    }

    /**
     * Health check endpoint for monitoring tools and load balancers.
     */
    @GET
    @Path("/health")
    public Response healthCheck() {
        JsonObject health = Json.createObjectBuilder()
                .add("status", "UP")
                .add("application", "GlobalTrade SCM")
                .add("version", "1.0-SNAPSHOT")
                .build();

        return Response.ok(health).build();
    }
}
