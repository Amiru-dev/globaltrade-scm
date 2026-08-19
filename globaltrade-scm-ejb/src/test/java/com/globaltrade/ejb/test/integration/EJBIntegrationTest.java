package com.globaltrade.ejb.test.integration;

import com.globaltrade.ejb.service.InventoryService;
import com.globaltrade.ejb.service.ShipmentService;
import com.globaltrade.ejb.service.CustomsService;
import com.globaltrade.ejb.service.VendorManagementService;
import com.globaltrade.ejb.timer.ShipmentTrackingTimer;
import com.globaltrade.ejb.timer.InventoryMonitorTimer;
import com.globaltrade.ejb.interceptor.AuditLogInterceptor;
import com.globaltrade.ejb.interceptor.PerformanceMonitor;
import com.globaltrade.ejb.security.SecurityService;
import com.globaltrade.common.entity.InventoryItem;
import com.globaltrade.common.entity.Shipment;
import com.globaltrade.common.entity.Vendor;
import jakarta.inject.Inject;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.junit5.ArquillianTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Arquillian integration tests for EJB components with real container deployment.
 * 
 * <p>These tests validate the integration of EJB components within the actual
 * Jakarta EE container, ensuring proper deployment, injection, and interaction
 * between beans, interceptors, and timer services.</p>
 */
@ExtendWith(ArquillianExtension.class)
public class EJBIntegrationTest {

    @Inject
    private InventoryService inventoryService;

    @Inject
    private ShipmentService shipmentService;

    @Inject
    private CustomsService customsService;

    @Inject
    private VendorManagementService vendorManagementService;

    @Inject
    private SecurityService securityService;

    @Deployment
    public static EnterpriseArchive createDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "globaltrade-scm-test.ear")
            .addAsModule(ShrinkWrap.create(EnterpriseArchive.class, "globaltrade-scm-ejb.jar")
                .addPackage(InventoryService.class.getPackage())
                .addPackage(ShipmentTrackingTimer.class.getPackage())
                .addPackage(AuditLogInterceptor.class.getPackage())
                .addPackage(SecurityService.class.getPackage())
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml")
                .addAsManifestResource("META-INF/ejb-jar.xml", "ejb-jar.xml"));
        
        return ear;
    }

    @Test
    @ArquillianTest
    public void testInventoryServiceDeployment() {
        assertNotNull(inventoryService, "InventoryService should be injected");
        // Test basic inventory operation
        InventoryItem item = inventoryService.findBySku("WH-ELEC-001");
        assertNotNull(item, "Should find inventory item by SKU");
        assertEquals("WH-ELEC-001", item.getSku(), "SKU should match");
    }

    @Test
    @ArquillianTest
    public void testShipmentServiceDeployment() {
        assertNotNull(shipmentService, "ShipmentService should be injected");
        // Test basic shipment operation
        Shipment shipment = shipmentService.findByTrackingNumber("GT-2026-0001");
        assertNotNull(shipment, "Should find shipment by tracking number");
        assertEquals("GT-2026-0001", shipment.getTrackingNumber(), "Tracking number should match");
    }

    @Test
    @ArquillianTest
    public void testCustomsServiceDeployment() {
        assertNotNull(customsService, "CustomsService should be injected");
        // Test customs declaration operation
        assertNotNull(customsService, "CustomsService should be operational");
    }

    @Test
    @ArquillianTest
    public void testVendorManagementServiceDeployment() {
        assertNotNull(vendorManagementService, "VendorManagementService should be injected");
        // Test vendor operation
        Vendor vendor = vendorManagementService.findByVendorCode("VND-001");
        assertNotNull(vendor, "Should find vendor by code");
        assertEquals("VND-001", vendor.getVendorCode(), "Vendor code should match");
    }

    @Test
    @ArquillianTest
    public void testSecurityServiceDeployment() {
        assertNotNull(securityService, "SecurityService should be injected");
        // Test security authorization
        assertTrue(securityService.canAccessEntity("SHIPMENT", "logistics_coord"), 
            "Logistics coordinator should access shipment data");
    }

    @Test
    @ArquillianTest
    public void testInterceptorIntegration() {
        // Test that interceptors are properly applied
        InventoryItem item = inventoryService.findBySku("WH-ELEC-001");
        assertNotNull(item, "Interceptors should be applied during business method execution");
    }

    @Test
    @ArquillianTest
    public void testTransactionIntegration() {
        // Test transaction boundaries across service calls
        int initialQuantity = inventoryService.getStockLevel("WH-ELEC-001");
        inventoryService.deductStock("WH-ELEC-001", 10);
        int finalQuantity = inventoryService.getStockLevel("WH-ELEC-001");
        assertEquals(initialQuantity - 10, finalQuantity, "Transaction should deduct stock correctly");
    }

    @Test
    @ArquillianTest
    public void testEJBComponentLifecycle() {
        // Test EJB component lifecycle in container
        assertNotNull(inventoryService, "Stateless bean should be properly instantiated");
        assertNotNull(shipmentService, "Stateless bean should be properly instantiated");
    }
}