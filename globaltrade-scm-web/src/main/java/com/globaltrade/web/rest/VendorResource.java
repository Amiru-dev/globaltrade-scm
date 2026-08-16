package com.globaltrade.web.rest;

import com.globaltrade.common.entity.Vendor;
import com.globaltrade.common.enums.ComplianceStatus;
import com.globaltrade.common.exception.VendorValidationException;
import com.globaltrade.ejb.service.VendorManagementService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RESTful resource for vendor management operations.
 *
 * <p>Exposes vendor CRUD, performance rating queries, and compliance
 * status management via REST endpoints.</p>
 */
@Path("/vendors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VendorResource {

    private static final Logger LOGGER = Logger.getLogger(VendorResource.class.getName());

    @EJB
    private VendorManagementService vendorService;

    /**
     * Retrieves all registered vendors.
     */
    @GET
    public Response getAllVendors() {
        List<Vendor> vendors = vendorService.findAllVendors();
        return Response.ok(vendors).build();
    }

    /**
     * Retrieves a vendor by ID.
     */
    @GET
    @Path("/{id}")
    public Response getVendorById(@PathParam("id") Long id) {
        Vendor vendor = vendorService.findById(id);
        if (vendor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Vendor not found with ID: " + id + "\"}")
                    .build();
        }
        return Response.ok(vendor).build();
    }

    /**
     * Retrieves a vendor by their unique vendor code.
     */
    @GET
    @Path("/code/{vendorCode}")
    public Response getVendorByCode(@PathParam("vendorCode") String vendorCode) {
        Vendor vendor = vendorService.findByVendorCode(vendorCode);
        if (vendor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Vendor not found with code: " + vendorCode + "\"}")
                    .build();
        }
        return Response.ok(vendor).build();
    }

    /**
     * Registers a new vendor.
     */
    @POST
    public Response registerVendor(Vendor vendor) {
        try {
            Vendor created = vendorService.registerVendor(vendor);
            return Response.status(Response.Status.CREATED)
                    .entity(created)
                    .build();
        } catch (VendorValidationException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error registering vendor", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to register vendor: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Updates an existing vendor's details.
     */
    @PUT
    @Path("/{id}")
    public Response updateVendor(@PathParam("id") Long id, Vendor vendor) {
        try {
            vendor.setVendorId(id);
            Vendor updated = vendorService.updateVendor(vendor);
            return Response.ok(updated).build();
        } catch (VendorValidationException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    /**
     * Retrieves vendors filtered by compliance status.
     */
    @GET
    @Path("/compliance/{status}")
    public Response getVendorsByCompliance(@PathParam("status") String status) {
        try {
            ComplianceStatus complianceStatus = ComplianceStatus.valueOf(status.toUpperCase());
            List<Vendor> vendors = vendorService.findByComplianceStatus(complianceStatus);
            return Response.ok(vendors).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Invalid compliance status: " + status + "\"}")
                    .build();
        }
    }

    /**
     * Retrieves vendors with performance below a given threshold.
     */
    @GET
    @Path("/low-performance")
    public Response getLowPerformanceVendors(
            @QueryParam("threshold") @DefaultValue("3.0") BigDecimal threshold) {
        List<Vendor> vendors = vendorService.findLowPerformanceVendors(threshold);
        return Response.ok(vendors).build();
    }

    /**
     * Retrieves vendors with expired contracts.
     */
    @GET
    @Path("/expired-contracts")
    public Response getExpiredContracts() {
        List<Vendor> vendors = vendorService.findExpiredContracts();
        return Response.ok(vendors).build();
    }
}
