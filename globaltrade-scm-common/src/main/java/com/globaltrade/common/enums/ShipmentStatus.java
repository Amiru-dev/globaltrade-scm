package com.globaltrade.common.enums;

/**
 * Represents the lifecycle status of a shipment within the supply chain.
 * Transitions follow a defined workflow: CREATED → PICKED_UP → IN_TRANSIT → ... → DELIVERED.
 */
public enum ShipmentStatus {

    CREATED("Shipment created, awaiting pickup"),
    PICKED_UP("Picked up by carrier"),
    IN_TRANSIT("In transit to destination"),
    AT_CUSTOMS("Held at customs for clearance"),
    CUSTOMS_CLEARED("Customs clearance completed"),
    OUT_FOR_DELIVERY("Out for final delivery"),
    DELIVERED("Successfully delivered"),
    DELAYED("Shipment delayed due to disruption"),
    RETURNED("Returned to origin"),
    CANCELLED("Shipment cancelled");

    private final String description;

    ShipmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
