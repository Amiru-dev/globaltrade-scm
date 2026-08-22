package com.globaltrade.web.rest;

import com.globaltrade.common.dto.DTOConverter;
import com.globaltrade.common.dto.VendorDTO;
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
import java.util.logging.Logger;

/**
 * RESTful resource for vendor management operations.
 *
 * <p><strong>EJB Best Practice - REST Layer Design:</strong></p>
 * <ul>
 *   <li><strong>Thin Controllers:</strong> REST resources are thin wrappers around EJB services</li>
 *   <li><strong>DTO Pattern:</strong> Uses DTOs instead of exposing JPA entities directly</li>
 *   <li><strong>Exception Mapping:</strong> Relies on exception mappers for error handling</li>
 *   <li><strong>Security:</strong> EJB layer enforces security via @RolesAllowed</li>
 * </ul>
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
        List<Vendor> entities = vendorService.findAllVendors();
        List<VendorDTO> dtos = DTOConverter.toVendorDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves a vendor by ID.
     */
    @GET
    @Path("/{id}")
    public Response getVendorById(@PathParam("id") Long id) {
        Vendor entity = vendorService.findById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        VendorDTO dto = DTOConverter.toVendorDTO(entity);
        return Response.ok(dto).build();
    }

    /**
     * Retrieves a vendor by their unique vendor code.
     */
    @GET
    @Path("/code/{vendorCode}")
    public Response getVendorByCode(@PathParam("vendorCode") String vendorCode) {
        Vendor entity = vendorService.findByVendorCode(vendorCode);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        VendorDTO dto = DTOConverter.toVendorDTO(entity);
        return Response.ok(dto).build();
    }

    /**
     * Registers a new vendor.
     */
    @POST
    public Response registerVendor(VendorDTO vendorDTO) throws VendorValidationException {
        Vendor entity = DTOConverter.toVendorEntity(vendorDTO);
        Vendor created = vendorService.registerVendor(entity);
        VendorDTO createdDTO = DTOConverter.toVendorDTO(created);
        return Response.status(Response.Status.CREATED)
                .entity(createdDTO)
                .build();
    }

    /**
     * Updates an existing vendor's details.
     */
    @PUT
    @Path("/{id}")
    public Response updateVendor(@PathParam("id") Long id, VendorDTO vendorDTO) throws VendorValidationException {
        vendorDTO.setVendorId(id);
        Vendor entity = DTOConverter.toVendorEntity(vendorDTO);
        Vendor updated = vendorService.updateVendor(entity);
        VendorDTO updatedDTO = DTOConverter.toVendorDTO(updated);
        return Response.ok(updatedDTO).build();
    }

    /**
     * Retrieves vendors filtered by compliance status.
     */
    @GET
    @Path("/compliance/{status}")
    public Response getVendorsByCompliance(@PathParam("status") String status) {
        ComplianceStatus complianceStatus = ComplianceStatus.valueOf(status.toUpperCase());
        List<Vendor> entities = vendorService.findByComplianceStatus(complianceStatus);
        List<VendorDTO> dtos = DTOConverter.toVendorDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves vendors with performance below a given threshold.
     */
    @GET
    @Path("/low-performance")
    public Response getLowPerformanceVendors(
            @QueryParam("threshold") @DefaultValue("3.0") BigDecimal threshold) {
        List<Vendor> entities = vendorService.findLowPerformanceVendors(threshold);
        List<VendorDTO> dtos = DTOConverter.toVendorDTOList(entities);
        return Response.ok(dtos).build();
    }

    /**
     * Retrieves vendors with expired contracts.
     */
    @GET
    @Path("/expired-contracts")
    public Response getExpiredContracts() {
        List<Vendor> entities = vendorService.findExpiredContracts();
        List<VendorDTO> dtos = DTOConverter.toVendorDTOList(entities);
        return Response.ok(dtos).build();
    }
}
