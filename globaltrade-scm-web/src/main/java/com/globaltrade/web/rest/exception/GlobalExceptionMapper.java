package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global exception mapper for unhandled exceptions.
 *
 * <p><strong>EJB Exception Handling Best Practice:</strong></p>
 * <ul>
 *   <li><strong>Catch-All:</strong> Handles any exception not specifically mapped</li>
 *   <li><strong>Security:</strong> Never exposes stack traces or sensitive information</li>
 *   <li><strong>Logging:</strong> Full server-side logging for debugging</li>
 *   <li><strong>User Experience:</strong> Generic error message for client safety</li>
 * </ul>
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Log the full exception on server side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception occurred", exception);

        // Return generic error to client for security
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists."
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}