package com.globaltrade.ejb.test;

import com.globaltrade.ejb.interceptor.PerformanceMonitor;

import jakarta.ejb.TimerConfig;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Performance benchmark tests for EJB components.
 *
 * <p><strong>Critical Analysis — Performance Testing in EJB:</strong></p>
 * <ul>
 *   <li><strong>Interceptor overhead:</strong> Measures the cost of the interceptor chain.
 *       Each interceptor adds latency; the PerformanceMonitor interceptor itself should
 *       have minimal overhead (~0.1ms) to avoid skewing measurements.</li>
 *   <li><strong>Timer scheduling latency:</strong> Verifies that timer configuration
 *       objects can be created within acceptable time bounds.</li>
 *   <li><strong>Exception creation cost:</strong> Stack trace capture during exception
 *       creation is expensive. Verifies that custom exceptions don't add unnecessary
 *       overhead beyond the JVM's native stack trace cost.</li>
 * </ul>
 *
 * <p><strong>Note:</strong> These are micro-benchmarks and should not be treated as
 * production performance indicators. For realistic benchmarks, use JMH or Payara's
 * built-in monitoring tools under load.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Performance Benchmark Tests")
class PerformanceBenchmarkTest {

    @Mock
    private InvocationContext invocationContext;

    @Nested
    @DisplayName("Interceptor Overhead Benchmarks")
    class InterceptorBenchmarks {

        @Test
        @DisplayName("PerformanceMonitor interceptor overhead should be under 50ms")
        void performanceMonitor_OverheadUnder50ms() throws Exception {
            PerformanceMonitor monitor = new PerformanceMonitor();
            when(invocationContext.proceed()).thenReturn("result");
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                monitor.monitorPerformance(invocationContext);
            }
            long elapsed = System.nanoTime() - start;
            double avgMs = (elapsed / 1_000_000.0) / 1000;

            assertTrue(avgMs < 50,
                    "Average interceptor overhead should be under 50ms, was: " + avgMs + "ms");
        }

        @RepeatedTest(5)
        @DisplayName("Single interceptor invocation should complete within 10ms")
        void singleInvocation_CompletesQuickly() throws Exception {
            PerformanceMonitor monitor = new PerformanceMonitor();
            when(invocationContext.proceed()).thenReturn("result");
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());

            long start = System.nanoTime();
            monitor.monitorPerformance(invocationContext);
            long elapsed = (System.nanoTime() - start) / 1_000_000;

            assertTrue(elapsed < 10,
                    "Single invocation should complete within 10ms, took: " + elapsed + "ms");
        }
    }

    @Nested
    @DisplayName("Timer Configuration Benchmarks")
    class TimerConfigBenchmarks {

        @Test
        @DisplayName("Timer configuration creation should be under 1ms per instance")
        void timerConfigCreation_IsEfficient() {
            long start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                TimerConfig config = new TimerConfig("Timer-" + i, false);
                assertNotNull(config.getInfo());
            }
            long elapsed = System.nanoTime() - start;
            double avgMicros = (elapsed / 1000.0) / 10000;

            assertTrue(avgMicros < 1000,
                    "TimerConfig creation should average under 1ms, was: " + avgMicros + "µs");
        }
    }

    @Nested
    @DisplayName("Exception Creation Benchmarks")
    class ExceptionBenchmarks {

        @Test
        @DisplayName("Custom exception creation should be efficient")
        void customExceptionCreation_IsEfficient() {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                new com.globaltrade.common.exception.ShipmentTrackingException(
                        "Tracking failed for TN-" + i, "TN-" + i);
            }
            long elapsed = System.nanoTime() - start;
            double avgMs = (elapsed / 1_000_000.0) / 1000;

            // Exception creation involves stack trace capture, so ~1ms is expected
            assertTrue(avgMs < 5,
                    "Exception creation should average under 5ms, was: " + avgMs + "ms");
        }

        @Test
        @DisplayName("Exception with cause chaining should not add significant overhead")
        void exceptionWithCause_MinimalOverhead() {
            Exception cause = new RuntimeException("root cause");

            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                com.globaltrade.common.exception.ShipmentTrackingException ex =
                        new com.globaltrade.common.exception.ShipmentTrackingException(
                                "Tracking failed", "TN-" + i);
                ex.initCause(cause);
            }
            long elapsed = System.nanoTime() - start;
            double avgMs = (elapsed / 1_000_000.0) / 1000;

            assertTrue(avgMs < 10,
                    "Exception with cause should average under 10ms, was: " + avgMs + "ms");
        }
    }

    @Nested
    @DisplayName("Entity Operation Benchmarks")
    class EntityBenchmarks {

        @Test
        @DisplayName("Entity creation should be lightweight")
        void entityCreation_IsLightweight() {
            long start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                com.globaltrade.common.entity.Shipment s = new com.globaltrade.common.entity.Shipment();
                s.setTrackingNumber("GT-" + i);
                s.setOriginCity("Shanghai");
                s.setOriginCountry("China");
                s.setDestinationCity("Rotterdam");
                s.setDestinationCountry("Netherlands");
            }
            long elapsed = System.nanoTime() - start;
            double avgMicros = (elapsed / 1000.0) / 10000;

            assertTrue(avgMicros < 1000,
                    "Entity creation should average under 1ms, was: " + avgMicros + "µs");
        }
    }
}
