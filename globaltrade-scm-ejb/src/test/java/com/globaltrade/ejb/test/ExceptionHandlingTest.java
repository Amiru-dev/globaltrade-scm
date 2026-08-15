package com.globaltrade.ejb.test;

import com.globaltrade.common.exception.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the custom exception hierarchy.
 *
 * <p><strong>Critical Analysis — Exception Hierarchy Design:</strong></p>
 * <ul>
 *   <li><strong>Application Exceptions</strong> ({@code @ApplicationException}):
 *       Preserve the transaction state and allow the client to handle the error
 *       (retry, partial fulfilment, etc.).</li>
 *   <li><strong>System Exceptions</strong> ({@code extends EJBException}):
 *       Cause automatic transaction rollback and bean instance discard.
 *       Used for unrecoverable infrastructure failures.</li>
 *   <li><strong>rollback=true vs rollback=false</strong>: Customs compliance
 *       violations MUST rollback (data integrity). Inventory shortages should
 *       NOT rollback (partial fulfilment is an acceptable business outcome).</li>
 * </ul>
 */
@DisplayName("Exception Handling Tests")
class ExceptionHandlingTest {

    @Nested
    @DisplayName("Application Exception Tests")
    class ApplicationExceptionTests {

        @Test
        @DisplayName("ShipmentTrackingException should carry tracking context")
        void shipmentTrackingException_CarriesTrackingInfo() {
            ShipmentTrackingException ex = new ShipmentTrackingException(
                "Carrier API timeout for tracking GT-2024-001",
                "GT-2024-001");

            assertEquals("Carrier API timeout for tracking GT-2024-001", ex.getMessage());
            assertEquals("GT-2024-001", ex.getTrackingNumber());
        }

        @Test
        @DisplayName("InventoryShortageException should carry shortage details")
        void inventoryShortageException_CarriesShortageDetails() {
            InventoryShortageException ex = new InventoryShortageException(
                "Insufficient stock for SKU WH-ELEC-001",
                "WH-ELEC-001", 100, 30);

            assertAll(
                () -> assertEquals("WH-ELEC-001", ex.getSku()),
                () -> assertEquals(100, ex.getRequestedQuantity()),
                () -> assertEquals(30, ex.getAvailableQuantity()),
                () -> assertTrue(ex.getMessage().contains("Insufficient stock"))
            );
        }

        @Test
        @DisplayName("CustomsComplianceException should carry compliance details")
        void customsComplianceException_CarriesComplianceInfo() {
            CustomsComplianceException ex = new CustomsComplianceException(
                "Missing HS code for declaration DEC-2024-099",
                "DEC-2024-099",
                "MISSING_HS_CODE");

            assertEquals("DEC-2024-099", ex.getDeclarationId());
            assertEquals("MISSING_HS_CODE", ex.getViolationType());
        }

        @Test
        @DisplayName("VendorValidationException should carry validation context")
        void vendorValidationException_CarriesValidationContext() {
            VendorValidationException ex = new VendorValidationException(
                "Invalid vendor email format",
                "VND-001", "contactEmail");

            assertEquals("VND-001", ex.getVendorCode());
            assertEquals("contactEmail", ex.getFieldName());
        }

        @Test
        @DisplayName("TransactionRollbackException should carry transaction context")
        void transactionRollbackException_CarriesTxContext() {
            TransactionRollbackException ex = new TransactionRollbackException(
                "Data integrity violation during inventory transfer",
                "TRANSFER-001-002");

            assertEquals("TRANSFER-001-002", ex.getTransactionId());
        }
    }

    @Nested
    @DisplayName("Exception Hierarchy Tests")
    class ExceptionHierarchyTests {

        @Test
        @DisplayName("All application exceptions should extend SupplyChainException")
        void applicationExceptions_ExtendBase() {
            assertAll(
                () -> assertInstanceOf(SupplyChainException.class,
                    new ShipmentTrackingException("test", "TN-001")),
                () -> assertInstanceOf(SupplyChainException.class,
                    new InventoryShortageException("test", "SKU-001", 10, 5)),
                () -> assertInstanceOf(SupplyChainException.class,
                    new CustomsComplianceException("test", "DEC-001", "TYPE")),
                () -> assertInstanceOf(SupplyChainException.class,
                    new VendorValidationException("test", "VND-001", "field")),
                () -> assertInstanceOf(SupplyChainException.class,
                    new TransactionRollbackException("test", "TX-001"))
            );
        }

        @Test
        @DisplayName("CarrierSystemException should be a RuntimeException (system exception)")
        void carrierSystemException_IsRuntimeException() {
            CarrierSystemException ex = new CarrierSystemException(
                "Carrier API completely unavailable");

            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("SupplyChainException should be a checked exception")
        void supplyChainException_IsCheckedException() {
            // SupplyChainException extends Exception (checked), not RuntimeException
            ShipmentTrackingException ex = new ShipmentTrackingException("test", "TN-001");
            assertInstanceOf(Exception.class, ex);
            assertFalse(ex instanceof RuntimeException,
                "Application exceptions should be checked exceptions");
        }
    }

    @Nested
    @DisplayName("Exception Chaining Tests")
    class ExceptionChainingTests {

        @Test
        @DisplayName("Should preserve root cause through exception chain")
        void exceptionChaining_PreservesRootCause() {
            Exception rootCause = new java.sql.SQLException("Connection refused");
            ShipmentTrackingException ex = new ShipmentTrackingException(
                "Failed to query carrier for GT-2024-001",
                "GT-2024-001");
            ex.initCause(rootCause);

            assertNotNull(ex.getCause());
            assertInstanceOf(java.sql.SQLException.class, ex.getCause());
            assertEquals("Connection refused", ex.getCause().getMessage());
        }
    }
}
