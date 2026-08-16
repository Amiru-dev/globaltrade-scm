package com.globaltrade.common.entity;

import com.globaltrade.common.enums.OrderStatus;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a purchase order placed with a vendor for goods or materials.
 * Manages the full order lifecycle from draft through payment.
 *
 * <p>Purchase order processing demonstrates transaction management concepts,
 * as order creation involves atomic updates across inventory, vendor, and order tables.</p>
 */
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_vendor", columnList = "vendor_id"),
        @Index(name = "idx_po_status", columnList = "status"),
        @Index(name = "idx_po_order_date", columnList = "order_date")
})
@NamedQueries({
        @NamedQuery(
                name = "PurchaseOrder.findByVendor",
                query = "SELECT po FROM PurchaseOrder po WHERE po.vendorId = :vendorId ORDER BY po.orderDate DESC"
        ),
        @NamedQuery(
                name = "PurchaseOrder.findByStatus",
                query = "SELECT po FROM PurchaseOrder po WHERE po.status = :status"
        ),
        @NamedQuery(
                name = "PurchaseOrder.findOverdue",
                query = "SELECT po FROM PurchaseOrder po WHERE po.expectedDelivery < :date " +
                        "AND po.status NOT IN (com.globaltrade.common.enums.OrderStatus.RECEIVED, " +
                        "com.globaltrade.common.enums.OrderStatus.CANCELLED)"
        ),
        @NamedQuery(
                name = "PurchaseOrder.findAll",
                query = "SELECT po FROM PurchaseOrder po ORDER BY po.orderDate DESC"
        )
})
public class PurchaseOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_number", unique = true, nullable = false, length = 30)
    private String orderNumber;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "item_description", nullable = false, length = 500)
    private String itemDescription;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery")
    private LocalDate expectedDelivery;

    @Column(name = "actual_delivery")
    private LocalDate actualDelivery;

    @Column(name = "destination_warehouse_id")
    private Long destinationWarehouseId;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OrderStatus.DRAFT;
        }
        if (this.orderDate == null) {
            this.orderDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Constructors ========================

    public PurchaseOrder() {
    }

    public PurchaseOrder(String orderNumber, Long vendorId, String itemDescription,
                         int quantity, BigDecimal unitPrice, String currency) {
        this.orderNumber = orderNumber;
        this.vendorId = vendorId;
        this.itemDescription = itemDescription;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.currency = currency;
        this.status = OrderStatus.DRAFT;
        this.orderDate = LocalDate.now();
    }

    // ======================== Getters and Setters ========================

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDelivery() {
        return expectedDelivery;
    }

    public void setExpectedDelivery(LocalDate expectedDelivery) {
        this.expectedDelivery = expectedDelivery;
    }

    public LocalDate getActualDelivery() {
        return actualDelivery;
    }

    public void setActualDelivery(LocalDate actualDelivery) {
        this.actualDelivery = actualDelivery;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "orderId=" + orderId +
                ", orderNumber='" + orderNumber + '\'' +
                ", vendorId=" + vendorId +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                '}';
    }
}
