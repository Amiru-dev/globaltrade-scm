package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.VendorValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for VendorValidationException.
 *
 * <p><strong>Vendor Data Validation Exception Handling:</strong></p>
 * <ul>
 *   <li><strong>HTTP 400:</strong> Invalid vendor data</li>
 *   <li><strong>HTTP 409:</strong> Duplicate vendor code or business rule violation</li>
 *   <li><strong>Field Validation:</strong> Detailed field-level error reporting</li>
 * </ul>
 */
@Provider
public class VendorValidationExceptionMapper implements ExceptionMapper<VendorValidationException> {

    private static final Logger LOGGER = Logger.getLogger(VendorValidationExceptionMapper.class.getName());

    @Override
    public Response toResponse(VendorValidationException exception) {
        LOGGER.log(Level.WARNING, "Vendor validation exception: Vendor={0}, Field={1}, Reason={2}",
                new Object[]{exception.getVendorCode(), exception.getFieldName(),
                        exception.getMessage()});

        // Determine appropriate HTTP status based on validation issue type
        int status = determineStatus(exception);
        String errorType = determineErrorType(status);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                status,
                errorType,
                exception.getMessage()
        );

        // Add field-level error for validation details
        errorResponse.addFieldError(exception.getFieldName(),
                exception.getVendorCode(),
                exception.getMessage());

        return Response.status(status)
                .entity(errorResponse)
                .build();
    }

    /**
     * Determines appropriate HTTP status based on validation issue type.
     */
    private int determineStatus(VendorValidationException exception) {
        String message = exception.getMessage().toLowerCase();

        if (message.contains("exists") || message.contains("duplicate")) {
            return Response.Status.CONFLICT.getStatusCode();
        } else if (message.contains("invalid") || message.contains("format")) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        } else {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
    }

    /**
     * Determines error type string based on HTTP status.
     */
    private String determineErrorType(int status) {
        switch (status) {
            case 409:
                return "Conflict";
            case 400:
                return "Bad Request";
            default:
                return "Validation Error";
        }
    }
}