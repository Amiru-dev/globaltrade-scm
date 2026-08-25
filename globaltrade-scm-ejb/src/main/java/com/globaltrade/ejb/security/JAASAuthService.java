package com.globaltrade.ejb.security;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.Stateless;

import javax.security.auth.Subject;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JAAS Authentication Service that wraps JAAS LoginContext for programmatic authentication.
 *
 * <p><strong>Purpose:</strong> This service provides a programmatic interface to JAAS authentication,
 * allowing servlets to authenticate users via JAAS LoginModule while maintaining compatibility
 * with Jakarta Security for container-level authorization.</p>
 *
 * <p><strong>Multi-Layered Security Architecture:</strong></p>
 * <ul>
 *   <li><strong>JAAS Layer:</strong> Provides explicit authentication mechanism via LoginContext</li>
 *   <li><strong>Jakarta Security Layer:</strong> Provides container-level authorization integration</li>
 *   <li><strong>JWT Layer:</strong> Provides stateless authentication for external APIs</li>
 * </ul>
 *
 * <p><strong>Critical Analysis — JAAS Integration:</strong></p>
 * <p>This service bridges JAAS authentication with Jakarta EE security by:</p>
 * <ol>
 *   <li>Using JAAS LoginContext to invoke GlobalTradeLoginModule</li>
 *   <li>Returning authentication results for session management</li>
 *   <li>Allowing Jakarta Security to handle container-level authorization</li>
 *   <li>Providing the explicit custom login module required by assignment specifications</li>
 * </ol>
 */
@Stateless
public class JAASAuthService {

    private static final Logger LOGGER = Logger.getLogger(JAASAuthService.class.getName());

    // JAAS configuration name (must match login.conf or server configuration)
    private static final String JAAS_CONFIG_NAME = "GlobalTradeRealm";

    /**
     * Authenticates a user using JAAS LoginContext.
     *
     * <p><strong>Process:</strong></p>
     * <ol>
     *   <li>Creates LoginContext with JAAS configuration name</li>
     *   <li>Uses CallbackHandler to provide username/password</li>
     *   <li>Invokes GlobalTradeLoginModule for credential validation</li>
     *   <li>Returns Subject with authenticated principals and roles</li>
     * </ol>
     *
     * @param username the username to authenticate
     * @param password the plain text password
     * @return Subject containing authenticated principals and roles
     * @throws LoginException if authentication fails
     */
    @PermitAll
    public Subject authenticate(String username, char[] password) throws LoginException {
        LOGGER.log(Level.INFO, "JAAS: Starting authentication for user: {0}", username);

        // Create callback handler for username/password
        CallbackHandler callbackHandler = callbacks -> {
            for (Object callbackObj : callbacks) {
                if (callbackObj instanceof NameCallback) {
                    ((NameCallback) callbackObj).setName(username);
                } else if (callbackObj instanceof PasswordCallback) {
                    ((PasswordCallback) callbackObj).setPassword(password);
                }
            }
        };

        // Create LoginContext with the JAAS configuration
        LoginContext loginContext = new LoginContext(JAAS_CONFIG_NAME, callbackHandler);

        try {
            // Perform JAAS authentication
            loginContext.login();
            Subject subject = loginContext.getSubject();

            LOGGER.log(Level.INFO, "JAAS: Authentication SUCCEEDED for user: {0}, principals: {1}",
                    new Object[]{username, subject.getPrincipals().size()});

            return subject;

        } catch (LoginException e) {
            LOGGER.log(Level.WARNING, "JAAS: Authentication FAILED for user: {0}", username);
            throw e;
        }
    }

    /**
     * Logs out a user from JAAS session.
     *
     * @param subject the subject to logout
     * @throws LoginException if logout fails
     */
    @PermitAll
    public void logout(Subject subject) throws LoginException {
        LOGGER.log(Level.INFO, "JAAS: Logging out subject with {0} principals",
                subject.getPrincipals().size());

        // Create LoginContext with the subject to logout
        LoginContext loginContext = new LoginContext(JAAS_CONFIG_NAME, subject, null);

        try {
            loginContext.logout();
            LOGGER.log(Level.INFO, "JAAS: Logout completed successfully");
        } catch (LoginException e) {
            LOGGER.log(Level.SEVERE, "JAAS: Logout failed", e);
            throw e;
        }
    }

    /**
     * Extracts role names from a JAAS Subject.
     *
     * @param subject the authenticated subject
     * @return Set of role names (Strings)
     */
    @PermitAll
    public Set<String> extractRoles(Subject subject) {
        return subject.getPrincipals().stream()
                .map(principal -> principal.getName())
                .filter(name -> !name.equals(subject.getPrincipals().iterator().next().getName())) // Exclude username principal
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Extracts username from a JAAS Subject.
     *
     * @param subject the authenticated subject
     * @return username (String)
     */
    @PermitAll
    public String extractUsername(Subject subject) {
        return subject.getPrincipals().iterator().next().getName();
    }
}