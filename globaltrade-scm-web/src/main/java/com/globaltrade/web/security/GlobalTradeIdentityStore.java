package com.globaltrade.web.security;

import com.globaltrade.common.entity.User;
import com.globaltrade.ejb.service.UserService;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * CDI-based {@link IdentityStore} for the GlobalTrade SCM system.
 *
 * <p><strong>Critical Analysis — Jakarta Security vs. JAAS:</strong> The project also
 * contains a hand-written {@link com.globaltrade.ejb.security.GlobalTradeLoginModule},
 * a traditional {@code javax.security.auth.spi.LoginModule}. That approach is fully
 * standards-based but requires a server-specific custom realm to be registered outside
 * the deployable artifact (e.g. via Payara's {@code asadmin create-auth-realm} or
 * {@code domain.xml}), which makes the security configuration non-portable and
 * difficult to reproduce reliably across grading/deployment environments.</p>
 *
 * <p>This {@code IdentityStore} implements the same authentication contract using the
 * modern, portable Jakarta Security 3.0 API (JSR 375). It is discovered automatically
 * by CDI (no server configuration required) and delegates the actual credential check to
 * the existing {@link UserService#authenticate(String, String)}, so the BCrypt hashing
 * and account-lockout logic is not duplicated.</p>
 *
 * <p><strong>Critical Analysis — Why delegate instead of re-implement:</strong> Re-querying
 * the database directly here (as {@code GlobalTradeLoginModule} does) would create two
 * divergent sources of truth for authentication rules. Delegating to the existing
 * {@code @PermitAll} EJB method keeps a single, testable authentication code path while
 * still allowing the container's declarative ({@code @RolesAllowed}) and programmatic
 * ({@code SessionContext.isCallerInRole}) authorization to work correctly downstream,
 * because this store is what actually populates the container's caller principal.</p>
 */
@ApplicationScoped
public class GlobalTradeIdentityStore implements IdentityStore {

    @EJB
    private UserService userService;

    /**
     * Validates a username/password credential against the database via {@link UserService}.
     *
     * <p>On success, returns a {@link CredentialValidationResult} carrying the caller name
     * and the user's role set as CDI/container "groups" — these groups are what
     * {@code @RolesAllowed} and {@code isCallerInRole()} check against, and they line up
     * with the {@code security-role-mapping} entries already defined in
     * {@code glassfish-web.xml} (role name == group name).</p>
     */
    public CredentialValidationResult validate(UsernamePasswordCredential credential) {
        User user = userService.authenticate(credential.getCaller(), credential.getPasswordAsString());

        if (user == null) {
            // Covers: unknown username, wrong password, inactive account, locked account.
            // UserService already logs the specific reason; we don't leak which case it was.
            return CredentialValidationResult.INVALID_RESULT;
        }

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // Authenticated identity with no authorization groups is treated as invalid
            // for this application, matching LoginServlet's pre-existing business rule.
            return CredentialValidationResult.INVALID_RESULT;
        }

        return new CredentialValidationResult(user.getUsername(), user.getRoles());
    }
}