package com.globaltrade.ejb.test;

import com.globaltrade.ejb.interceptor.AuditLogInterceptor;
import com.globaltrade.ejb.interceptor.PerformanceMonitor;
import com.globaltrade.ejb.service.AuditService;

import jakarta.ejb.SessionContext;
import jakarta.interceptor.InvocationContext;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the interceptor framework.
 *
 * <p><strong>Critical Analysis — Interceptor Testing:</strong>
 * Interceptors are tested by mocking the {@link InvocationContext} to verify:
 * <ul>
 *   <li>The interceptor correctly invokes {@code ctx.proceed()} to continue the chain</li>
 *   <li>Cross-cutting logic executes before and/or after the target method</li>
 *   <li>Exception propagation through the interceptor chain</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Interceptor Framework Unit Tests")
class InterceptorTest {

    @Mock
    private InvocationContext invocationContext;

    @Mock
    private AuditService auditService;

    @Mock
    private SessionContext sessionContext;

    @Nested
    @DisplayName("PerformanceMonitor Interceptor Tests")
    class PerformanceMonitorTests {

        private PerformanceMonitor performanceMonitor;

        @BeforeEach
        void setUp() {
            performanceMonitor = new PerformanceMonitor();
        }

        @Test
        @DisplayName("Should proceed with invocation and return result")
        void monitor_ValidInvocation_ProceedsAndReturns() throws Exception {
            Object expectedResult = "success";
            when(invocationContext.proceed()).thenReturn(expectedResult);
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());

            Object result = performanceMonitor.monitorPerformance(invocationContext);

            assertEquals(expectedResult, result);
            verify(invocationContext).proceed();
        }

        @Test
        @DisplayName("Should propagate exceptions from target method")
        void monitor_TargetThrowsException_PropagatesException() throws Exception {
            when(invocationContext.proceed()).thenThrow(new RuntimeException("Service error"));
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());

            assertThrows(RuntimeException.class,
                    () -> performanceMonitor.monitorPerformance(invocationContext));
        }
    }

    @Nested
    @DisplayName("AuditLogInterceptor Tests")
    class AuditLogInterceptorTests {

        private AuditLogInterceptor auditInterceptor;

        @BeforeEach
        void setUp() {
            auditInterceptor = new AuditLogInterceptor();
            auditInterceptor.setAuditService(auditService);
            auditInterceptor.setSessionContext(sessionContext);

            // Mock the audit service to do nothing when persistAuditLog is called
            doNothing().when(auditService).persistAuditLog(any());

            // Mock the session context to return a principal
            Principal mockPrincipal = mock(Principal.class);
            when(mockPrincipal.getName()).thenReturn("test-user");
            when(sessionContext.getCallerPrincipal()).thenReturn(mockPrincipal);
        }

        @Test
        @DisplayName("Should proceed with invocation for audit logging")
        void audit_ValidInvocation_ProceedsAndLogs() throws Exception {
            Object expectedResult = "audited-result";
            when(invocationContext.proceed()).thenReturn(expectedResult);
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());
            when(invocationContext.getParameters()).thenReturn(new Object[]{});

            Object result = auditInterceptor.audit(invocationContext);

            assertEquals(expectedResult, result);
            verify(invocationContext).proceed();
        }

        @Test
        @DisplayName("Should capture method parameters for audit trail")
        void audit_WithParameters_CapturesParameters() throws Exception {
            Object[] params = new Object[]{"param1", 42};
            when(invocationContext.proceed()).thenReturn("result");
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());
            when(invocationContext.getParameters()).thenReturn(params);

            auditInterceptor.audit(invocationContext);

            verify(invocationContext, times(2)).getParameters();
        }
    }

    @Nested
    @DisplayName("Interceptor Chain Behaviour Tests")
    class InterceptorChainTests {

        @BeforeEach
        void setUp() {
            // Mock the audit service to do nothing when persistAuditLog is called
            doNothing().when(auditService).persistAuditLog(any());

            // Mock the session context to return a principal
            Principal mockPrincipal = mock(Principal.class);
            when(mockPrincipal.getName()).thenReturn("test-user");
            when(sessionContext.getCallerPrincipal()).thenReturn(mockPrincipal);
        }

        @Test
        @DisplayName("All interceptors must call proceed() to continue the chain")
        void interceptorChain_AllCallProceed() throws Exception {
            // This test validates the fundamental interceptor contract:
            // every interceptor MUST call ctx.proceed() unless short-circuiting
            PerformanceMonitor pm = new PerformanceMonitor();
            AuditLogInterceptor al = new AuditLogInterceptor();
            al.setAuditService(auditService);
            al.setSessionContext(sessionContext);

            when(invocationContext.proceed()).thenReturn("result");
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());
            when(invocationContext.getParameters()).thenReturn(new Object[]{});

            pm.monitorPerformance(invocationContext);
            verify(invocationContext, times(1)).proceed();

            reset(invocationContext);
            when(invocationContext.proceed()).thenReturn("result");
            when(invocationContext.getMethod()).thenReturn(
                    Object.class.getMethod("toString"));
            when(invocationContext.getTarget()).thenReturn(new Object());
            when(invocationContext.getParameters()).thenReturn(new Object[]{});

            al.audit(invocationContext);
            verify(invocationContext, times(1)).proceed();
        }
    }
}
