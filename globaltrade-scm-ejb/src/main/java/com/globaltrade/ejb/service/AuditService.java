package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.AuditLog;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Session Bean for audit log persistence.
 *
 * <p><strong>Transaction Management:</strong> Uses REQUIRES_NEW to ensure
 * audit logs are persisted in their own transaction, independent of the
 * business method's transaction outcome. This guarantees audit trail
 * integrity even if the business operation rolls back.</p>
 *
 * <p><strong>Separation of Concerns:</strong> Extracting audit persistence
 * into a dedicated EJB ensures proper transaction management and avoids
 * the TransactionRequiredException that can occur when interceptors try
 * to persist entities directly.</p>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class AuditService {

    private static final Logger LOGGER = Logger.getLogger(AuditService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Persists an audit log entry in a separate transaction.
     *
     * @param auditLog The audit log entry to persist
     */
    public void persistAuditLog(AuditLog auditLog) {
        try {
            em.persist(auditLog);
            LOGGER.log(Level.FINE, "Audit log persisted: {0}", auditLog.getMethodName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist audit log", e);
            // Don't throw - audit logging failure should not affect business operations
        }
    }
}