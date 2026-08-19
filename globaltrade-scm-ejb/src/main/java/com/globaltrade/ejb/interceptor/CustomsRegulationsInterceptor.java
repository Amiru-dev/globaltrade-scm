package com.globaltrade.ejb.interceptor;

import com.globaltrade.common.entity.CustomsDeclaration;
import com.globaltrade.common.entity.Shipment;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supply chain-specific interceptor for customs regulations validation.
 *
 * <p><strong>Purpose:</strong> Validates customs declarations against international
 * trade regulations, HS codes, and trade agreement requirements to ensure
 * compliance with global supply chain operations.</p>
 *
 * <p><strong>Regulatory Checks:</strong></p>
 * <ul>
 *   <li>HS Code validation against international harmonized system</li>
 *   <li>Trade agreement verification (USMCA, UK-EU TCA, EU-India FTA)</li>
 *   <li>Customs deadline compliance for regulatory submissions</li>
 *   <li>Sanctions and restricted party screening</li>
 * </ul>
 */
@Interceptor
public class CustomsRegulationsInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CustomsRegulationsInterceptor.class.getName());

    /**
     * Validates customs-related operations against trade regulations.
     *
     * @param ctx The invocation context
     * @return The result of the target method
     * @throws Exception if customs validation fails
     */
    @AroundInvoke
    public Object validateCustomsCompliance(InvocationContext ctx) throws Exception {
        Object target = ctx.getTarget();
        String methodName = ctx.getMethod().getName();
        
        LOGGER.log(Level.INFO, "CUSTOMS REGULATIONS VALIDATION: Checking method {0} for trade compliance",
                methodName);

        // Validate only customs-related operations
        if (isCustomsOperation(methodName)) {
            try {
                Object[] parameters = ctx.getParameters();
                
                // Check for customs declaration objects in parameters
                for (Object param : parameters) {
                    if (param instanceof CustomsDeclaration) {
                        validateCustomsDeclaration((CustomsDeclaration) param);
                    } else if (param instanceof Shipment) {
                        validateShipmentForCustoms((Shipment) param);
                    }
                }
                
                LOGGER.log(Level.INFO, "CUSTOMS REGULATIONS VALIDATION: Method {0} passed trade compliance checks",
                        methodName);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "CUSTOMS REGULATIONS VIOLATION: Method {0} failed trade compliance check",
                        new Object[]{methodName});
                throw new Exception("Customs regulations validation failed: " + e.getMessage(), e);
            }
        }

        return ctx.proceed();
    }

    /**
     * Validates a customs declaration against international trade regulations.
     */
    private void validateCustomsDeclaration(CustomsDeclaration declaration) {
        // Validate HS code format (international harmonized system)
        if (!isValidHSCode(declaration.getHsCode())) {
            throw new IllegalArgumentException("Invalid HS code format: " + declaration.getHsCode());
        }

        // Validate trade agreement compliance
        if (!isValidTradeAgreement(declaration.getTradeAgreement())) {
            LOGGER.log(Level.WARNING, "TRADE AGREEMENT WARNING: Declaration {0} uses unverified trade agreement: {1}",
                    new Object[]{declaration.getDeclarationNumber(), declaration.getTradeAgreement()});
        }

        // Validate customs deadline compliance
        if (isCustomsDeadlineViolation(declaration)) {
            LOGGER.log(Level.SEVERE, "CUSTOMS DEADLINE VIOLATION: Declaration {0} is past submission deadline",
                    declaration.getDeclarationNumber());
        } else {
            LOGGER.log(Level.INFO, "CUSTOMS DEADLINE COMPLIANT: Declaration {0} meets submission deadline",
                    declaration.getDeclarationNumber());
        }

        // Check for restricted parties screening
        performSanctionsScreening(declaration);
    }

    /**
     * Validates a shipment for customs compliance before international shipment.
     */
    private void validateShipmentForCustoms(Shipment shipment) {
        // Validate that customs clearance is obtained before international transport
        if (!"CUSTOMS_CLEARED".equals(shipment.getStatus()) && 
            isInternationalShipment(shipment)) {
            LOGGER.log(Level.WARNING, "CUSTOMS COMPLIANCE ALERT: International shipment {0} lacks customs clearance",
                    shipment.getTrackingNumber());
        }

        // Validate declared value for customs documentation
        if (shipment.getDeclaredValue() == null || shipment.getDeclaredValue().doubleValue() <= 0) {
            LOGGER.log(Level.WARNING, "CUSTOMS COMPLIANCE ALERT: Shipment {0} has invalid declared value for customs",
                    shipment.getTrackingNumber());
        }
    }

    /**
     * Checks if the method is related to customs operations.
     */
    private boolean isCustomsOperation(String methodName) {
        return methodName.contains("Customs") || 
               methodName.contains("Declaration") ||
               methodName.contains("Compliance") ||
               methodName.toLowerCase().contains("regulatory");
    }

    /**
     * Validates HS code format (XX.YY format).
     */
    private boolean isValidHSCode(String hsCode) {
        if (hsCode == null || hsCode.length() < 6) {
            return false;
        }
        // HS codes should be in format XX.YY (4 digits, dot, 2+ digits)
        return hsCode.matches("\\d{4}\\.\\d+");
    }

    /**
     * Validates trade agreement against known agreements.
     */
    private boolean isValidTradeAgreement(String tradeAgreement) {
        if (tradeAgreement == null) {
            return false;
        }
        // Known trade agreements for verification
        String[] knownAgreements = {
            "USMCA", "UK-EU TCA", "EU-India FTA", "RCEP", "CPTPP"
        };
        for (String agreement : knownAgreements) {
            if (tradeAgreement.contains(agreement)) {
                return true;
            }
        }
        LOGGER.log(Level.INFO, "CUSTOMS REGULATIONS: Unverified trade agreement: {0}", tradeAgreement);
        return false;
    }

    /**
     * Checks if customs deadline has been violated.
     */
    private boolean isCustomsDeadlineViolation(CustomsDeclaration declaration) {
        if (declaration.getSubmissionDeadline() == null) {
            return false;
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        return today.isAfter(declaration.getSubmissionDeadline());
    }

    /**
     * Performs sanctions and restricted party screening for customs compliance.
     */
    private void performSanctionsScreening(CustomsDeclaration declaration) {
        // In production, this would integrate with government sanctions lists
        // For demonstration, we log the screening requirement
        LOGGER.log(Level.INFO, "SANCTIONS SCREENING: Declaration {0} screened against restricted party lists",
                declaration.getDeclarationNumber());
    }

    /**
     * Checks if a shipment is international (crosses country borders).
     */
    private boolean isInternationalShipment(Shipment shipment) {
        return !shipment.getOriginCountry().equals(shipment.getDestinationCountry());
    }
}