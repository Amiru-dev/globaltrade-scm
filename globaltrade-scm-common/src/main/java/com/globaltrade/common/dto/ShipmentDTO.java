package com.globaltrade.common.dto;

import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.ShipmentStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for shipment information.
 * Used for transferring shipment data across service boundaries
 * without exposing JPA entity internals.
 *
 * <p><strong>EJB Best Practice - DTO Pattern:</strong></p>
 * <ul>
 *   <li><strong>Decoupling:</strong> Separates API contract from database schema</li>
 *   <li><strong>Security:</strong> Hides sensitive entity fields and JPA annotations</li>
 *   <li><strong>Performance:</strong> Only transfers needed data, avoiding lazy loading issues</li>
 *   <li><strong>Versioning:</strong> Supports API versioning independent of entity changes</li>
 * </ul>
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

    /**
     * Converts a Shipment entity to ShipmentDTO.
     *
     * @param entity the Shipment entity to convert
     * @return the converted ShipmentDTO, or null if entity is null
     */
    public static ShipmentDTO fromEntity(Shipment entity) {
        if (entity == null) {
            return null;
        }

        ShipmentDTO dto = new ShipmentDTO();
        dto.setShipmentId(entity.getShipmentId());
        dto.setTrackingNumber(entity.getTrackingNumber());
        dto.setOriginCountry(entity.getOriginCountry());
        dto.setOriginCity(entity.getOriginCity());
        dto.setDestinationCountry(entity.getDestinationCountry());
        dto.setDestinationCity(entity.getDestinationCity());
        dto.setCarrier(entity.getCarrier());
        dto.setStatus(entity.getStatus());
        dto.setWeightKg(entity.getWeightKg());
        dto.setDeclaredValue(entity.getDeclaredValue());
        dto.setCurrency(entity.getCurrency());
        dto.setCustomsCleared(entity.isCustomsCleared());
        dto.setEstimatedDelivery(entity.getEstimatedDelivery());
        dto.setActualDelivery(entity.getActualDelivery());
        dto.setNotes(entity.getNotes());

        return dto;
    }

    /**
     * Converts this ShipmentDTO to a Shipment entity.
     *
     * @return the converted Shipment entity
     */
    public Shipment toEntity() {
        Shipment entity = new Shipment();
        entity.setShipmentId(this.shipmentId);
        entity.setTrackingNumber(this.trackingNumber);
        entity.setOriginCountry(this.originCountry);
        entity.setOriginCity(this.originCity);
        entity.setDestinationCountry(this.destinationCountry);
        entity.setDestinationCity(this.destinationCity);
        entity.setCarrier(this.carrier);
        entity.setStatus(this.status);
        entity.setWeightKg(this.weightKg);
        entity.setDeclaredValue(this.declaredValue);
        entity.setCurrency(this.currency);
        entity.setCustomsCleared(this.customsCleared);
        entity.setEstimatedDelivery(this.estimatedDelivery);
        entity.setActualDelivery(this.actualDelivery);
        entity.setNotes(this.notes);

        return entity;
    }

    // ======================== Getters and Setters ========================

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(BigDecimal declaredValue) {
        this.declaredValue = declaredValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isCustomsCleared() {
        return customsCleared;
    }

    public void setCustomsCleared(boolean customsCleared) {
        this.customsCleared = customsCleared;
    }

    public LocalDateTime getEstimatedDelivery() {
        return estimatedDelivery;
    }

    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) {
        this.estimatedDelivery = estimatedDelivery;
    }

    public LocalDateTime getActualDelivery() {
        return actualDelivery;
    }

    public void setActualDelivery(LocalDateTime actualDelivery) {
        this.actualDelivery = actualDelivery;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "ShipmentDTO{" +
                "shipmentId=" + shipmentId +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", originCountry='" + originCountry + '\'' +
                ", originCity='" + originCity + '\'' +
                ", destinationCountry='" + destinationCountry + '\'' +
                ", destinationCity='" + destinationCity + '\'' +
                ", carrier='" + carrier + '\'' +
                ", status=" + status +
                ", weightKg=" + weightKg +
                ", declaredValue=" + declaredValue +
                ", currency='" + currency + '\'' +
                ", customsCleared=" + customsCleared +
                ", estimatedDelivery=" + estimatedDelivery +
                ", actualDelivery=" + actualDelivery +
                ", notes='" + notes + '\'' +
                '}';
    }
}
