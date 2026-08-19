package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Supply chain-specific exception for customs processing failures in international trade.
 *
 * <p><strong>Use Case:</strong> Thrown when customs declaration processing fails due to
 * regulatory issues, documentation errors, or customs authority system failures that
 * affect international shipment clearance.</p>
 *
 * <p>{@code rollback = true}: Customs failures are critical for international trade compliance.
 * Any shipment-related data must be rolled back to prevent illegal cross-border movement
 * and ensure regulatory compliance.</p>
 */
@ApplicationException(rollback = true)
public class CustomsFailureException extends SupplyChainException {

    private final String customsAuthority;
    private final String failureReason;
    private final String shipmentTrackingNumber;
    private final java.time.LocalDateTime failureTimestamp;

    public CustomsFailureException(String message, String customsAuthority, String failureReason,
                                   String shipmentTrackingNumber, java.time.LocalDateTime failureTimestamp) {
        super(message, "SCM-CUST-FAIL-001", "CustomsFailure", shipmentTrackingNumber);
        this.customsAuthority = customsAuthority;
        this.failureReason = failureReason;
        this.shipmentTrackingNumber = shipmentTrackingNumber;
        this.failureTimestamp = failureTimestamp;
    }

    public CustomsFailureException(String message, String customsAuthority, String failureReason,
                                   String shipmentTrackingNumber, java.time.LocalDateTime failureTimestamp,
                                   Throwable cause) {
        super(message, "SCM-CUST-FAIL-001", "CustomsFailure", shipmentTrackingNumber, cause);
        this.customsAuthority = customsAuthority;
        this.failureReason = failureReason;
        this.shipmentTrackingNumber = shipmentTrackingNumber;
        this.failureTimestamp = failureTimestamp;
    }

    public String getCustomsAuthority() { return customsAuthority; }
    public String getFailureReason() { return failureReason; }
    public String getShipmentTrackingNumber() { return shipmentTrackingNumber; }
    public java.time.LocalDateTime getFailureTimestamp() { return failureTimestamp; }
}