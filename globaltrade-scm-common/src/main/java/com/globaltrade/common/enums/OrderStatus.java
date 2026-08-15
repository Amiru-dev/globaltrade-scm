package com.globaltrade.common.enums;

/**
 * Represents the lifecycle status of a purchase order placed with a vendor.
 */
public enum OrderStatus {

    DRAFT("Order drafted, not yet submitted"),
    SUBMITTED("Order submitted to vendor"),
    CONFIRMED("Vendor confirmed order"),
    PROCESSING("Order is being processed/manufactured"),
    SHIPPED("Order shipped by vendor"),
    PARTIALLY_RECEIVED("Partial delivery received"),
    RECEIVED("Full order received at warehouse"),
    INVOICED("Invoice received from vendor"),
    PAID("Payment completed"),
    CANCELLED("Order cancelled"),
    DISPUTED("Order under dispute");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
