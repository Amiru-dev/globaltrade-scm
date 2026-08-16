package com.globaltrade.common.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents an inventory item stored in a warehouse.
 * Tracks stock levels against reorder thresholds for automated replenishment alerts.
 *
 * <p>The {@code InventoryMonitorTimer} periodically checks all inventory items
 * and raises alerts when quantity falls below reorderLevel.</p>
 */
@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inventory_sku", columnList = "sku"),
        @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_inventory_low_stock", columnList = "quantity, reorder_level")
})
@NamedQueries({
        @NamedQuery(
                name = "InventoryItem.findBySku",
                query = "SELECT i FROM InventoryItem i WHERE i.sku = :sku"
        ),
        @NamedQuery(
                name = "InventoryItem.findByWarehouse",
                query = "SELECT i FROM InventoryItem i WHERE i.warehouseId = :warehouseId"
        ),
        @NamedQuery(
                name = "InventoryItem.findLowStock",
                query = "SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderLevel"
        ),
        @NamedQuery(
                name = "InventoryItem.findOutOfStock",
                query = "SELECT i FROM InventoryItem i WHERE i.quantity = 0"
        )
})
public class InventoryItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(name = "max_stock_level")
    private int maxStockLevel;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "last_replenished")
    private LocalDateTime lastReplenished;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public InventoryItem() {
    }

    public InventoryItem(String sku, String productName, Long warehouseId,
                         int quantity, int reorderLevel) {
        this.sku = sku;
        this.productName = productName;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    // ======================== Business Methods ========================

    /**
     * Checks whether current stock is at or below the reorder threshold.
     */
    public boolean isLowStock() {
        return this.quantity <= this.reorderLevel;
    }

    /**
     * Checks whether the item is completely out of stock.
     */
    public boolean isOutOfStock() {
        return this.quantity == 0;
    }

    // ======================== Getters and Setters ========================

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public int getMaxStockLevel() {
        return maxStockLevel;
    }

    public void setMaxStockLevel(int maxStockLevel) {
        this.maxStockLevel = maxStockLevel;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public LocalDateTime getLastReplenished() {
        return lastReplenished;
    }

    public void setLastReplenished(LocalDateTime lastReplenished) {
        this.lastReplenished = lastReplenished;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
                "itemId=" + itemId +
                ", sku='" + sku + '\'' +
                ", productName='" + productName + '\'' +
                ", warehouseId=" + warehouseId +
                ", quantity=" + quantity +
                ", reorderLevel=" + reorderLevel +
                ", lowStock=" + isLowStock() +
                '}';
    }
}
