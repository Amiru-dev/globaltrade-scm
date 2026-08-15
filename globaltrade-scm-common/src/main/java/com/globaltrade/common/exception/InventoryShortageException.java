package com.globaltrade.common.exception;

import jakarta.ejb.ApplicationException;

/**
 * Thrown when inventory stock falls below the reorder threshold
 * or when a stock deduction would result in negative inventory.
 *
 * <p>{@code rollback = false}: Inventory shortages are business-level events.
 * Partial fulfilment is an acceptable outcome — the order can proceed with
 * a reduced quantity, and a replenishment order can be triggered separately.
 * Rolling back would undo the entire operation unnecessarily.</p>
 */
@ApplicationException(rollback = false)
public class InventoryShortageException extends SupplyChainException {

    private final String sku;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InventoryShortageException(String message, String sku,
                                      int requestedQuantity, int availableQuantity) {
        super(message, "SCM-INV-001", "InventoryItem", sku);
        this.sku = sku;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getSku() { return sku; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
}
