package com.globaltrade.web.rest;

import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.exception.InventoryShortageException;
import com.globaltrade.ejb.service.InventoryService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RESTful resource for inventory management operations.
 *
 * <p>Exposes inventory queries, stock adjustments, and warehouse transfer
 * operations via a clean REST API.</p>
 */
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    private static final Logger LOGGER = Logger.getLogger(InventoryResource.class.getName());

    @EJB
    private InventoryService inventoryService;

    /**
     * Retrieves an inventory item by ID.
     */
    @GET
    @Path("/{id}")
    public Response getInventoryById(@PathParam("id") Long id) {
        InventoryItem item = inventoryService.findById(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Inventory item not found with ID: " + id + "\"}")
                    .build();
        }
        return Response.ok(item).build();
    }

    /**
     * Retrieves items below reorder level — for automated alerts.
     */
    @GET
    @Path("/low-stock")
    public Response getLowStockItems() {
        List<InventoryItem> lowStock = inventoryService.findLowStockItems();
        return Response.ok(lowStock).build();
    }

    /**
     * Retrieves out-of-stock items.
     */
    @GET
    @Path("/out-of-stock")
    public Response getOutOfStockItems() {
        List<InventoryItem> outOfStock = inventoryService.findOutOfStockItems();
        return Response.ok(outOfStock).build();
    }

    /**
     * Retrieves inventory items for a specific warehouse.
     */
    @GET
    @Path("/warehouse/{warehouseId}")
    public Response getInventoryByWarehouse(@PathParam("warehouseId") Long warehouseId) {
        List<InventoryItem> items = inventoryService.findByWarehouse(warehouseId);
        return Response.ok(items).build();
    }

    /**
     * Adds a new inventory item to a warehouse.
     */
    @POST
    public Response addInventoryItem(InventoryItem item) {
        try {
            InventoryItem created = inventoryService.addInventoryItem(item);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating inventory item", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create inventory item: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Deducts stock from an inventory item.
     */
    @PUT
    @Path("/{id}/deduct")
    public Response deductStock(@PathParam("id") Long id,
                                @QueryParam("quantity") int quantity) {
        try {
            InventoryItem updated = inventoryService.deductStock(id, quantity);
            return Response.ok(updated).build();
        } catch (InventoryShortageException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Replenishes stock for an inventory item.
     */
    @PUT
    @Path("/{id}/replenish")
    public Response replenishStock(@PathParam("id") Long id,
                                   @QueryParam("quantity") int quantity) {
        InventoryItem updated = inventoryService.replenishStock(id, quantity);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Inventory item not found: " + id + "\"}")
                    .build();
        }
        return Response.ok(updated).build();
    }

    /**
     * Transfers inventory between warehouses atomically.
     */
    @POST
    @Path("/transfer")
    public Response transferInventory(@QueryParam("sourceId") Long sourceId,
                                      @QueryParam("targetId") Long targetId,
                                      @QueryParam("quantity") int quantity) {
        try {
            inventoryService.transferInventory(sourceId, targetId, quantity);
            return Response.ok("{\"message\": \"Inventory transferred successfully\"}").build();
        } catch (InventoryShortageException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Transfer failed: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
