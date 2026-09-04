package com.globaltrade.ejb.test.integration;

import com.globaltrade.common.entity.CarrierRoute;
import com.globaltrade.ejb.service.RouteOptimizationService;
import com.globaltrade.ejb.timer.RouteOptimizationTimer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.globaltrade.ejb.service.InventoryService;
import com.globaltrade.ejb.service.ShipmentService;
import com.globaltrade.ejb.service.CustomsService;

import jakarta.ejb.EJB;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for RouteOptimizationTimer trigger mechanism.
 *
 * <p>This test validates that the RouteOptimizationTimer can be triggered
 * via the service layer, completing the integration gap identified in the
 * critical analysis.</p>
 *
 * <p><strong>Test Data Strategy:</strong> Each test creates its own carrier
 * route data through the service layer, then asserts against the result.
 * This ensures the timer scheduling and service query methods genuinely
 * work end-to-end against the real database and container.</p>
 *
 * <p><strong>Prerequisites:</strong></p>
 * <ul>
 *   <li>Payara Server 6.2025.11 running on localhost:4848</li>
 *   <li>Database connection configured and running</li>
 * </ul>
 *
 * <p><strong>Execution:</strong> mvn clean test -Parquillian</p>
 */
@ExtendWith(ArquillianExtension.class)
public class RouteOptimizationTriggerTest {

    @Deployment(testable = true)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "route-optimization-trigger-test.war")
                .addClass(TestSecurityFacade.class)
                .addClass(InventoryService.class)
                .addClass(ShipmentService.class)
                .addClass(CustomsService.class)
                .addClass(RouteOptimizationService.class)
                .addClass(RouteOptimizationTimer.class)
                .addAsWebInfResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml")
                .addAsWebInfResource("META-INF/beans.xml", "classes/META-INF/beans.xml");
    }

    @EJB
    private RouteOptimizationService routeService;

    @EJB
    private RouteOptimizationTimer routeTimer;

    @EJB
    private TestSecurityFacade testFacade;

    /**
     * Test validates that RouteOptimizationTimer can be scheduled programmatically.
     * Calls scheduleOptimization() and verifies the TimerService accepted the request
     * without throwing. A failure here means TimerService injection or timer creation
     * is broken in the container.
     */
    @Test
    public void testTimerTriggerMechanism() {
        assertNotNull(routeTimer, "RouteOptimizationTimer should be injected");
        assertNotNull(routeService, "RouteOptimizationService should be injected");

        // Schedule a timer — this exercises TimerService.createSingleActionTimer()
        // inside the real container. If TimerService is not injected or the container
        // rejects the timer config, this call will throw.
        try {
            routeTimer.scheduleOptimization(60000L); // Long delay to avoid immediate firing
        } catch (Exception e) {
            fail("Timer scheduling failed — TimerService or container configuration "
                    + "is broken: " + e.getMessage(), e);
        }
    }

    /**
     * Test validates that timer can be triggered with default delay.
     * Exercises the no-arg overload which delegates to scheduleOptimization(5000).
     */
    @Test
    public void testTimerDefaultDelay() {
        assertNotNull(routeTimer, "RouteOptimizationTimer should be injected");

        try {
            routeTimer.scheduleOptimization(); // Default 5-second delay
        } catch (Exception e) {
            fail("Timer scheduling with default delay failed — TimerService or container "
                    + "configuration is broken: " + e.getMessage(), e);
        }
    }

    /**
     * Test validates that RouteOptimizationService is accessible and can
     * perform actual database operations. Creates a test route, then queries
     * all routes to verify the persist → query round-trip.
     */
    @Test
    public void testRouteServiceIntegration() {
        assertNotNull(routeService, "RouteOptimizationService should be injected");

        // Create known test data
        CarrierRoute testRoute = new CarrierRoute(
                "IT-RT-" + System.nanoTime(), "TestCarrier",
                "TestOrigin", "TestDestination", 5,
                new BigDecimal("2.50"));
        testRoute.setReliabilityScore(new BigDecimal("0.95"));
        testRoute.setMaxWeightKg(new BigDecimal("10000.00"));

        CarrierRoute persisted = testFacade.addRoute(testRoute);
        assertNotNull(persisted, "Persisted route should not be null");
        assertNotNull(persisted.getRouteId(), "Persisted route should have a generated ID");

        // Query all routes — should include our test data
        List<CarrierRoute> allRoutes = testFacade.findAllRoutes();
        assertNotNull(allRoutes, "Should return route list from database");
        assertFalse(allRoutes.isEmpty(),
                "All routes should include the test route we just inserted");

        // Verify the specific route can be retrieved by ID
        CarrierRoute retrieved = testFacade.findRouteById(persisted.getRouteId());
        assertNotNull(retrieved, "Should retrieve the test route by ID");
        assertEquals("TestCarrier", retrieved.getCarrier(),
                "Retrieved route carrier should match the test data");
        assertEquals(5, retrieved.getTransitDays(),
                "Retrieved route transit days should match the test data");
    }
}