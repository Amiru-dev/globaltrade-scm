package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Base exception for all supply chain application-level exceptions.
 *
 * <p>Annotated with {@code @ApplicationException} to indicate this is a
 * business-level exception that should NOT trigger automatic container
 * rollback by default. Subclasses override the rollback behaviour
 * based on the severity of the specific exception scenario.</p>
 *
 * <p><strong>Critical Analysis — Application vs System Exceptions:</strong></p>
 * <ul>
 *   <li>Application exceptions are checked business exceptions that the client
 *       can anticipate and potentially recover from.</li>
 *   <li>They preserve the EJB instance (no bean discard) and give the client
 *       control over transaction outcome.</li>
 *   <li>System exceptions (extending {@code EJBException}) cause automatic
 *       rollback, bean discard, and log the error — used for unrecoverable failures.</li>
 * </ul>
 */
@ApplicationException(rollback = false)
public abstract class SupplyChainException extends Exception {

    private final String errorCode;
    private final String entityType;
    private final String entityId;

    protected SupplyChainException(String message) {
        super(message);
        this.errorCode = "SCM-000";
        this.entityType = null;
        this.entityId = null;
    }

    protected SupplyChainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.entityType = null;
        this.entityId = null;
    }

    protected SupplyChainException(String message, String errorCode,
                                   String entityType, String entityId) {
        super(message);
        this.errorCode = errorCode;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    protected SupplyChainException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SCM-000";
        this.entityType = null;
        this.entityId = null;
    }

    protected SupplyChainException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.entityType = null;
        this.entityId = null;
    }

    protected SupplyChainException(String message, String errorCode,
                                   String entityType, String entityId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }
}
