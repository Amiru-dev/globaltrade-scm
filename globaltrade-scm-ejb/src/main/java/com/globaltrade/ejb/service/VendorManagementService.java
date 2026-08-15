package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.Vendor;
import com.globaltrade.common.enums.ComplianceStatus;
import com.globaltrade.common.exception.VendorValidationException;

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
 * Stateless Session Bean for vendor management and performance evaluation.
 *
 * <p>Vendor operations include CRUD, performance scoring, and compliance tracking.
 * The VendorDataValidator interceptor automatically validates vendor input data
 * before persistence operations.</p>
 */
@Stateless
@RolesAllowed({"LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
public class VendorManagementService {

    private static final Logger LOGGER = Logger.getLogger(VendorManagementService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Registers a new vendor in the system.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor registerVendor(Vendor vendor) throws VendorValidationException {
        // Check for duplicate vendor code
        List<Vendor> existing = em.createQuery(
            "SELECT v FROM Vendor v WHERE v.vendorCode = :code", Vendor.class)
            .setParameter("code", vendor.getVendorCode())
            .getResultList();

        if (!existing.isEmpty()) {
            throw new VendorValidationException(
                "Vendor code already exists: " + vendor.getVendorCode(),
                vendor.getVendorCode(), "vendorCode");
        }

        em.persist(vendor);
        LOGGER.log(Level.INFO, "Vendor registered: {0} ({1})",
                new Object[]{vendor.getName(), vendor.getVendorCode()});
        return vendor;
    }

    /**
     * Updates vendor details.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Vendor updateVendor(Vendor vendor) throws VendorValidationException {
        Vendor existing = em.find(Vendor.class, vendor.getVendorId());
        if (existing == null) {
            throw new VendorValidationException(
                "Vendor not found: " + vendor.getVendorId(),
                vendor.getVendorCode(), "vendorId");
        }

        existing.setName(vendor.getName());
        existing.setCountry(vendor.getCountry());
        existing.setCity(vendor.getCity());
        existing.setContactEmail(vendor.getContactEmail());
        existing.setContactPhone(vendor.getContactPhone());
        existing.setCategory(vendor.getCategory());
        existing.setPaymentTerms(vendor.getPaymentTerms());

        Vendor updated = em.merge(existing);
        LOGGER.log(Level.INFO, "Vendor updated: {0}", vendor.getVendorCode());
        return updated;
    }

    /**
     * Updates vendor performance rating — called by VendorPerformanceTimer.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updatePerformanceRating(Long vendorId, BigDecimal newRating) {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor != null) {
            BigDecimal oldRating = vendor.getPerformanceRating();
            vendor.setPerformanceRating(newRating);
            em.merge(vendor);
            LOGGER.log(Level.INFO, "Vendor {0} rating updated: {1} → {2}",
                    new Object[]{vendor.getVendorCode(), oldRating, newRating});
        }
    }

    /**
     * Updates vendor compliance status.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateComplianceStatus(Long vendorId, ComplianceStatus status) {
        Vendor vendor = em.find(Vendor.class, vendorId);
        if (vendor != null) {
            vendor.setComplianceStatus(status);
            em.merge(vendor);
            LOGGER.log(Level.INFO, "Vendor {0} compliance: {1}",
                    new Object[]{vendor.getVendorCode(), status});
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Vendor findById(Long vendorId) {
        return em.find(Vendor.class, vendorId);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findAllVendors() {
        return em.createNamedQuery("Vendor.findAll", Vendor.class).getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findByComplianceStatus(ComplianceStatus status) {
        return em.createNamedQuery("Vendor.findByComplianceStatus", Vendor.class)
                .setParameter("status", status)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findLowPerformanceVendors(BigDecimal threshold) {
        return em.createNamedQuery("Vendor.findLowPerformance", Vendor.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Vendor> findExpiredContracts() {
        return em.createNamedQuery("Vendor.findExpiredContracts", Vendor.class)
                .setParameter("date", LocalDate.now())
                .getResultList();
    }

    /**
     * Allows vendor representatives to view their own data.
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @RolesAllowed({"VENDOR_REPRESENTATIVE", "LOGISTICS_COORDINATOR", "SYSTEM_ADMIN"})
    public Vendor findByVendorCode(String vendorCode) {
        List<Vendor> results = em.createQuery(
            "SELECT v FROM Vendor v WHERE v.vendorCode = :code", Vendor.class)
            .setParameter("code", vendorCode)
            .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
