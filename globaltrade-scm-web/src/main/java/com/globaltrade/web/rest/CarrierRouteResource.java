package com.globaltrade.web.rest;

import com.globaltrade.common.dto.CarrierRouteDTO;
import com.globaltrade.common.dto.DTOConverter;
import com.globaltrade.common.entity.CarrierRoute;
import com.globaltrade.ejb.service.RouteOptimizationService;
import com.globaltrade.ejb.timer.RouteOptimizationTimer;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RESTful resource for carrier route management operations.
 *
 * <p><strong>EJB Best Practice - REST Layer Design:</strong></p>
 * <ul>
 *   <li><strong>Thin Controllers:</strong> REST resources are thin wrappers around EJB services</li>
 *   <li><strong>DTO Pattern:</strong> Uses DTOs instead of exposing JPA entities directly</li>
 *   <li><strong>Exception Mapping:</strong> Relies on exception mappers for error handling</li>
 *   <li><strong>Security:</strong> EJB layer enforces security via @RolesAllowed</li>
 *   <li><strong>Timer Integration:</strong> Triggers RouteOptimizationTimer on cost changes</li>
 * </ul>
 *
 * <p><strong>Timer Trigger Mechanism:</strong></p>
 * <ul>
 *   <li><strong>Automatic Triggering:</strong> POST/PUT operations trigger timer when cost changes</li>
 *   <li><strong>Manual Triggering:</strong> Dedicated endpoint for on-demand optimization</li>
 *   <li><strong>Debouncing:</strong> Timer's built-in debouncing prevents excessive optimization</li>
 * </ul>
 */
@Path("/routes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarrierRouteResource {

    private static final Logger LOGGER = Logger.getLogger(CarrierRouteResource.class.getName());

    @EJB
    private RouteOptimizationService routeService;

    @EJB
    private RouteOptimizationTimer routeTimer;

    /**
     * Retrieves all carrier routes.
     */
    @GET
    public Response getAllRoutes() {
        List<CarrierRoute> entities = routeService.findAllRoutes();
        List<CarrierRouteDTO> dtos = DTOConverter.toCarrierRouteDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves a carrier route by ID.
     */
    @GET
    @Path("/{id}")
    public Response getRouteById(@PathParam("id") Long id) {
        CarrierRoute entity = routeService.findRouteById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        CarrierRouteDTO dto = DTOConverter.toCarrierRouteDTO(entity);
        return Response.ok(dto).build();
    }

    /**
     * Creates a new carrier route.
     * Triggers route optimization timer to recalculate optimal routes.
     */
    @POST
    @RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public Response createRoute(CarrierRouteDTO routeDTO) {
        try {
            CarrierRoute entity = DTOConverter.toCarrierRouteEntity(routeDTO);
            CarrierRoute created = routeService.addRoute(entity);
            CarrierRouteDTO createdDTO = DTOConverter.toCarrierRouteDTO(created);
            
            // Trigger route optimization since a new route may affect optimal calculations
            LOGGER.log(Level.INFO, "New carrier route created - triggering route optimization");
            routeTimer.scheduleOptimization();
            
            return Response.status(Response.Status.CREATED)
                    .entity(createdDTO)
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating carrier route", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to create carrier route\"}")
                    .build();
        }
    }

    /**
     * Updates an existing carrier route.
     * Triggers route optimization timer if cost changes.
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public Response updateRoute(@PathParam("id") Long id, CarrierRouteDTO routeDTO) {
        try {
            CarrierRoute existingRoute = routeService.findRouteById(id);
            if (existingRoute == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Check if cost changed - trigger timer if so
            BigDecimal oldCost = existingRoute.getCostPerKg();
            BigDecimal newCost = routeDTO.getCostPerKg();
            boolean costChanged = (oldCost == null && newCost != null) || 
                               (oldCost != null && !oldCost.equals(newCost));

            // Update the route
            routeDTO.setRouteId(id);
            CarrierRoute entity = DTOConverter.toCarrierRouteEntity(routeDTO);
            CarrierRoute updated = routeService.updateRoute(entity);
            CarrierRouteDTO updatedDTO = DTOConverter.toCarrierRouteDTO(updated);

            // Trigger optimization if cost changed
            if (costChanged) {
                LOGGER.log(Level.INFO, "Carrier route cost changed from {0} to {1} - triggering route optimization", 
                        new Object[]{oldCost, newCost});
                routeTimer.scheduleOptimization();
            }

            return Response.ok(updatedDTO).build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating carrier route", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to update carrier route\"}")
                    .build();
        }
    }

    /**
     * Updates carrier route cost specifically (for external API integration).
     * Triggers route optimization timer automatically if cost changes.
     */
    @PUT
    @Path("/{id}/cost")
    @RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public Response updateRouteCost(@PathParam("id") Long id, 
                                   @QueryParam("cost") BigDecimal newCost) {
        try {
            CarrierRoute existingRoute = routeService.findRouteById(id);
            if (existingRoute == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            BigDecimal oldCost = existingRoute.getCostPerKg();
            
            // Check if cost actually changed
            boolean costChanged = (oldCost == null && newCost != null) || 
                               (oldCost != null && !oldCost.equals(newCost));
            
            // Update cost using the new method
            routeService.updateRouteCost(id, newCost);
            
            // Trigger optimization only if cost changed
            if (costChanged) {
                LOGGER.log(Level.INFO, "Carrier route cost updated from {0} to {1} - triggering route optimization", 
                        new Object[]{oldCost, newCost});
                routeTimer.scheduleOptimization();
                return Response.ok().entity("{\"message\":\"Route cost updated and optimization triggered\"}").build();
            } else {
                LOGGER.log(Level.INFO, "Carrier route cost unchanged - skipping optimization trigger");
                return Response.ok().entity("{\"message\":\"Route cost updated (no change detected)\"}").build();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating route cost", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to update route cost\"}")
                    .build();
        }
    }

    /**
     * Manually triggers route optimization.
     * Useful for immediate optimization or testing purposes.
     */
    @POST
    @Path("/optimize")
    @RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public Response triggerOptimization(
            @QueryParam("delay") @DefaultValue("5000") Long delayMs) {
        try {
            LOGGER.log(Level.INFO, "Manual route optimization trigger requested with delay: {0}ms", delayMs);
            routeTimer.scheduleOptimization(delayMs);
            return Response.ok()
                    .entity("{\"message\":\"Route optimization scheduled in " + delayMs + "ms\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error triggering route optimization", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to trigger route optimization\"}")
                    .build();
        }
    }

    /**
     * Deactivates a carrier route.
     * Triggers route optimization since route availability changed.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public Response deactivateRoute(@PathParam("id") Long id) {
        try {
            CarrierRoute existingRoute = routeService.findRouteById(id);
            if (existingRoute == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            existingRoute.setActive(false);
            routeService.updateRoute(existingRoute);

            // Trigger optimization since route availability changed
            LOGGER.log(Level.INFO, "Carrier route deactivated - triggering route optimization");
            routeTimer.scheduleOptimization();

            return Response.noContent().build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating carrier route", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Failed to deactivate carrier route\"}")
                    .build();
        }
    }
}