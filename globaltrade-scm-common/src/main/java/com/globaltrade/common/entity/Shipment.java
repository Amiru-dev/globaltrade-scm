package com.globaltrade.common.entity;

import com.globaltrade.common.enums.ShipmentStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an individual cargo shipment within the global supply chain.
 * Tracks the full lifecycle from creation through customs clearance to final delivery.
 *
 * <p>Named queries are provided for common lookup patterns used by timer services
 * and dashboard reporting.</p>
 */
@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipment_status", columnList = "status"),
    @Index(name = "idx_shipment_carrier", columnList = "carrier"),
    @Index(name = "idx_shipment_destination", columnList = "destination_country")
})
@NamedQueries({
    @NamedQuery(
        name = "Shipment.findByStatus",
        query = "SELECT s FROM Shipment s WHERE s.status = :status ORDER BY s.estimatedDelivery"
    ),
    @NamedQuery(
        name = "Shipment.findActiveShipments",
        query = "SELECT s FROM Shipment s WHERE s.status NOT IN " +
                "(com.globaltrade.common.enums.ShipmentStatus.DELIVERED, " +
                "com.globaltrade.common.enums.ShipmentStatus.CANCELLED) " +
                "ORDER BY s.estimatedDelivery"
    ),
    @NamedQuery(
        name = "Shipment.findDelayed",
        query = "SELECT s FROM Shipment s WHERE s.status = com.globaltrade.common.enums.ShipmentStatus.DELAYED"
    ),
    @NamedQuery(
        name = "Shipment.findByCarrier",
        query = "SELECT s FROM Shipment s WHERE s.carrier = :carrier"
    ),
    @NamedQuery(
        name = "Shipment.findPendingCustoms",
        query = "SELECT s FROM Shipment s WHERE s.status = com.globaltrade.common.enums.ShipmentStatus.AT_CUSTOMS"
    )
})
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long shipmentId;

    @Column(name = "tracking_number", unique = true, nullable = false, length = 50)
    private String trackingNumber;

    @Column(name = "origin_country", nullable = false, length = 100)
    private String originCountry;

    @Column(name = "origin_city", nullable = false, length = 100)
    private String originCity;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "carrier", nullable = false, length = 100)
    private String carrier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentStatus status;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "declared_value", precision = 15, scale = 2)
    private BigDecimal declaredValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "customs_cleared")
    private boolean customsCleared;

    @Column(name = "estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @Column(name = "actual_delivery")
    private LocalDateTime actualDelivery;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ShipmentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public Shipment() {
    }

    public Shipment(String trackingNumber, String originCountry, String originCity,
                    String destinationCountry, String destinationCity, String carrier) {
        this.trackingNumber = trackingNumber;
        this.originCountry = originCountry;
        this.originCity = originCity;
        this.destinationCountry = destinationCountry;
        this.destinationCity = destinationCity;
        this.carrier = carrier;
        this.status = ShipmentStatus.CREATED;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "shipmentId=" + shipmentId +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", origin='" + originCity + ", " + originCountry + '\'' +
                ", destination='" + destinationCity + ", " + destinationCountry + '\'' +
                ", carrier='" + carrier + '\'' +
                ", status=" + status +
                '}';
    }
}
