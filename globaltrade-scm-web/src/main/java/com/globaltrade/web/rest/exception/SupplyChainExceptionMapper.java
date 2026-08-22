package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.SupplyChainException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for base SupplyChainException.
 *
 * <p><strong>EJB Exception Handling Best Practice:</strong></p>
 * <ul>
 *   <li><strong>Centralized Handling:</strong> All supply chain exceptions handled in one place</li>
 *   <li><strong>Consistent Responses:</strong> Standardized error format across all endpoints</li>
 *   <li><strong>Security:</strong> No stack traces exposed to clients</li>
 *   <li><strong>Logging:</strong> Server-side logging for debugging while protecting client</li>
 * </ul>
 */
@Provider
public class SupplyChainExceptionMapper implements ExceptionMapper<SupplyChainException> {

    private static final Logger LOGGER = Logger.getLogger(SupplyChainExceptionMapper.class.getName());

    @Override
    public Response toResponse(SupplyChainException exception) {
        LOGGER.log(Level.WARNING, "Supply chain exception occurred: {0}", exception.getMessage());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Supply Chain Error",
                exception.getMessage()
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}