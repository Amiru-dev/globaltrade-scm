package com.globaltrade.web.rest;

import com.globaltrade.common.dto.DTOConverter;
import com.globaltrade.common.dto.InventoryItemDTO;
import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.exception.InventoryShortageException;
import com.globaltrade.common.exception.TransactionRollbackException;
import com.globaltrade.ejb.service.InventoryService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Logger;

/**
 * RESTful resource for inventory management operations.
 *
 * <p><strong>EJB Best Practice - REST Layer Design:</strong></p>
 * <ul>
 *   <li><strong>Thin Controllers:</strong> REST resources are thin wrappers around EJB services</li>
 *   <li><strong>DTO Pattern:</strong> Uses DTOs instead of exposing JPA entities directly</li>
 *   <li><strong>Exception Mapping:</strong> Relies on exception mappers for error handling</li>
 *   <li><strong>Security:</strong> EJB layer enforces security via @RolesAllowed</li>
 * </ul>
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
        InventoryItem entity = inventoryService.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        InventoryItemDTO dto = DTOConverter.toInventoryItemDTO(entity);
        return Response.ok(dto).build();
    }

    /**
     * Retrieves items below reorder level — for automated alerts.
     */
    @GET
    @Path("/low-stock")
    public Response getLowStockItems() {
        List<InventoryItem> entities = inventoryService.findLowStockItems();
        List<InventoryItemDTO> dtos = DTOConverter.toInventoryItemDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves out-of-stock items.
     */
    @GET
    @Path("/out-of-stock")
    public Response getOutOfStockItems() {
        List<InventoryItem> entities = inventoryService.findOutOfStockItems();
        List<InventoryItemDTO> dtos = DTOConverter.toInventoryItemDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves inventory items for a specific warehouse.
     */
    @GET
    @Path("/warehouse/{warehouseId}")
    public Response getInventoryByWarehouse(@PathParam("warehouseId") Long warehouseId) {
        List<InventoryItem> entities = inventoryService.findByWarehouse(warehouseId);
        List<InventoryItemDTO> dtos = DTOConverter.toInventoryItemDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Adds a new inventory item to a warehouse.
     */
    @POST
    public Response addInventoryItem(InventoryItemDTO itemDTO) {
        InventoryItem entity = DTOConverter.toInventoryItemEntity(itemDTO);
        InventoryItem created = inventoryService.addInventoryItem(entity);
        InventoryItemDTO createdDTO = DTOConverter.toInventoryItemDTO(created);
        return Response.status(Response.Status.CREATED)
                .entity(createdDTO)
                .build();
    }

    /**
     * Deducts stock from an inventory item.
     */
    @PUT
    @Path("/{id}/deduct")
    public Response deductStock(@PathParam("id") Long id,
                                @QueryParam("quantity") int quantity) throws InventoryShortageException {
        InventoryItem updated = inventoryService.deductStock(id, quantity);
        InventoryItemDTO updatedDTO = DTOConverter.toInventoryItemDTO(updated);
        return Response.ok(updatedDTO).build();
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
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        InventoryItemDTO updatedDTO = DTOConverter.toInventoryItemDTO(updated);
        return Response.ok(updatedDTO).build();
    }

    /**
     * Transfers inventory between warehouses atomically.
     */
    @POST
    @Path("/transfer")
    public Response transferInventory(@QueryParam("sourceId") Long sourceId,
                                      @QueryParam("targetId") Long targetId,
                                      @QueryParam("quantity") int quantity) throws InventoryShortageException, TransactionRollbackException {
        inventoryService.transferInventory(sourceId, targetId, quantity);
        return Response.ok("{\"message\": \"Inventory transferred successfully\"}").build();
    }
}
