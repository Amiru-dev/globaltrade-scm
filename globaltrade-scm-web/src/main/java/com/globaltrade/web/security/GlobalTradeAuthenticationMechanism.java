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
 * container security context for the GlobalTrade SCM login page.
 *
 * <p><strong>Root cause this fixes:</strong> {@code LoginServlet} previously authenticated
 * users by calling {@code UserService.authenticate()} directly and storing the result only
 * as {@code HttpSession} attributes. That never told the container who the caller was, so
 * every subsequent {@code @EJB}-injected call (e.g. {@code TransactionServlet} &rarr;
 * {@code InventoryService.transferInventory()}) was executed as an anonymous caller. Since
 * {@code InventoryService} is annotated {@code @RolesAllowed(...)}, the EJB container
 * correctly rejected the anonymous caller with {@code EJBAccessException: Client not
 * authorized for this invocation} — the annotation was doing its job; nothing was ever
 * populating the identity it checks.</p>
 *
 * <p><strong>How this mechanism works:</strong> It does nothing on ordinary requests
 * ({@link HttpMessageContext#doNothing()}). It only activates when the application
 * explicitly calls {@code SecurityContext.authenticate(request, response, params)} — which
 * sets {@link HttpMessageContext#isAuthenticationRequest()} to {@code true} — at which point
 * it delegates credential validation to {@link GlobalTradeIdentityStore} via the standard
 * {@link IdentityStoreHandler}, and notifies the container of the result. This keeps the
 * existing custom login form/flow intact while making the authentication result "real" from
 * the container's point of view.</p>
 *
 * <p><strong>{@literal @}AutoApplySession — Critical Analysis:</strong> Jakarta Security's
 * programmatic {@code authenticate()} call, by itself, only establishes the caller identity
 * for the current request/response round trip. {@code @AutoApplySession} instructs the
 * container to remember the successful authentication result in the {@code HttpSession} and
 * automatically re-apply it as the caller identity on every subsequent request in that
 * session — without re-challenging the user — which is exactly the persistence model the
 * application's session-based login page already assumes.</p>
 */
@ApplicationScoped
@AutoApplySession
public class GlobalTradeAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                                                 HttpMessageContext httpMessageContext) throws AuthenticationException {

        if (httpMessageContext.isAuthenticationRequest()) {
            Credential credential = httpMessageContext.getAuthParameters().getCredential();
            CredentialValidationResult result = identityStoreHandler.validate(credential);
            return httpMessageContext.notifyContainerAboutLogin(result);
        }

        // Not an explicit authenticate() call (e.g. a plain page request) — let the
        // request proceed unauthenticated for this mechanism; @AutoApplySession handles
        // re-establishing an already-authenticated session's identity separately.
        return httpMessageContext.doNothing();
    }
}
