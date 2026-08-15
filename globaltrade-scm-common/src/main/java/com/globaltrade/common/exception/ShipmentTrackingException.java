package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when a shipment tracking operation fails — e.g., carrier API timeout,
 * tracking number not found, or status update failure.
 *
 * <p>{@code rollback = false}: Shipment tracking failures are recoverable.
 * The client can retry with an alternative carrier API or use cached status data.
 * The transaction should NOT be rolled back because other operations in the same
 * transaction context (e.g., audit logging) should still persist.</p>
 */
@ApplicationException(rollback = false)
public class ShipmentTrackingException extends SupplyChainException {

    private final String trackingNumber;

    public ShipmentTrackingException(String message, String trackingNumber) {
        super(message, "SCM-SHIP-001", "Shipment", trackingNumber);
        this.trackingNumber = trackingNumber;
    }

    public ShipmentTrackingException(String message, String trackingNumber, Throwable cause) {
        super(message, "SCM-SHIP-001", cause);
        this.trackingNumber = trackingNumber;
    }

    public String getTrackingNumber() { return trackingNumber; }
}
