package com.globaltrade.common.dto;

import com.globaltrade.common.entity.InventoryItem;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for inventory item information with Bean Validation.
 *
 * <p><strong>Critical Analysis — Input Validation:</strong></p>
 * <ul>
 *   <li><strong>Bean Validation:</strong> Uses Jakarta Bean Validation annotations
 *       for automatic input validation at REST endpoints.</li>
 *   <li><strong>Security:</strong> Prevents invalid data from entering the system,
 *       protecting against injection attacks and data corruption.</li>
 *   <li><strong>Consistency:</strong> Ensures data integrity constraints are enforced
 *       at the API boundary before reaching business logic.</li>
 * </ul>
 */
public class InventoryItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long itemId;

    @NotBlank(message = "SKU is required")
    @Size(min = 1, max = 50, message = "SKU must be 1-50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String productName;

    @NotNull(message = "Warehouse ID is required")
    private Long warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 1000000, message = "Quantity cannot exceed 1,000,000")
    private int quantity;

    @NotNull(message = "Reorder level is required")
    @Min(value = 0, message = "Reorder level cannot be negative")
    @Max(value = 10000, message = "Reorder level cannot exceed 10,000")
    private int reorderLevel;

    @Min(value = 0, message = "Max stock level cannot be negative")
    @Max(value = 100000, message = "Max stock level cannot exceed 100,000")
    private int maxStockLevel;

    @Size(max = 20, message = "Unit of measure must not exceed 20 characters")
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