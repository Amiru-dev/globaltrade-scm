package com.globaltrade.ejb.test;

import com.globaltrade.ejb.security.SecurityService;

import jakarta.ejb.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EJB security components.
 *
 * <p><strong>Critical Analysis — Security Testing:</strong>
 * These tests verify programmatic security logic using mocked {@link SessionContext}.
 * Declarative security ({@code @RolesAllowed}) is enforced by the container and
 * would be tested via integration tests.</p>
 *
 * <p>Key scenarios:</p>
 * <ul>
 *   <li>Data-level access control (vendor can only see own data)</li>
 *   <li>Role hierarchy (admin overrides)</li>
 *   <li>Emergency access authorization</li>
 *   <li>Multiple role checking</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Service Unit Tests")
class SecurityTest {

    @Mock
    private SessionContext sessionContext;

    @Mock
    private Principal principal;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        lenient().when(sessionContext.getCallerPrincipal()).thenReturn(principal);
        lenient().when(principal.getName()).thenReturn("testuser");
    }

    @Nested
    @DisplayName("Vendor Data Access Control Tests")
    class VendorAccessTests {

        @Test
        @DisplayName("SYSTEM_ADMIN should access any vendor data")
        void canAccessVendorData_Admin_GrantsAccess() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(true);

            boolean result = securityService.canAccessVendorData("VENDOR-A", "VENDOR-B");

            assertTrue(result, "SYSTEM_ADMIN should have unrestricted vendor access");
        }

        @Test
        @DisplayName("LOGISTICS_COORDINATOR should access any vendor data")
        void canAccessVendorData_Coordinator_GrantsAccess() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(false);
            when(sessionContext.isCallerInRole("LOGISTICS_COORDINATOR")).thenReturn(true);

            boolean result = securityService.canAccessVendorData("VENDOR-A", "VENDOR-B");

            assertTrue(result, "LOGISTICS_COORDINATOR should have unrestricted vendor access");
        }

        @Test
        @DisplayName("VENDOR_REPRESENTATIVE should access own vendor data only")
        void canAccessVendorData_VendorRep_OwnData_GrantsAccess() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(false);
            when(sessionContext.isCallerInRole("LOGISTICS_COORDINATOR")).thenReturn(false);
            when(sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")).thenReturn(true);

            boolean result = securityService.canAccessVendorData("VENDOR-A", "VENDOR-A");

            assertTrue(result, "Vendor rep should access own vendor data");
        }

        @Test
        @DisplayName("VENDOR_REPRESENTATIVE should be DENIED access to other vendor data")
        void canAccessVendorData_VendorRep_OtherData_DeniesAccess() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(false);
            when(sessionContext.isCallerInRole("LOGISTICS_COORDINATOR")).thenReturn(false);
            when(sessionContext.isCallerInRole("VENDOR_REPRESENTATIVE")).thenReturn(true);

            boolean result = securityService.canAccessVendorData("VENDOR-A", "VENDOR-B");

            assertFalse(result, "Vendor rep should NOT access other vendor's data");
        }
    }

    @Nested
    @DisplayName("Emergency Access Tests")
    class EmergencyAccessTests {

        @Test
        @DisplayName("SYSTEM_ADMIN should be granted emergency access")
        void authorizeEmergencyAccess_Admin_Grants() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(true);

            boolean result = securityService.authorizeEmergencyAccess(
                    "SHIPMENT", "Port closure — Hurricane Alpha");

            assertTrue(result, "Admin should be granted emergency access");
        }

        @Test
        @DisplayName("Non-admin should be DENIED emergency access")
        void authorizeEmergencyAccess_NonAdmin_Denies() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(false);

            boolean result = securityService.authorizeEmergencyAccess(
                    "SHIPMENT", "Attempted unauthorized access");

            assertFalse(result, "Non-admin should be denied emergency access");
        }
    }

    @Nested
    @DisplayName("Role Checking Tests")
    class RoleCheckTests {

        @Test
        @DisplayName("hasRole should return true for assigned role")
        void hasRole_AssignedRole_ReturnsTrue() {
            when(sessionContext.isCallerInRole("WAREHOUSE_MANAGER")).thenReturn(true);

            assertTrue(securityService.hasRole("WAREHOUSE_MANAGER"));
        }

        @Test
        @DisplayName("hasRole should return false for unassigned role")
        void hasRole_UnassignedRole_ReturnsFalse() {
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(false);

            assertFalse(securityService.hasRole("SYSTEM_ADMIN"));
        }

        @Test
        @DisplayName("hasAnyRole should return true if any role matches")
        void hasAnyRole_OneMatches_ReturnsTrue() {
            when(sessionContext.isCallerInRole("CUSTOMS_AGENT")).thenReturn(false);
            when(sessionContext.isCallerInRole("WAREHOUSE_MANAGER")).thenReturn(true);

            assertTrue(securityService.hasAnyRole("CUSTOMS_AGENT", "WAREHOUSE_MANAGER"));
        }

        @Test
        @DisplayName("hasAnyRole should return false if no role matches")
        void hasAnyRole_NoneMatch_ReturnsFalse() {
            when(sessionContext.isCallerInRole("CUSTOMS_AGENT")).thenReturn(false);
            when(sessionContext.isCallerInRole("SYSTEM_ADMIN")).thenReturn(false);

            assertFalse(securityService.hasAnyRole("CUSTOMS_AGENT", "SYSTEM_ADMIN"));
        }
    }

    @Nested
    @DisplayName("User Identity Tests")
    class UserIdentityTests {

        @Test
        @DisplayName("getCurrentUser should return authenticated user's name")
        void getCurrentUser_Authenticated_ReturnsUsername() {
            String user = securityService.getCurrentUser();
            assertEquals("testuser", user);
        }

        @Test
        @DisplayName("getCurrentUser should return ANONYMOUS when no principal")
        void getCurrentUser_NoPrincipal_ReturnsAnonymous() {
            when(sessionContext.getCallerPrincipal()).thenThrow(new RuntimeException("No auth"));

            String user = securityService.getCurrentUser();
            assertEquals("ANONYMOUS", user);
        }
    }
}
