package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.CustomsComplianceException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for CustomsComplianceException.
 *
 * <p><strong>Trade Compliance Exception Handling:</strong></p>
 * <ul>
 *   <li><strong>HTTP 422:</strong> Unprocessable entity - compliance validation failures</li>
 *   <li><strong>HTTP 403:</strong> Forbidden - restricted goods or sanctions</li>
 *   <li><strong>Regulatory:</strong> Supports customs and trade agreement compliance requirements</li>
 * </ul>
 */
@Provider
public class CustomsComplianceExceptionMapper implements ExceptionMapper<CustomsComplianceException> {

    private static final Logger LOGGER = Logger.getLogger(CustomsComplianceExceptionMapper.class.getName());

    @Override
    public Response toResponse(CustomsComplianceException exception) {
        LOGGER.log(Level.WARNING, "Customs compliance exception: Declaration={0}, Violation={1}",
                new Object[]{exception.getDeclarationNumber(), exception.getViolationType()});

        // Determine appropriate HTTP status based on compliance issue type
        int status = determineStatus(exception);
        String errorType = determineErrorType(status);

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                status,
                errorType,
                exception.getMessage()
        );

        // Add field-level error for compliance details
        errorResponse.addFieldError("compliance",
                exception.getViolationType(),
                "Declaration " + exception.getDeclarationNumber() + " failed compliance check");

        return Response.status(status)
                .entity(errorResponse)
                .build();
    }

    /**
     * Determines appropriate HTTP status based on compliance issue type.
     */
    private int determineStatus(CustomsComplianceException exception) {
        String violation = exception.getViolationType().toLowerCase();

        if (violation.contains("restricted") || violation.contains("sanctions") || violation.contains("prohibited")) {
            return Response.Status.FORBIDDEN.getStatusCode();
        } else if (violation.contains("missing") || violation.contains("incomplete") || violation.contains("invalid")) {
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
            case 403:
                return "Forbidden";
            case 400:
                return "Bad Request";
            default:
                return "Customs Compliance Error";
        }
    }
}