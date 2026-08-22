package com.globaltrade.web.rest.exception;

import com.globaltrade.common.dto.ErrorResponseDTO;
import com.globaltrade.common.exception.TransactionRollbackException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exception mapper for TransactionRollbackException.
 *
 * <p><strong>Transaction Exception Handling:</strong></p>
 * <ul>
 *   <li><strong>HTTP 409:</strong> Transaction conflict - rollback scenarios</li>
 *   <li><strong>Business Integrity:</strong> Ensures atomic operation failures are properly communicated</li>
 *   <li><strong>Recovery Support:</strong> Provides transaction context for client retry logic</li>
 * </ul>
 */
@Provider
public class TransactionRollbackExceptionMapper implements ExceptionMapper<TransactionRollbackException> {

    private static final Logger LOGGER = Logger.getLogger(TransactionRollbackExceptionMapper.class.getName());

    @Override
    public Response toResponse(TransactionRollbackException exception) {
        LOGGER.log(Level.SEVERE, "Transaction rollback: TransactionId={0}, Reason={1}",
                new Object[]{exception.getTransactionId(), exception.getMessage()});

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                Response.Status.CONFLICT.getStatusCode(),
                "Transaction Conflict",
                "Transaction rolled back: " + exception.getMessage()
        );

        // Add field-level error for transaction context
        errorResponse.addFieldError("transactionId",
                exception.getTransactionId(),
                exception.getMessage());

        return Response.status(Response.Status.CONFLICT)
                .entity(errorResponse)
                .build();
    }
}