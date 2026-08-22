package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when a data integrity violation requires mandatory transaction rollback —
 * e.g., inventory data corruption, duplicate key violations, or constraint failures.
 *
 * <p>{@code rollback = true}: Data integrity violations leave the system in an
 * inconsistent state. All changes in the current transaction MUST be undone
 * to maintain ACID properties. Unlike {@code CarrierSystemException} (System Exception),
 * this is an Application Exception — the EJB instance is NOT discarded,
 * allowing the container to reuse it for subsequent requests.</p>
 */
@ApplicationException(rollback = true)
public class TransactionRollbackException extends SupplyChainException {

    private final String transactionId;

    public TransactionRollbackException(String message, String transactionId) {
        super(message, "SCM-TX-001");
        this.transactionId = transactionId;
    }

    public TransactionRollbackException(String message, String transactionId, Throwable cause) {
        super(message, "SCM-TX-001", cause);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
