package com.globaltrade.ejb.test;

import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.enums.ShipmentStatus;
import com.globaltrade.common.exception.ShipmentTrackingException;
import com.globaltrade.ejb.service.ShipmentService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShipmentService session bean.
 *
 * <p>Tests verify CRUD operations, status transitions, exception handling,
 * and business rule enforcement. Uses Mockito to isolate the service
 * from the persistence layer.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentService Unit Tests")
class ShipmentServiceTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private ShipmentService shipmentService;

    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        testShipment = new Shipment();
        testShipment.setShipmentId(1L);
        testShipment.setTrackingNumber("GT-2024-001");
        testShipment.setOriginCity("Shanghai");
        testShipment.setOriginCountry("China");
        testShipment.setDestinationCity("Rotterdam");
        testShipment.setDestinationCountry("Netherlands");
        testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
        testShipment.setWeightKg(new BigDecimal("5000.0"));
        testShipment.setCustomsCleared(false);
    }

    @Nested
    @DisplayName("Create Shipment Tests")
    class CreateShipmentTests {

        @Test
        @DisplayName("Should persist and return new shipment")
        void createShipment_ValidData_PersistsAndReturns() {
            Shipment result = shipmentService.createShipment(testShipment);

            verify(em).persist(testShipment);
            assertNotNull(result);
            assertEquals("GT-2024-001", result.getTrackingNumber());
        }
    }

    @Nested
    @DisplayName("Update Shipment Status Tests")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should update status from IN_TRANSIT to DELIVERED")
        void updateStatus_ToDelivered_SetsDeliveryTimestamp() throws ShipmentTrackingException {
            when(em.find(Shipment.class, 1L)).thenReturn(testShipment);
            when(em.merge(any(Shipment.class))).thenReturn(testShipment);

            Shipment result = shipmentService.updateShipmentStatus(1L, ShipmentStatus.DELIVERED);

            assertEquals(ShipmentStatus.DELIVERED, result.getStatus());
            assertNotNull(result.getActualDelivery());
            verify(em).merge(testShipment);
        }

        @Test
        @DisplayName("Should set customs cleared flag when status is CUSTOMS_CLEARED")
        void updateStatus_ToCustomsCleared_SetsClearedFlag() throws ShipmentTrackingException {
            when(em.find(Shipment.class, 1L)).thenReturn(testShipment);
            when(em.merge(any(Shipment.class))).thenReturn(testShipment);

            Shipment result = shipmentService.updateShipmentStatus(1L, ShipmentStatus.CUSTOMS_CLEARED);

            assertTrue(result.isCustomsCleared());
        }

        @Test
        @DisplayName("Should throw ShipmentTrackingException for non-existent shipment")
        void updateStatus_NonExistent_ThrowsException() {
            when(em.find(Shipment.class, 999L)).thenReturn(null);

            ShipmentTrackingException ex = assertThrows(
                    ShipmentTrackingException.class,
                    () -> shipmentService.updateShipmentStatus(999L, ShipmentStatus.DELIVERED)
            );

            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    @Nested
    @DisplayName("Find Shipment Tests")
    class FindShipmentTests {

        @Test
        @DisplayName("Should find shipment by ID")
        void findById_ExistingId_ReturnsShipment() {
            when(em.find(Shipment.class, 1L)).thenReturn(testShipment);

            Shipment result = shipmentService.findById(1L);

            assertNotNull(result);
            assertEquals("GT-2024-001", result.getTrackingNumber());
        }

        @Test
        @DisplayName("Should return null for non-existent ID")
        void findById_NonExistentId_ReturnsNull() {
            when(em.find(Shipment.class, 999L)).thenReturn(null);

            Shipment result = shipmentService.findById(999L);

            assertNull(result);
        }

        @Test
        @DisplayName("Should find shipment by tracking number")
        @SuppressWarnings("unchecked")
        void findByTrackingNumber_Existing_ReturnsShipment() throws ShipmentTrackingException {
            TypedQuery<Shipment> mockQuery = mock(TypedQuery.class);
            when(em.createQuery(anyString(), eq(Shipment.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getResultList()).thenReturn(List.of(testShipment));

            Shipment result = shipmentService.findByTrackingNumber("GT-2024-001");

            assertNotNull(result);
            assertEquals("GT-2024-001", result.getTrackingNumber());
        }

        @Test
        @DisplayName("Should throw exception when tracking number not found")
        @SuppressWarnings("unchecked")
        void findByTrackingNumber_NotFound_ThrowsException() {
            TypedQuery<Shipment> mockQuery = mock(TypedQuery.class);
            when(em.createQuery(anyString(), eq(Shipment.class))).thenReturn(mockQuery);
            when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
            when(mockQuery.getResultList()).thenReturn(Collections.emptyList());

            assertThrows(ShipmentTrackingException.class,
                    () -> shipmentService.findByTrackingNumber("INVALID-TN"));
        }
    }

    @Nested
    @DisplayName("Delete Shipment Tests")
    class DeleteShipmentTests {

        @Test
        @DisplayName("Should delete CANCELLED shipment")
        void deleteShipment_CancelledStatus_Deletes() throws ShipmentTrackingException {
            testShipment.setStatus(ShipmentStatus.CANCELLED);
            when(em.find(Shipment.class, 1L)).thenReturn(testShipment);

            shipmentService.deleteShipment(1L);

            verify(em).remove(testShipment);
        }

        @Test
        @DisplayName("Should reject deletion of non-cancelled shipment")
        void deleteShipment_ActiveStatus_ThrowsException() {
            testShipment.setStatus(ShipmentStatus.IN_TRANSIT);
            when(em.find(Shipment.class, 1L)).thenReturn(testShipment);

            ShipmentTrackingException ex = assertThrows(
                    ShipmentTrackingException.class,
                    () -> shipmentService.deleteShipment(1L)
            );

            assertTrue(ex.getMessage().contains("CANCELLED"));
            verify(em, never()).remove(any());
        }
    }
}
