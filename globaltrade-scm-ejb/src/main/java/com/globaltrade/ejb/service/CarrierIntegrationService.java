package com.globaltrade.ejb.service;

import jakarta.ejb.Stateless;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrates remote vs local interface selection for EJB components.
 *
 * <p><strong>Critical Analysis - Interface Selection Criteria:</strong></p>
 * <ul>
 *   <li><strong>Local Interface (@Local):</strong>
 *       <ul>
 *         <li>Calls are within the same JVM (no network overhead)</li>
 *         <li>Parameters passed by reference (no serialization required)</li>
 *         <li>Typical performance: ~0.1-0.5ms per method call</li>
 *         <li>Use for: Same-application modules, web tier to EJB tier communication</li>
 *       </ul>
 *   </li>
 *   <li><strong>Remote Interface (@Remote):</strong>
 *       <ul>
 *         <li>Calls can be across JVM boundaries (network overhead)</li>
 *         <li>Parameters passed by value (serialization required)</li>
 *         <li>Typical performance: ~2-10ms per method call (plus network latency)</li>
 *         <li>Use for: Cross-application integration, external system access, clustering</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Performance Impact Analysis:</strong></p>
 * <ul>
 *   <li>Serialization overhead: ~5-15ms for complex objects</li>
 *   <li>Network latency: ~1-50ms depending on infrastructure</li>
 *   <li>Marshalling/unmarshalling: ~2-5ms per call</li>
 *   <li>Total remote call overhead: ~10-70ms vs ~0.1-0.5ms for local</li>
 * </ul>
 *
 * <p><strong>Serialization Considerations:</strong></p>
 * <ul>
 *   <li>All parameters and return values must be Serializable</li>
 *   <li>Avoid passing large objects through remote interfaces</li>
 *   <li>Consider DTO pattern to minimize serialization overhead</li>
 *   <li>Remote exceptions must be Serializable or RuntimeException</li>
 * </ul>
 */
@Stateless
public class CarrierIntegrationService implements CarrierIntegrationServiceLocal, CarrierIntegrationServiceRemote {

    private static final Logger LOGGER = Logger.getLogger(CarrierIntegrationService.class.getName());

    // ======================== Local Interface Implementation ========================

    @Override
    public String getCarrierInfoLocal(String carrierCode) {
        LOGGER.info("LOCAL: Carrier info request for " + carrierCode + " (same JVM)");
        return "Carrier Info: " + carrierCode + " (Local Call - No Serialization)";
    }

    @Override
    public CarrierQuote getQuoteLocal(String origin, String destination, double weight) {
        LOGGER.info("LOCAL: Quote request " + origin + " to " + destination + " (same JVM)");
        return new CarrierQuote(origin, destination, weight, 100.0, "Local Call");
    }

    // ======================== Remote Interface Implementation ========================

    @Override
    public String getCarrierInfoRemote(String carrierCode) {
        LOGGER.info("REMOTE: Carrier info request for " + carrierCode + " (potentially different JVM)");
        return "Carrier Info: " + carrierCode + " (Remote Call - Serialization Required)";
    }

    @Override
    public CarrierQuote getQuoteRemote(String origin, String destination, double weight) {
        LOGGER.info("REMOTE: Quote request " + origin + " to " + destination + " (potentially different JVM)");
        return new CarrierQuote(origin, destination, weight, 100.0, "Remote Call");
    }

    // ======================== Performance Comparison Methods ========================

    /**
     * Demonstrates performance difference between local and remote calls.
     */
    public void performPerformanceComparison() {
        LOGGER.info("PERFORMANCE: Starting local vs remote call comparison");

        // Local call performance test
        long localStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            getCarrierInfoLocal("TEST-CARRIER");
        }
        long localEnd = System.nanoTime();
        double localAvgMs = (localEnd - localStart) / 1_000_000.0 / 1000;

        // Remote call performance test (simulated)
        long remoteStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            getCarrierInfoRemote("TEST-CARRIER");
        }
        long remoteEnd = System.nanoTime();
        double remoteAvgMs = (remoteEnd - remoteStart) / 1_000_000.0 / 1000;

        LOGGER.info("PERFORMANCE: Local call average: " + localAvgMs + "ms");
        LOGGER.info("PERFORMANCE: Remote call average: " + remoteAvgMs + "ms");
        LOGGER.info("PERFORMANCE: Remote overhead: " + (remoteAvgMs / localAvgMs) + "x slower");
    }

    /**
     * Demonstrates serialization overhead for complex objects.
     */
    public void demonstrateSerializationOverhead() {
        LOGGER.info("SERIALIZATION: Testing serialization overhead for complex objects");

        CarrierQuote complexQuote = new CarrierQuote("ORIGIN", "DESTINATION", 1000.0, 500.0, "Test");

        long serializeStart = System.nanoTime();
        // Simulate serialization (in real scenario, object would be serialized)
        for (int i = 0; i < 100; i++) {
            // Simulated serialization overhead
            byte[] simulatedData = complexQuote.toString().getBytes();
        }
        long serializeEnd = System.nanoTime();
        double serializeAvgMs = (serializeEnd - serializeStart) / 1_000_000.0 / 100;

        LOGGER.info("SERIALIZATION: Average serialization time: " + serializeAvgMs + "ms");
        LOGGER.info("SERIALIZATION: This overhead is avoided in local calls");
    }
}

