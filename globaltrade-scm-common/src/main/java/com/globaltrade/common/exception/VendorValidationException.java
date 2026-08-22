package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when vendor data validation fails — e.g., invalid contact details,
 * missing required fields, or duplicate vendor code.
 *
 * <p>{@code rollback = false}: Vendor validation failures are correctable.
 * The client can fix the input data and resubmit. No transaction rollback
 * is needed as no partial state change has occurred at this point.</p>
 */
@ApplicationException(rollback = false)
public class VendorValidationException extends SupplyChainException {

    private final String vendorCode;
    private final String fieldName;

    public VendorValidationException(String message, String vendorCode, String fieldName) {
        super(message, "SCM-VND-001", "Vendor", vendorCode);
        this.vendorCode = vendorCode;
        this.fieldName = fieldName;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public String getFieldName() {
        return fieldName;
    }
}
