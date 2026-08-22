package com.globaltrade.common.dto;

import com.globaltrade.common.entity.InventoryItem;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for inventory item information.
 * Used for transferring inventory data across service boundaries
 * without exposing JPA entity internals.
 */
public class InventoryItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;
    private String sku;
    private String productName;
    private Long warehouseId;
    private int quantity;
    private int reorderLevel;
    private int maxStockLevel;
    private String unitOfMeasure;
    private LocalDateTime lastReplenished;

    public InventoryItemDTO() {
    }

    /**
     * Converts an InventoryItem entity to InventoryItemDTO.
     */
    public static InventoryItemDTO fromEntity(InventoryItem entity) {
        if (entity == null) {
            return null;
        }

        InventoryItemDTO dto = new InventoryItemDTO();
        dto.setItemId(entity.getItemId());
        dto.setSku(entity.getSku());
        dto.setProductName(entity.getProductName());
        dto.setWarehouseId(entity.getWarehouseId());
        dto.setQuantity(entity.getQuantity());
        dto.setReorderLevel(entity.getReorderLevel());
        dto.setMaxStockLevel(entity.getMaxStockLevel());
        dto.setUnitOfMeasure(entity.getUnitOfMeasure());
        dto.setLastReplenished(entity.getLastReplenished());

        return dto;
    }

    /**
     * Converts this InventoryItemDTO to an InventoryItem entity.
     */
    public InventoryItem toEntity() {
        InventoryItem entity = new InventoryItem();
        entity.setItemId(this.itemId);
        entity.setSku(this.sku);
        entity.setProductName(this.productName);
        entity.setWarehouseId(this.warehouseId);
        entity.setQuantity(this.quantity);
        entity.setReorderLevel(this.reorderLevel);
        entity.setMaxStockLevel(this.maxStockLevel);
        entity.setUnitOfMeasure(this.unitOfMeasure);
        entity.setLastReplenished(this.lastReplenished);

        return entity;
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "InventoryItemDTO{" +
                "itemId=" + itemId +
                ", sku='" + sku + '\'' +
                ", productName='" + productName + '\'' +
                ", warehouseId=" + warehouseId +
                ", quantity=" + quantity +
                ", reorderLevel=" + reorderLevel +
                ", maxStockLevel=" + maxStockLevel +
                ", unitOfMeasure='" + unitOfMeasure + '\'' +
                ", lastReplenished=" + lastReplenished +
                '}';
    }
}