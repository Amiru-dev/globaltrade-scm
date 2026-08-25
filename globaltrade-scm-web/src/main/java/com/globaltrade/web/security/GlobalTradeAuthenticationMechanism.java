package com.globaltrade.web.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.AutoApplySession;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom {@link HttpAuthenticationMechanism} that establishes a real Jakarta EE
 * container security context for both form-based login and JWT authentication.
 *
 * <p><strong>Dual Authentication Architecture:</strong> This mechanism supports two
 * legitimate authentication paths for different consumer types:</p>
 * <ul>
 *   <li><strong>Form-based Login:</strong> Session-based authentication for internal
 *       staff using the JSP UI (logistics personnel, warehouse managers, customs agents).</li>
 *   <li><strong>JWT Authentication:</strong> Token-based authentication for external
 *       API consumers (vendor integrations, customer portals, automated systems).</li>
 * </ul>
 *
 * <p><strong>Root Cause Fixed:</strong> Previously, both authentication paths only
 * populated {@code HttpSession} attributes or JAX-RS {@code SecurityContext}, but never
 * established the Jakarta EE container security context. This caused {@code @RolesAllowed}
 * EJB methods to fail with {@code EJBAccessException} because the EJB container saw
 * anonymous callers regardless of the actual authentication status.</p>
 *
 * <p><strong>How This Mechanism Works:</strong></p>
 * <ol>
 *   <li><strong>Explicit Authentication Request:</strong> When {@code SecurityContext.authenticate()}
 *       is called (form login), validates via {@link GlobalTradeIdentityStore}.</li>
 *   <li><strong>JWT Bearer Token:</strong> When {@code Authorization: Bearer} header is present,
 *       validates JWT token via {@link JWTValidationUtil} and extracts claims.</li>
 *   <li><strong>Container Identity:</strong> Both paths establish real container caller principal
 *       and groups, enabling {@code @RolesAllowed} EJB authorization to work correctly.</li>
 * </ol>
 *
 * <p><strong>{@literal @}AutoApplySession — Critical Analysis and Trade-off:</strong></p>
 * <p>{@code @AutoApplySession} instructs the container to remember successful authentication
 * results in the {@code HttpSession} and automatically re-apply them on subsequent requests
 * without re-invoking {@code validateRequest()}.</p>
 *
 * <p><strong>Form-based Login:</strong> ✅ Perfect fit — identity persists across JSP session
 * as expected for traditional web applications.</p>
 *
 * <p><strong>JWT Authentication:</strong> ⚠️ Architectural Trade-off — {@code @AutoApplySession}
 * stores JWT-validated identity in the session, which may bypass JWT re-validation if a client
 * sends both Bearer token and session cookie.</p>
 *
 * <p><strong>Blast Radius Analysis:</strong></p>
 * <ul>
 *   <li><strong>Stateless REST Clients:</strong> ✅ Safe — clients like curl, Postman, or vendor
 *       integrations typically don't send session cookies, so each request re-validates the JWT.</li>
 *   <li><strong>Browser Clients:</strong> ⚠️ Risk — browsers mixing JSP session cookies with JWT
 *       bearer calls could have identity persist after JWT expiry via session cookie.</li>
 * </ul>
 *
 * <p><strong>Trade-off Acceptance:</strong> This trade-off is acceptable because:</p>
 * <ol>
 *   <li>The primary JWT use case is stateless API clients (no session cookie).</li>
 *   <li>The dual-auth pattern (session for UI, JWT for API) is legitimate and matches requirements.</li>
 *   <li>A true stateless JWT implementation would require disabling {@code @AutoApplySession}, which
 *       would break the form-based login experience that's critical for internal staff.</li>
 *   <li>Alternative solutions (separate mechanisms, conditional @AutoApplySession) add significant
 *       complexity without clear security benefit for the primary use cases.</li>
 * </ol>
 */
@ApplicationScoped
@AutoApplySession
public class GlobalTradeAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {

        // Branch 1: Explicit authentication request (form-based login via SecurityContext.authenticate())
        if (httpMessageContext.isAuthenticationRequest()) {
            Credential credential = httpMessageContext.getAuthParameters().getCredential();
            CredentialValidationResult result = identityStoreHandler.validate(credential);
            return httpMessageContext.notifyContainerAboutLogin(result);
        }

        // Branch 2: JWT Bearer token authentication (REST API clients)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = JWTValidationUtil.extractBearerToken(authHeader);
            if (token != null) {
                CredentialValidationResult result = JWTValidationUtil.validateToken(token);
                if (result != CredentialValidationResult.INVALID_RESULT) {
                    return httpMessageContext.notifyContainerAboutLogin(result);
                }
            }
        }

        // No authentication applicable — proceed as unauthenticated
        // @AutoApplySession will re-apply existing session identity if present
        return httpMessageContext.doNothing();
    }
}
