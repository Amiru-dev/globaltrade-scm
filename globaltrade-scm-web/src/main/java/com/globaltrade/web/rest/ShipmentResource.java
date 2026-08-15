package com.globaltrade.web.rest;

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
 * <p><strong>Critical Analysis — REST Layer Design:</strong></p>
 * <ul>
 *   <li>REST resources are thin wrappers around EJB services — business logic
 *       resides entirely in the EJB layer, not in the REST controller.</li>
 *   <li>Exception mapping converts EJB application exceptions to appropriate
 *       HTTP status codes (404, 400, 500).</li>
 *   <li>Security is enforced at the EJB layer via {@code @RolesAllowed}
 *       and at the web layer via {@code web.xml} security constraints —
 *       providing defence-in-depth.</li>
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
        List<Shipment> shipments = shipmentService.findActiveShipments();
        return Response.ok(shipments).build();
    }

    /**
     * Retrieves a shipment by its ID.
     */
    @GET
    @Path("/{id}")
    public Response getShipmentById(@PathParam("id") Long id) {
        Shipment shipment = shipmentService.findById(id);
        if (shipment == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Shipment not found with ID: " + id + "\"}")
                    .build();
        }
        return Response.ok(shipment).build();
    }

    /**
     * Tracks a shipment by its tracking number.
     */
    @GET
    @Path("/track/{trackingNumber}")
    public Response trackShipment(@PathParam("trackingNumber") String trackingNumber) {
        try {
            Shipment shipment = shipmentService.findByTrackingNumber(trackingNumber);
            return Response.ok(shipment).build();
        } catch (ShipmentTrackingException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Retrieves shipments filtered by status.
     */
    @GET
    @Path("/status/{status}")
    public Response getShipmentsByStatus(@PathParam("status") String status) {
        try {
            ShipmentStatus shipmentStatus = ShipmentStatus.valueOf(status.toUpperCase());
            List<Shipment> shipments = shipmentService.findByStatus(shipmentStatus);
            return Response.ok(shipments).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid shipment status: " + status + "\"}")
                    .build();
        }
    }

    /**
     * Creates a new shipment.
     */
    @POST
    public Response createShipment(Shipment shipment) {
        try {
            Shipment created = shipmentService.createShipment(shipment);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating shipment", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create shipment: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Updates the status of an existing shipment.
     */
    @PUT
    @Path("/{id}/status")
    public Response updateStatus(@PathParam("id") Long id,
                                  @QueryParam("status") String status) {
        try {
            ShipmentStatus newStatus = ShipmentStatus.valueOf(status.toUpperCase());
            Shipment updated = shipmentService.updateShipmentStatus(id, newStatus);
            return Response.ok(updated).build();
        } catch (ShipmentTrackingException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid status: " + status + "\"}")
                    .build();
        }
    }

    /**
     * Deletes a cancelled shipment.
     */
    @DELETE
    @Path("/{id}")
    public Response deleteShipment(@PathParam("id") Long id) {
        try {
            shipmentService.deleteShipment(id);
            return Response.noContent().build();
        } catch (ShipmentTrackingException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Retrieves delayed shipments — for dashboard alerts.
     */
    @GET
    @Path("/delayed")
    public Response getDelayedShipments() {
        List<Shipment> delayed = shipmentService.findDelayedShipments();
        return Response.ok(delayed).build();
    }
}
