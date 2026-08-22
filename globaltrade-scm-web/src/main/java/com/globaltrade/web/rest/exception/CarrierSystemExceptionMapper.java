package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.CarrierSystemException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for CarrierSystemException.
 *
 * <p><strong>Carrier Integration Exception Handling:</strong></p>
 * <ul>
 *   <li><strong>HTTP 503:</strong> Service unavailable - carrier system outages</li>
 *   <li><strong>HTTP 504:</strong> Gateway timeout - carrier system timeout</li>
 *   <li><strong>Recovery:</strong> Supports automatic retry logic with appropriate status codes</li>
 * </ul>
 */
@Provider
public class CarrierSystemExceptionMapper implements ExceptionMapper<CarrierSystemException> {

    private static final Logger LOGGER = Logger.getLogger(CarrierSystemExceptionMapper.class.getName());

    @Override
    public Response toResponse(CarrierSystemException exception) {
        LOGGER.log(Level.SEVERE, "Carrier system exception: Carrier={0}, RetryAttempts={1}",
                new Object[]{exception.getCarrierName(), exception.getRetryAttempts()});

        // Determine appropriate HTTP status based on carrier issue type
        int status = determineStatus(exception);
        String errorType = determineErrorType(status);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                status,
                errorType,
                "Carrier system error: " + exception.getMessage()
        );

        // Add field-level error for carrier system details
        errorResponse.addFieldError("carrier",
                exception.getCarrierName(),
                "Retry attempts: " + exception.getRetryAttempts());

        return Response.status(status)
                .entity(errorResponse)
                .build();
    }

    /**
     * Determines appropriate HTTP status based on carrier issue type.
     */
    private int determineStatus(CarrierSystemException exception) {
        String message = exception.getMessage().toLowerCase();

        if (message.contains("timeout") || message.contains("timed out")) {
            return Response.Status.GATEWAY_TIMEOUT.getStatusCode();
        } else if (message.contains("unavailable") || message.contains("down") || message.contains("outage")) {
            return Response.Status.SERVICE_UNAVAILABLE.getStatusCode();
        } else {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
    }

    /**
     * Determines error type string based on HTTP status.
     */
    private String determineErrorType(int status) {
        switch (status) {
            case 503:
                return "Service Unavailable";
            case 504:
                return "Gateway Timeout";
            default:
                return "Carrier System Error";
        }
    }
}