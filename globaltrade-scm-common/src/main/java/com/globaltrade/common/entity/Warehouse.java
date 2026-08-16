package com.globaltrade.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a physical warehouse/storage facility in the logistics network.
 * Tracks capacity and utilization for warehouse management and inventory allocation.
 */
@Entity
@Table(name = "warehouses", indexes = {
        @Index(name = "idx_warehouse_region", columnList = "region"),
        @Index(name = "idx_warehouse_utilization", columnList = "utilization_percent")
})
@NamedQueries({
        @NamedQuery(
                name = "Warehouse.findAll",
                query = "SELECT w FROM Warehouse w ORDER BY w.region, w.location"
        ),
        @NamedQuery(
                name = "Warehouse.findByRegion",
                query = "SELECT w FROM Warehouse w WHERE w.region = :region"
        ),
        @NamedQuery(
                name = "Warehouse.findHighUtilization",
                query = "SELECT w FROM Warehouse w WHERE w.utilizationPercent > :threshold ORDER BY w.utilizationPercent DESC"
        )
})
public class Warehouse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_code", unique = true, nullable = false, length = 20)
    private String warehouseCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "region", nullable = false, length = 50)
    private String region;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "utilization_percent", precision = 5, scale = 2)
    private BigDecimal utilizationPercent;

    @Column(name = "manager_name", length = 150)
    private String managerName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

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
        if (this.utilizationPercent == null) {
            this.utilizationPercent = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public Warehouse() {
    }

    public Warehouse(String warehouseCode, String name, String location,
                     String country, String region, int capacity) {
        this.warehouseCode = warehouseCode;
        this.name = name;
        this.location = location;
        this.country = country;
        this.region = region;
        this.capacity = capacity;
        this.active = true;
        this.utilizationPercent = BigDecimal.ZERO;
    }

    // ======================== Getters and Setters ========================

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public void setWarehouseCode(String warehouseCode) {
        this.warehouseCode = warehouseCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getUtilizationPercent() {
        return utilizationPercent;
    }

    public void setUtilizationPercent(BigDecimal utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
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
        return "Warehouse{" +
                "warehouseId=" + warehouseId +
                ", warehouseCode='" + warehouseCode + '\'' +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", utilization=" + utilizationPercent + "%" +
                '}';
    }
}
