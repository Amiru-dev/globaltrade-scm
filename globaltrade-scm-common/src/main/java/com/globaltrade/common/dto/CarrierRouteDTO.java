package com.globaltrade.common.dto;

import com.globaltrade.common.entity.CarrierRoute;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for carrier route information.
 * Used for transferring carrier route data across service boundaries
 * without exposing JPA entity internals.
 */
public class CarrierRouteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long routeId;
    private String routeCode;
    private String carrier;
    private String origin;
    private String destination;
    private int transitDays;
    private BigDecimal costPerKg;
    private BigDecimal maxWeightKg;
    private BigDecimal reliabilityScore;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CarrierRouteDTO() {
    }

    /**
     * Converts a CarrierRoute entity to CarrierRouteDTO.
     */
    public static CarrierRouteDTO fromEntity(CarrierRoute entity) {
        if (entity == null) {
            return null;
        }

        CarrierRouteDTO dto = new CarrierRouteDTO();
        dto.setRouteId(entity.getRouteId());
        dto.setRouteCode(entity.getRouteCode());
        dto.setCarrier(entity.getCarrier());
        dto.setOrigin(entity.getOrigin());
        dto.setDestination(entity.getDestination());
        dto.setTransitDays(entity.getTransitDays());
        dto.setCostPerKg(entity.getCostPerKg());
        dto.setMaxWeightKg(entity.getMaxWeightKg());
        dto.setReliabilityScore(entity.getReliabilityScore());
        dto.setActive(entity.isActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /**
     * Converts this CarrierRouteDTO to a CarrierRoute entity.
     */
    public CarrierRoute toEntity() {
        CarrierRoute entity = new CarrierRoute();
        entity.setRouteId(this.routeId);
        entity.setRouteCode(this.routeCode);
        entity.setCarrier(this.carrier);
        entity.setOrigin(this.origin);
        entity.setDestination(this.destination);
        entity.setTransitDays(this.transitDays);
        entity.setCostPerKg(this.costPerKg);
        entity.setMaxWeightKg(this.maxWeightKg);
        entity.setReliabilityScore(this.reliabilityScore);
        entity.setActive(this.active);

        return entity;
    }

    // Getters and Setters
    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getRouteCode() {
        return routeCode;
    }

    public void setRouteCode(String routeCode) {
        this.routeCode = routeCode;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getTransitDays() {
        return transitDays;
    }

    public void setTransitDays(int transitDays) {
        this.transitDays = transitDays;
    }

    public BigDecimal getCostPerKg() {
        return costPerKg;
    }

    public void setCostPerKg(BigDecimal costPerKg) {
        this.costPerKg = costPerKg;
    }

    public BigDecimal getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(BigDecimal maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }

    public BigDecimal getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(BigDecimal reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "CarrierRouteDTO{" +
                "routeId=" + routeId +
                ", routeCode='" + routeCode + '\'' +
                ", carrier='" + carrier + '\'' +
                ", origin='" + origin + '\'' +
                ", destination='" + destination + '\'' +
                ", transitDays=" + transitDays +
                ", costPerKg=" + costPerKg +
                ", maxWeightKg=" + maxWeightKg +
                ", reliabilityScore=" + reliabilityScore +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}