package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.ShipmentTrackingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for ShipmentTrackingException.
 *
 * <p><strong>Exception Mapping Strategy:</strong></p>
 * <ul>
 *   <li><strong>HTTP 404:</strong> Shipment not found scenarios</li>
 *   <li><strong>HTTP 400:</strong> Invalid tracking number format</li>
 *   <li><strong>HTTP 409:</strong> Shipment status conflict (e.g., cannot delete non-cancelled shipment)</li>
 * </ul>
 */
@Provider
public class ShipmentTrackingExceptionMapper implements ExceptionMapper<ShipmentTrackingException> {

    private static final Logger LOGGER = Logger.getLogger(ShipmentTrackingExceptionMapper.class.getName());

    @Override
    public Response toResponse(ShipmentTrackingException exception) {
        LOGGER.log(Level.WARNING, "Shipment tracking exception: {0}, Tracking number: {1}",
                new Object[]{exception.getMessage(), exception.getTrackingNumber()});

        // Determine appropriate HTTP status based on exception context
        int status = determineStatus(exception);
        String errorType = determineErrorType(status);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                status,
                errorType,
                exception.getMessage()
        );

        return Response.status(status)
                .entity(errorResponse)
                .build();
    }

    /**
     * Determines appropriate HTTP status based on exception context.
     */
    private int determineStatus(ShipmentTrackingException exception) {
        String message = exception.getMessage().toLowerCase();

        if (message.contains("not found")) {
            return Response.Status.NOT_FOUND.getStatusCode();
        } else if (message.contains("cannot delete") || message.contains("only cancelled")) {
            return Response.Status.CONFLICT.getStatusCode();
        } else if (message.contains("invalid") || message.contains("format")) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
    }

    /**
     * Determines error type string based on HTTP status.
     */
    private String determineErrorType(int status) {
        switch (status) {
            case 404:
                return "Not Found";
            case 409:
                return "Conflict";
            case 400:
                return "Bad Request";
            default:
                return "Shipment Tracking Error";
        }
    }
}