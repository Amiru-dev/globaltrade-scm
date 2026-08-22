package com.globaltrade.web.rest;

import com.globaltrade.common.dto.DTOConverter;
import com.globaltrade.common.dto.ShipmentDTO;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.common.exception.ShipmentTrackingException;
import com.globaltrade.ejb.service.ShipmentService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RESTful resource for shipment management operations.
 *
 * <p><strong>EJB Best Practice - REST Layer Design:</strong></p>
 * <ul>
 *   <li><strong>Thin Controllers:</strong> REST resources are thin wrappers around EJB services</li>
 *   <li><strong>DTO Pattern:</strong> Uses DTOs instead of exposing JPA entities directly</li>
 *   <li><strong>Exception Mapping:</strong> Relies on exception mappers for error handling</li>
 *   <li><strong>Security:</strong> EJB layer enforces security via @RolesAllowed</li>
 * </ul>
 */
@Path("/shipments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    private static final Logger LOGGER = Logger.getLogger(ShipmentResource.class.getName());

    @EJB
    private ShipmentService shipmentService;

    /**
     * Retrieves all active (non-delivered, non-cancelled) shipments.
     */
    @GET
    public Response getActiveShipments() {
        List<Shipment> entities = shipmentService.findActiveShipments();
        List<ShipmentDTO> dtos = DTOConverter.toShipmentDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves a shipment by its ID.
     */
    @GET
    @Path("/{id}")
    public Response getShipmentById(@PathParam("id") Long id) {
        Shipment entity = shipmentService.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ShipmentDTO dto = DTOConverter.toShipmentDTO(entity);
        return Response.ok(dto).build();
    }

    /**
     * Tracks a shipment by its tracking number.
     */
    @GET
    @Path("/track/{trackingNumber}")
    public Response trackShipment(@PathParam("trackingNumber") String trackingNumber) throws ShipmentTrackingException {
        Shipment entity = shipmentService.findByTrackingNumber(trackingNumber);
        ShipmentDTO dto = DTOConverter.toShipmentDTO(entity);
        return Response.ok(dto).build();
    }

    /**
     * Retrieves shipments filtered by status.
     */
    @GET
    @Path("/status/{status}")
    public Response getShipmentsByStatus(@PathParam("status") String status) {
        ShipmentStatus shipmentStatus = ShipmentStatus.valueOf(status.toUpperCase());
        List<Shipment> entities = shipmentService.findByStatus(shipmentStatus);
        List<ShipmentDTO> dtos = DTOConverter.toShipmentDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Creates a new shipment.
     */
    @POST
    public Response createShipment(ShipmentDTO shipmentDTO) throws ShipmentTrackingException {
        Shipment entity = DTOConverter.toShipmentEntity(shipmentDTO);
        Shipment created = shipmentService.createShipment(entity);
        ShipmentDTO createdDTO = DTOConverter.toShipmentDTO(created);
        return Response.status(Response.Status.CREATED)
                .entity(createdDTO)
                .build();
    }

    /**
     * Updates the status of an existing shipment.
     */
    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id,
                                 @QueryParam("status") String status) throws ShipmentTrackingException {
        ShipmentStatus newStatus = ShipmentStatus.valueOf(status.toUpperCase());
        Shipment updated = shipmentService.updateShipmentStatus(id, newStatus);
        ShipmentDTO updatedDTO = DTOConverter.toShipmentDTO(updated);
        return Response.ok(updatedDTO).build();
    }

    /**
     * Deletes a cancelled shipment.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteShipment(@PathParam("id") Long id) throws ShipmentTrackingException {
        shipmentService.deleteShipment(id);
        return Response.noContent().build();
    }

    /**
     * Retrieves delayed shipments — for dashboard alerts.
     */
    @GET
    @Path("/delayed")
    public Response getDelayedShipments() {
        List<Shipment> entities = shipmentService.findDelayedShipments();
        List<ShipmentDTO> dtos = DTOConverter.toShipmentDTOList(entities);
        return Response.ok(dtos).build();
    }
}
