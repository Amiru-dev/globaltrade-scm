package com.globaltrade.ejb.interceptor;

import com.globaltrade.common.entity.Vendor;
import com.globaltrade.common.exception.VendorValidationException;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Method-level interceptor for validating vendor data before persistence.
 *
 * <p><strong>Interceptor Level: METHOD-LEVEL</strong> — Applied selectively to
 * vendor create/update methods using {@code @Interceptors(VendorDataValidator.class)}.
 * Method-level is appropriate here because only specific operations require
 * input validation, not all service methods.</p>
 *
 * <p><strong>Critical Analysis — Method-level vs Class-level:</strong></p>
 * <ul>
 *   <li><strong>Method-level</strong> (chosen here): Targeted validation only
 *       where needed. Reduces unnecessary overhead on read operations.</li>
 *   <li><strong>Class-level</strong>: Would apply validation to every method,
 *       including read operations that don't modify data — wasteful.</li>
 *   <li><strong>Short-circuit pattern:</strong> Validation fails fast before
 *       the actual business method executes, preventing unnecessary
 *       database operations on invalid data.</li>
 * </ul>
 */
public class VendorDataValidator implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(VendorDataValidator.class.getName());

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9\\-\\s]{7,20}$");
    private static final Pattern VENDOR_CODE_PATTERN =
            Pattern.compile("^VND-[0-9]{3,}$");

    /**
     * Validates vendor data before the business method proceeds.
     * Uses short-circuit pattern — throws exception immediately on first validation failure.
     */
    @AroundInvoke
    public Object validateVendorData(InvocationContext ctx) throws Exception {
        LOGGER.log(Level.FINE, "VendorDataValidator intercepting: {0}",
                ctx.getMethod().getName());

        // Find Vendor parameter in method arguments
        for (Object param : ctx.getParameters()) {
            if (param instanceof Vendor vendor) {
                validateVendor(vendor);
            }
        }

        // Validation passed — proceed to actual business method
        return ctx.proceed();
    }

    /**
     * Performs comprehensive validation on vendor data.
     */
    private void validateVendor(Vendor vendor) throws VendorValidationException {
        // Validate vendor code format
        if (vendor.getVendorCode() == null || vendor.getVendorCode().isBlank()) {
            throw new VendorValidationException(
                "Vendor code is required",
                vendor.getVendorCode(), "vendorCode");
        }
        if (!VENDOR_CODE_PATTERN.matcher(vendor.getVendorCode()).matches()) {
            throw new VendorValidationException(
                "Vendor code must follow format: VND-XXX (e.g., VND-001)",
                vendor.getVendorCode(), "vendorCode");
        }

        // Validate name
        if (vendor.getName() == null || vendor.getName().isBlank()) {
            throw new VendorValidationException(
                "Vendor name is required",
                vendor.getVendorCode(), "name");
        }
        if (vendor.getName().length() < 3 || vendor.getName().length() > 200) {
            throw new VendorValidationException(
                "Vendor name must be between 3 and 200 characters",
                vendor.getVendorCode(), "name");
        }

        // Validate country
        if (vendor.getCountry() == null || vendor.getCountry().isBlank()) {
            throw new VendorValidationException(
                "Country is required for trade compliance",
                vendor.getVendorCode(), "country");
        }

        // Validate email format (if provided)
        if (vendor.getContactEmail() != null && !vendor.getContactEmail().isBlank()) {
            if (!EMAIL_PATTERN.matcher(vendor.getContactEmail()).matches()) {
                throw new VendorValidationException(
                    "Invalid email format: " + vendor.getContactEmail(),
                    vendor.getVendorCode(), "contactEmail");
            }
        }

        // Validate phone format (if provided)
        if (vendor.getContactPhone() != null && !vendor.getContactPhone().isBlank()) {
            if (!PHONE_PATTERN.matcher(vendor.getContactPhone()).matches()) {
                throw new VendorValidationException(
                    "Invalid phone format: " + vendor.getContactPhone(),
                    vendor.getVendorCode(), "contactPhone");
            }
        }

        // Validate category
        if (vendor.getCategory() == null || vendor.getCategory().isBlank()) {
            throw new VendorValidationException(
                "Vendor category is required",
                vendor.getVendorCode(), "category");
        }

        LOGGER.log(Level.FINE, "Vendor data validated: {0} ({1})",
                new Object[]{vendor.getVendorCode(), vendor.getName()});
    }
}
