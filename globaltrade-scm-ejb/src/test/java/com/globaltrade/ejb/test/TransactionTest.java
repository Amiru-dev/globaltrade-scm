package com.globaltrade.ejb.test;

import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.PurchaseOrder;
import com.globaltrade.common.exception.InventoryShortageException;
import com.globaltrade.common.exception.TransactionRollbackException;
import com.globaltrade.ejb.service.InventoryService;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for transaction management behaviour.
 *
 * <p><strong>Critical Analysis — Transaction Testing:</strong>
 * These unit tests verify the <em>business logic</em> that determines when
 * transactions should succeed or fail. The actual transaction demarcation
 * (REQUIRED, REQUIRES_NEW, etc.) is managed by the EJB container and
 * would be verified via integration tests with Arquillian.</p>
 *
 * <p>Key scenarios tested:</p>
 * <ul>
 *   <li>Atomic inventory transfers (debit + credit must both succeed or both fail)</li>
 *   <li>Exception types that trigger rollback vs. those that don't</li>
 *   <li>Data integrity validation before persistence</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Management Tests")
class TransactionTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem sourceItem;
    private InventoryItem targetItem;

    @BeforeEach
    void setUp() {
        sourceItem = new InventoryItem();
        sourceItem.setItemId(1L);
        sourceItem.setSku("WH-ELEC-001");
        sourceItem.setQuantity(100);
        sourceItem.setWarehouseId(1L);

        targetItem = new InventoryItem();
        targetItem.setItemId(2L);
        targetItem.setSku("WH-ELEC-002");
        targetItem.setQuantity(20);
        targetItem.setWarehouseId(2L);
    }

    @Nested
    @DisplayName("Atomic Inventory Transfer Tests")
    class AtomicTransferTests {

        @Test
        @DisplayName("Should atomically debit source and credit target")
        void transferInventory_ValidTransfer_UpdatesBothItems()
                throws InventoryShortageException, TransactionRollbackException {

            when(em.find(InventoryItem.class, 1L)).thenReturn(sourceItem);
            when(em.find(InventoryItem.class, 2L)).thenReturn(targetItem);

            inventoryService.transferInventory(1L, 2L, 30);

            // Verify both source and target were updated
            assertEquals(70, sourceItem.getQuantity(), "Source should be debited");
            assertEquals(50, targetItem.getQuantity(), "Target should be credited");
            verify(em).merge(sourceItem);
            verify(em).merge(targetItem);
        }

        @Test
        @DisplayName("Should throw InventoryShortageException when insufficient stock")
        void transferInventory_InsufficientStock_ThrowsShortageException() {
            when(em.find(InventoryItem.class, 1L)).thenReturn(sourceItem);
            when(em.find(InventoryItem.class, 2L)).thenReturn(targetItem);

            // Request more than available (100 units)
            assertThrows(InventoryShortageException.class,
                () -> inventoryService.transferInventory(1L, 2L, 150));

            // Verify neither item was modified (no em.merge called)
            verify(em, never()).merge(any());
        }

        @Test
        @DisplayName("Should throw TransactionRollbackException when item not found")
        void transferInventory_ItemNotFound_ThrowsRollbackException() {
            when(em.find(InventoryItem.class, 1L)).thenReturn(sourceItem);
            when(em.find(InventoryItem.class, 999L)).thenReturn(null);

            assertThrows(TransactionRollbackException.class,
                () -> inventoryService.transferInventory(1L, 999L, 10));
        }
    }

    @Nested
    @DisplayName("Stock Deduction Transaction Tests")
    class StockDeductionTests {

        @Test
        @DisplayName("Should deduct stock successfully when sufficient quantity exists")
        void deductStock_SufficientQuantity_DeductsAndReturns()
                throws InventoryShortageException {

            when(em.find(InventoryItem.class, 1L)).thenReturn(sourceItem);
            when(em.merge(any(InventoryItem.class))).thenReturn(sourceItem);

            InventoryItem result = inventoryService.deductStock(1L, 30);

            assertEquals(70, result.getQuantity());
            verify(em).merge(sourceItem);
        }

        @Test
        @DisplayName("InventoryShortageException should NOT trigger rollback (rollback=false)")
        void deductStock_InsufficientStock_ThrowsNonRollbackException() {
            when(em.find(InventoryItem.class, 1L)).thenReturn(sourceItem);

            InventoryShortageException ex = assertThrows(
                InventoryShortageException.class,
                () -> inventoryService.deductStock(1L, 200)
            );

            // Critical Analysis: rollback=false on InventoryShortageException
            // means the transaction is NOT automatically rolled back.
            // The caller can decide to proceed with partial fulfilment.
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("Insufficient stock"));
        }

        @Test
        @DisplayName("Should throw InventoryShortageException for non-existent item")
        void deductStock_ItemNotFound_ThrowsShortageException() {
            when(em.find(InventoryItem.class, 999L)).thenReturn(null);

            assertThrows(InventoryShortageException.class,
                () -> inventoryService.deductStock(999L, 10));
        }
    }

    @Nested
    @DisplayName("Replenishment Transaction Tests")
    class ReplenishmentTests {

        @Test
        @DisplayName("Should replenish stock and update lastReplenished timestamp")
        void replenishStock_ValidItem_IncreasesQuantity() {
            when(em.find(InventoryItem.class, 1L)).thenReturn(sourceItem);

            InventoryItem result = inventoryService.replenishStock(1L, 50);

            assertEquals(150, result.getQuantity());
            assertNotNull(result.getLastReplenished());
            verify(em).merge(sourceItem);
        }

        @Test
        @DisplayName("Should return null for non-existent item")
        void replenishStock_ItemNotFound_ReturnsNull() {
            when(em.find(InventoryItem.class, 999L)).thenReturn(null);

            InventoryItem result = inventoryService.replenishStock(999L, 50);

            assertNull(result);
            verify(em, never()).merge(any());
        }
    }
}
