package com.globaltrade.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a shipping route between two locations with carrier information.
 * Used by the RouteOptimizationTimer to calculate optimal shipping paths
 * based on cost, transit time, and reliability metrics.
 */
@Entity
@Table(name = "carrier_routes", indexes = {
        @Index(name = "idx_route_carrier", columnList = "carrier"),
        @Index(name = "idx_route_origin_dest", columnList = "origin, destination")
})
@NamedQueries({
        @NamedQuery(
                name = "CarrierRoute.findByCarrier",
                query = "SELECT cr FROM CarrierRoute cr WHERE cr.carrier = :carrier"
        ),
        @NamedQuery(
                name = "CarrierRoute.findByOriginDestination",
                query = "SELECT cr FROM CarrierRoute cr WHERE cr.origin = :origin AND cr.destination = :destination " +
                        "ORDER BY cr.costPerKg ASC"
        ),
        @NamedQuery(
                name = "CarrierRoute.findAll",
                query = "SELECT cr FROM CarrierRoute cr ORDER BY cr.carrier, cr.origin"
        ),
        @NamedQuery(
                name = "CarrierRoute.findMostReliable",
                query = "SELECT cr FROM CarrierRoute cr WHERE cr.origin = :origin AND cr.destination = :destination " +
                        "ORDER BY cr.reliabilityScore DESC"
        )
})
public class CarrierRoute implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "route_code", unique = true, nullable = false, length = 20)
    private String routeCode;

    @Column(name = "carrier", nullable = false, length = 100)
    private String carrier;

    @Column(name = "origin", nullable = false, length = 100)
    private String origin;

    @Column(name = "destination", nullable = false, length = 100)
    private String destination;

    @Column(name = "transit_days", nullable = false)
    private int transitDays;

    @Column(name = "cost_per_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal costPerKg;

    @Column(name = "max_weight_kg", precision = 10, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "reliability_score", precision = 3, scale = 2)
    private BigDecimal reliabilityScore;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
        if (this.reliabilityScore == null) {
            this.reliabilityScore = new BigDecimal("0.90");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public CarrierRoute() {
    }

    public CarrierRoute(String routeCode, String carrier, String origin,
                        String destination, int transitDays, BigDecimal costPerKg) {
        this.routeCode = routeCode;
        this.carrier = carrier;
        this.origin = origin;
        this.destination = destination;
        this.transitDays = transitDays;
        this.costPerKg = costPerKg;
        this.active = true;
    }

    // ======================== Getters and Setters ========================

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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "CarrierRoute{" +
                "routeId=" + routeId +
                ", carrier='" + carrier + '\'' +
                ", route='" + origin + " → " + destination + '\'' +
                ", transitDays=" + transitDays +
                ", costPerKg=" + costPerKg +
                ", reliability=" + reliabilityScore +
                '}';
    }
}
