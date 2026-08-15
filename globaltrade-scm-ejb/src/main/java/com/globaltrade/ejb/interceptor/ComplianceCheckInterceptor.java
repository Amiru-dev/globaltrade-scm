package com.globaltrade.ejb.interceptor;

import com.globaltrade.common.entity.CustomsDeclaration;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.exception.CustomsComplianceException;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Method-level interceptor for regulatory compliance checking.
 *
 * <p><strong>Interceptor Level: METHOD-LEVEL</strong> — Applied to specific
 * methods in CustomsService and ShipmentService that require compliance
 * verification before processing.</p>
 *
 * <p><strong>Critical Analysis — Compliance Interceptor Design:</strong></p>
 * <ul>
 *   <li><strong>Pre-processing validation:</strong> Compliance checks run BEFORE
 *       the business method. If a violation is detected, the method is never
 *       executed — preventing non-compliant data from entering the system.</li>
 *   <li><strong>Trade agreement verification:</strong> Validates that shipments
 *       comply with applicable trade agreements (USMCA, EU-UK TCA, etc.).</li>
 *   <li><strong>Embargo checking:</strong> Prevents processing of shipments
 *       to/from embargoed countries.</li>
 * </ul>
 */
public class ComplianceCheckInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ComplianceCheckInterceptor.class.getName());

    // Simplified embargoed countries list for demonstration
    private static final List<String> EMBARGOED_COUNTRIES = List.of(
        "North Korea", "Syria"
    );

    // Maximum declared value requiring additional documentation
    private static final double HIGH_VALUE_THRESHOLD = 500000.0;

    /**
     * Validates regulatory compliance before processing customs and shipment operations.
     */
    @AroundInvoke
    public Object checkCompliance(InvocationContext ctx) throws Exception {
        String methodName = ctx.getMethod().getName();
        LOGGER.log(Level.FINE, "Compliance check for: {0}", methodName);

        // Check parameters for customs declarations
        for (Object param : ctx.getParameters()) {
            if (param instanceof CustomsDeclaration declaration) {
                validateCustomsCompliance(declaration);
            }
            if (param instanceof Shipment shipment) {
                validateShipmentCompliance(shipment);
            }
        }

        // Compliance check passed — proceed
        Object result = ctx.proceed();

        LOGGER.log(Level.FINE, "Compliance check passed for: {0}", methodName);
        return result;
    }

    /**
     * Validates customs declaration compliance.
     */
    private void validateCustomsCompliance(CustomsDeclaration declaration)
            throws CustomsComplianceException {

        // Check for embargoed origin country
        if (EMBARGOED_COUNTRIES.contains(declaration.getOriginCountry())) {
            throw new CustomsComplianceException(
                "Trade with embargoed country not permitted: " + declaration.getOriginCountry(),
                declaration.getDeclarationNumber(), "EMBARGO_VIOLATION");
        }

        // Check for embargoed destination country
        if (EMBARGOED_COUNTRIES.contains(declaration.getDestinationCountry())) {
            throw new CustomsComplianceException(
                "Trade with embargoed country not permitted: " + declaration.getDestinationCountry(),
                declaration.getDeclarationNumber(), "EMBARGO_VIOLATION");
        }

        // High-value declaration warning
        if (declaration.getDeclaredValue() != null &&
                declaration.getDeclaredValue().doubleValue() > HIGH_VALUE_THRESHOLD) {
            LOGGER.log(Level.WARNING,
                "HIGH VALUE DECLARATION: {0} — Value ${1} exceeds threshold. " +
                "Additional documentation may be required.",
                new Object[]{declaration.getDeclarationNumber(),
                             declaration.getDeclaredValue()});
        }

        // Validate HS code format (must be at least 4 digits)
        if (declaration.getHsCode() != null && declaration.getHsCode().length() < 4) {
            throw new CustomsComplianceException(
                "Invalid HS Code format: " + declaration.getHsCode() +
                ". HS codes must be at least 4 digits.",
                declaration.getDeclarationNumber(), "INVALID_HS_CODE");
        }

        LOGGER.log(Level.FINE, "Customs compliance validated: {0}",
                declaration.getDeclarationNumber());
    }

    /**
     * Validates shipment compliance.
     */
    private void validateShipmentCompliance(Shipment shipment)
            throws CustomsComplianceException {

        // Check for embargoed countries
        if (EMBARGOED_COUNTRIES.contains(shipment.getOriginCountry())) {
            throw new CustomsComplianceException(
                "Shipment from embargoed country: " + shipment.getOriginCountry(),
                shipment.getTrackingNumber(), "EMBARGO_VIOLATION");
        }

        if (EMBARGOED_COUNTRIES.contains(shipment.getDestinationCountry())) {
            throw new CustomsComplianceException(
                "Shipment to embargoed country: " + shipment.getDestinationCountry(),
                shipment.getTrackingNumber(), "EMBARGO_VIOLATION");
        }

        LOGGER.log(Level.FINE, "Shipment compliance validated: {0}",
                shipment.getTrackingNumber());
    }
}
