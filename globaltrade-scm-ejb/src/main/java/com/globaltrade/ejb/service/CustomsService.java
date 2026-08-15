package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.CustomsDeclaration;
import com.globaltrade.common.enums.ComplianceStatus;
import com.globaltrade.common.exception.CustomsComplianceException;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stateless Session Bean for customs declaration and trade compliance management.
 *
 * <p><strong>Transaction Analysis — REQUIRES_NEW:</strong>
 * Customs declaration processing uses {@code REQUIRES_NEW} because customs
 * operations must commit or fail independently of the calling transaction.
 * If a shipment update fails after customs approval, the customs approval
 * must NOT be rolled back (it's already filed with the customs authority).</p>
 */
@Stateless
@RolesAllowed({"CUSTOMS_AGENT", "LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class CustomsService {

    private static final Logger LOGGER = Logger.getLogger(CustomsService.class.getName());

    // Restricted HS codes (simplified for demonstration)
    private static final List<String> RESTRICTED_HS_CODES = List.of(
        "9301", "9302", "9303", "9304", "2804.70", "2845.10"
    );

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Creates a new customs declaration with compliance validation.
     * Uses REQUIRES_NEW — independent transaction from caller.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CustomsDeclaration createDeclaration(CustomsDeclaration declaration)
            throws CustomsComplianceException {

        // Validate HS code against restricted list
        validateHsCode(declaration.getHsCode(), declaration.getDeclarationNumber());

        // Calculate duty amount
        if (declaration.getTariffRate() != null && declaration.getDeclaredValue() != null) {
            BigDecimal duty = declaration.getDeclaredValue()
                    .multiply(declaration.getTariffRate())
                    .divide(BigDecimal.valueOf(100));
            declaration.setDutyAmount(duty);
        }

        em.persist(declaration);
        LOGGER.log(Level.INFO, "Customs declaration created: {0} for shipment {1}",
                new Object[]{declaration.getDeclarationNumber(), declaration.getShipmentId()});
        return declaration;
    }

    /**
     * Validates HS code against restricted goods list.
     * Throws CustomsComplianceException with rollback=true if restricted.
     */
    private void validateHsCode(String hsCode, String declarationNumber)
            throws CustomsComplianceException {

        for (String restricted : RESTRICTED_HS_CODES) {
            if (hsCode.startsWith(restricted)) {
                throw new CustomsComplianceException(
                    "HS Code " + hsCode + " is restricted for trade. " +
                    "Declaration cannot be processed.",
                    declarationNumber, "RESTRICTED_GOODS");
            }
        }
    }

    /**
     * Approves a customs declaration.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CustomsDeclaration approveDeclaration(Long declarationId)
            throws CustomsComplianceException {

        CustomsDeclaration declaration = em.find(CustomsDeclaration.class, declarationId);
        if (declaration == null) {
            throw new CustomsComplianceException(
                "Declaration not found: " + declarationId,
                String.valueOf(declarationId), "NOT_FOUND");
        }

        declaration.setStatus(ComplianceStatus.COMPLIANT);
        declaration.setApprovedDate(LocalDate.now());
        CustomsDeclaration updated = em.merge(declaration);

        LOGGER.log(Level.INFO, "Customs declaration approved: {0}",
                declaration.getDeclarationNumber());
        return updated;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public CustomsDeclaration findById(Long declarationId) {
        return em.find(CustomsDeclaration.class, declarationId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDeclaration> findPendingDeclarations() {
        return em.createNamedQuery("CustomsDeclaration.findPending", CustomsDeclaration.class)
                .getResultList();
    }

    /**
     * Finds declarations approaching their submission deadline.
     * Used by CustomsDeadlineTimer for automated alerts.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDeclaration> findApproachingDeadlines(int daysAhead) {
        LocalDate deadline = LocalDate.now().plusDays(daysAhead);
        return em.createNamedQuery("CustomsDeclaration.findApproachingDeadline",
                CustomsDeclaration.class)
                .setParameter("deadline", deadline)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDeclaration> findByShipment(Long shipmentId) {
        return em.createNamedQuery("CustomsDeclaration.findByShipment", CustomsDeclaration.class)
                .setParameter("shipmentId", shipmentId)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<CustomsDeclaration> findNonCompliant() {
        return em.createNamedQuery("CustomsDeclaration.findNonCompliant",
                CustomsDeclaration.class)
                .getResultList();
    }
}
