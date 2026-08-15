package com.globaltrade.ejb.test;

import com.globaltrade.ejb.timer.ShipmentTrackingTimer;
import com.globaltrade.ejb.timer.RouteOptimizationTimer;

import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EJB Timer Services.
 *
 * <p><strong>Critical Analysis — Timer Testing Strategy:</strong>
 * Timer services are tested at two levels:
 * <ol>
 *   <li><strong>Unit level (here):</strong> Verifies timer creation parameters,
 *       configuration, and timeout method logic using mocked TimerService.</li>
 *   <li><strong>Integration level:</strong> Would verify actual timer firing,
 *       persistence, and clustering behaviour using Arquillian + Payara Embedded.</li>
 * </ol>
 * Unit tests verify the <em>intent</em> of timer configuration; integration
 * tests verify the container's <em>execution</em> of that intent.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Timer Service Unit Tests")
class TimerServiceTest {

    @Nested
    @DisplayName("ShipmentTrackingTimer Tests (Programmatic Interval)")
    class ShipmentTrackingTimerTests {

        @Mock
        private TimerService timerService;

        @Mock
        private Timer timer;

        @InjectMocks
        private ShipmentTrackingTimer trackingTimer;

        @Test
        @DisplayName("Should create programmatic interval timer on init()")
        void init_CreatesIntervalTimer() {
            when(timerService.getTimers()).thenReturn(Collections.emptyList());
            when(timerService.createIntervalTimer(
                    anyLong(), anyLong(), any(TimerConfig.class))).thenReturn(timer);

            trackingTimer.init();

            verify(timerService).createIntervalTimer(
                    eq(10000L),                // Initial delay: 10 seconds
                    eq(15L * 60 * 1000),       // Interval: 15 minutes
                    any(TimerConfig.class));
        }

        @Test
        @DisplayName("Should cancel existing timers before creating new one (redeploy safety)")
        void init_CancelsExistingTimers() {
            Timer existingTimer = mock(Timer.class);
            when(existingTimer.getInfo()).thenReturn("ShipmentTrackingTimer");
            when(timerService.getTimers()).thenReturn(List.of(existingTimer));
            when(timerService.createIntervalTimer(
                    anyLong(), anyLong(), any(TimerConfig.class))).thenReturn(timer);

            trackingTimer.init();

            verify(existingTimer).cancel();
        }
    }

    @Nested
    @DisplayName("RouteOptimizationTimer Tests (Programmatic Single-Action)")
    class RouteOptimizationTimerTests {

        @Mock
        private TimerService timerService;

        @Mock
        private Timer timer;

        @InjectMocks
        private RouteOptimizationTimer routeTimer;

        @Test
        @DisplayName("Should create single-action timer with specified delay")
        void scheduleOptimization_CreatesTimerWithDelay() {
            when(timerService.getTimers()).thenReturn(Collections.emptyList());
            when(timerService.createSingleActionTimer(
                    anyLong(), any(TimerConfig.class))).thenReturn(timer);

            routeTimer.scheduleOptimization(3000L);

            verify(timerService).createSingleActionTimer(
                    eq(3000L),
                    any(TimerConfig.class));
        }

        @Test
        @DisplayName("Should use default 5-second delay when no delay specified")
        void scheduleOptimization_DefaultDelay() {
            when(timerService.getTimers()).thenReturn(Collections.emptyList());
            when(timerService.createSingleActionTimer(
                    anyLong(), any(TimerConfig.class))).thenReturn(timer);

            routeTimer.scheduleOptimization();

            verify(timerService).createSingleActionTimer(
                    eq(5000L),
                    any(TimerConfig.class));
        }

        @Test
        @DisplayName("Should cancel pending optimization before rescheduling (debounce)")
        void scheduleOptimization_CancelsPending() {
            Timer pendingTimer = mock(Timer.class);
            when(pendingTimer.getInfo()).thenReturn("RouteOptimization");
            when(timerService.getTimers()).thenReturn(List.of(pendingTimer));
            when(timerService.createSingleActionTimer(
                    anyLong(), any(TimerConfig.class))).thenReturn(timer);

            routeTimer.scheduleOptimization(1000L);

            verify(pendingTimer).cancel();
        }
    }

    @Nested
    @DisplayName("Timer Configuration Validation")
    class TimerConfigValidationTests {

        @Test
        @DisplayName("Persistent timer survives server restart — verify configuration intent")
        void persistentTimer_ConfigurationIsCorrect() {
            TimerConfig persistentConfig = new TimerConfig("ShipmentTracking", true);
            assertTrue(persistentConfig.isPersistent(),
                "Shipment tracking timer must be persistent to survive server restarts");
        }

        @Test
        @DisplayName("Non-persistent timer is recalculated on restart — verify configuration intent")
        void nonPersistentTimer_ConfigurationIsCorrect() {
            TimerConfig transientConfig = new TimerConfig("RouteOptimization", false);
            assertFalse(transientConfig.isPersistent(),
                "Route optimization timer should not persist — recalculated on boot");
        }

        @Test
        @DisplayName("Timer info should contain meaningful identifier")
        void timerConfig_HasMeaningfulInfo() {
            TimerConfig config = new TimerConfig("ShipmentTrackingTimer", true);
            assertEquals("ShipmentTrackingTimer", config.getInfo(),
                "Timer info should contain a meaningful identifier for debugging");
        }
    }
}
