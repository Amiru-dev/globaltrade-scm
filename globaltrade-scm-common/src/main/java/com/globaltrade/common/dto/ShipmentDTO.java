package com.globaltrade.common.dto;

import com.globaltrade.common.enums.ShipmentStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for shipment information.
 * Used for transferring shipment data across service boundaries
 * without exposing JPA entity internals.
 */
public class ShipmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long shipmentId;
    private String trackingNumber;
    private String originCountry;
    private String originCity;
    private String destinationCountry;
    private String destinationCity;
    private String carrier;
    private ShipmentStatus status;
    private BigDecimal weightKg;
    private BigDecimal declaredValue;
    private String currency;
    private boolean customsCleared;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime actualDelivery;
    private String notes;

    public ShipmentDTO() {
    }

    // ======================== Getters and Setters ========================

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }

    public String getOriginCity() { return originCity; }
    public void setOriginCity(String originCity) { this.originCity = originCity; }

    public String getDestinationCountry() { return destinationCountry; }
    public void setDestinationCountry(String destinationCountry) { this.destinationCountry = destinationCountry; }

    public String getDestinationCity() { return destinationCity; }
    public void setDestinationCity(String destinationCity) { this.destinationCity = destinationCity; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public BigDecimal getDeclaredValue() { return declaredValue; }
    public void setDeclaredValue(BigDecimal declaredValue) { this.declaredValue = declaredValue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isCustomsCleared() { return customsCleared; }
    public void setCustomsCleared(boolean customsCleared) { this.customsCleared = customsCleared; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public LocalDateTime getActualDelivery() { return actualDelivery; }
    public void setActualDelivery(LocalDateTime actualDelivery) { this.actualDelivery = actualDelivery; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
