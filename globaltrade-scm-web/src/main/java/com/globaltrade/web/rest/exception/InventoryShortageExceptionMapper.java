package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.InventoryShortageException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for InventoryShortageException.
 *
 * <p><strong>Supply Chain Exception Handling:</strong></p>
 * <ul>
 *   <li><strong>HTTP 409:</strong> Inventory conflict - insufficient stock</li>
 *   <li><strong>Business Logic:</strong> Distinguishes between permanent shortage and temporary allocation issues</li>
 *   <li><strong>Recovery Support:</strong> Includes available quantity in error response for client recovery</li>
 * </ul>
 */
@Provider
public class InventoryShortageExceptionMapper implements ExceptionMapper<InventoryShortageException> {

    private static final Logger LOGGER = Logger.getLogger(InventoryShortageExceptionMapper.class.getName());

    @Override
    public Response toResponse(InventoryShortageException exception) {
        LOGGER.log(Level.WARNING, "Inventory shortage: SKU={0}, Requested={1}, Available={2}",
                new Object[]{exception.getSku(), exception.getRequestedQuantity(),
                        exception.getAvailableQuantity()});

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                Response.Status.CONFLICT.getStatusCode(),
                "Inventory Shortage",
                exception.getMessage()
        );

        // Add field-level error for client recovery
        errorResponse.addFieldError("quantity",
                String.valueOf(exception.getRequestedQuantity()),
                "Insufficient stock. Available: " + exception.getAvailableQuantity());

        return Response.status(Response.Status.CONFLICT)
                .entity(errorResponse)
                .build();
    }
}