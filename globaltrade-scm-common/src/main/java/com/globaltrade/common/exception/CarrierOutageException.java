package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Supply chain-specific exception for carrier outages affecting global logistics.
 *
 * <p><strong>Use Case:</strong> Thrown when a carrier experiences a partial or complete
 * outage that affects multiple shipments, requiring alternative routing or vendor
 * escalation for supply chain continuity.</p>
 *
 * <p>{@code rollback = false}: Carrier outages are external events. Transaction
 * rollback would not help resolve the carrier issue and would only lose business
 * context needed for recovery strategies.</p>
 */
@ApplicationException(rollback = false)
public class CarrierOutageException extends SupplyChainException {

    private final String carrierName;
    private final String outageType;
    private final int affectedShipments;
    private final java.time.LocalDateTime outageStartTime;

    public CarrierOutageException(String message, String carrierName, String outageType,
                                  int affectedShipments, java.time.LocalDateTime outageStartTime) {
        super(message, "SCM-CARR-OUTAGE-001", "CarrierOutage", carrierName);
        this.carrierName = carrierName;
        this.outageType = outageType;
        this.affectedShipments = affectedShipments;
        this.outageStartTime = outageStartTime;
    }

    public CarrierOutageException(String message, String carrierName, String outageType,
                                  int affectedShipments, java.time.LocalDateTime outageStartTime,
                                  Throwable cause) {
        super(message, "SCM-CARR-OUTAGE-001", "CarrierOutage", carrierName, cause);
        this.carrierName = carrierName;
        this.outageType = outageType;
        this.affectedShipments = affectedShipments;
        this.outageStartTime = outageStartTime;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getOutageType() {
        return outageType;
    }

    public int getAffectedShipments() {
        return affectedShipments;
    }

    public java.time.LocalDateTime getOutageStartTime() {
        return outageStartTime;
    }
}