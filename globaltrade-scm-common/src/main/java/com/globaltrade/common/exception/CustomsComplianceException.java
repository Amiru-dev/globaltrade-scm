package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when a customs compliance violation is detected — e.g., invalid HS code,
 * restricted goods, missing documentation, or trade agreement violation.
 *
 * <p>{@code rollback = true}: Customs compliance violations are critical.
 * Any data persisted as part of a non-compliant operation MUST be rolled back
 * to prevent illegal trade activities from being recorded in the system.
 * This ensures data integrity and regulatory compliance.</p>
 */
@ApplicationException(rollback = true)
public class CustomsComplianceException extends SupplyChainException {

    private final String declarationNumber;
    private final String violationType;

    public CustomsComplianceException(String message, String declarationNumber,
                                      String violationType) {
        super(message, "SCM-CUST-001", "CustomsDeclaration", declarationNumber);
        this.declarationNumber = declarationNumber;
        this.violationType = violationType;
    }

    public CustomsComplianceException(String message, String declarationNumber,
                                      String violationType, Throwable cause) {
        super(message, "SCM-CUST-001", cause);
        this.declarationNumber = declarationNumber;
        this.violationType = violationType;
    }

    public String getDeclarationNumber() {
        return declarationNumber;
    }

    public String getViolationType() {
        return violationType;
    }
}
