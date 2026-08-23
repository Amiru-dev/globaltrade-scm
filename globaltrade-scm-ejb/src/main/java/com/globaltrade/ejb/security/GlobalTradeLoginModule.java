package com.globaltrade.ejb.security;

import com.globaltrade.common.util.PasswordUtil;
import com.globaltrade.common.util.SystemConstants;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Custom JAAS Login Module for GlobalTrade SCM authentication.
 *
 * <p>Implements JDBC-based authentication against the {@code users} and
 * {@code user_roles} tables in MySQL with BCrypt password hashing and
 * account lockout enforcement.</p>
 *
 * <p><strong>Critical Analysis — JAAS vs Jakarta Security API:</strong></p>
 * <ul>
 *   <li><strong>JAAS (Java Authentication and Authorization Service):</strong>
 *       The traditional Java SE security framework. Mature and well-understood,
 *       but verbose and tightly coupled to the container. Requires server-specific
 *       realm configuration (e.g., Payara's {@code JDBCRealm}).</li>
 *   <li><strong>Jakarta Security 3.0:</strong> The modern Jakarta EE approach
 *       with {@code @DatabaseIdentityStoreDefinition} — simpler, portable, and
 *       annotation-driven. However, JAAS is demonstrated here to show deeper
 *       understanding of the underlying authentication mechanism that Jakarta
 *       Security abstracts away.</li>
 * </ul>
 *
 * <p><strong>Password Security:</strong> Uses BCrypt with work factor 12 for
 * secure password hashing, replacing the previous SHA-256 implementation.
 * BCrypt provides adaptive work factor resistance to GPU cracking attacks.</p>
 *
 * <p><strong>Account Lockout:</strong> Enforces account lockout after 5 failed
 * login attempts for 30 minutes, protecting against brute force attacks.</p>
 *
 * <p><strong>Integration:</strong> Configured in Payara Server via
 * {@code login.conf} or Payara's admin console as a custom login realm.</p>
 */
public class GlobalTradeLoginModule implements LoginModule {

    private static final Logger LOGGER = Logger.getLogger(GlobalTradeLoginModule.class.getName());

    // Security constants from SystemConstants
    private static final int MAX_LOGIN_ATTEMPTS = SystemConstants.MAX_LOGIN_ATTEMPTS;
    private static final int ACCOUNT_LOCK_MINUTES = SystemConstants.ACCOUNT_LOCK_MINUTES;

    // JNDI datasource name
    private static final String DATASOURCE_JNDI = "jdbc/GlobalTradeDS";

    // SQL queries
    private static final String AUTHENTICATE_SQL =
            "SELECT password_hash, failed_login_attempts, account_locked_until FROM users WHERE username = ? AND active = TRUE";
    private static final String ROLES_SQL =
            "SELECT role_name FROM user_roles WHERE user_id = ?";
    private static final String INCREMENT_FAILED_ATTEMPTS_SQL =
            "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, " +
            "account_locked_until = CASE WHEN failed_login_attempts + 1 >= ? " +
            "THEN DATE_ADD(NOW(), INTERVAL ? MINUTE) ELSE NULL END " +
            "WHERE username = ?";
    private static final String RESET_FAILED_ATTEMPTS_SQL =
            "UPDATE users SET failed_login_attempts = 0, account_locked_until = NULL WHERE username = ?";

    // JAAS state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    // Authentication state
    private boolean loginSucceeded = false;
    private boolean commitSucceeded = false;
    private String authenticatedUsername;
    private final List<Principal> authenticatedPrincipals = new ArrayList<>();

    /**
     * Initialises this LoginModule with the relevant information.
     *
     * <p><strong>Critical Analysis:</strong> The {@code sharedState} map enables
     * stacked login modules to share credentials, supporting multi-factor
     * authentication chains (e.g., LDAP + OTP).</p>
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        LOGGER.log(Level.FINE, "GlobalTradeLoginModule initialized");
    }

    /**
     * Authenticates the user by verifying credentials against the database.
     *
     * <p><strong>Two-phase authentication:</strong> Phase 1 ({@code login()})
     * validates credentials but does not yet associate principals with the
     * subject. Phase 2 ({@code commit()}) performs the actual association.
     * This two-phase design supports atomic authentication across stacked
     * login modules — all must succeed before any commit.</p>
     */
    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("No CallbackHandler configured for GlobalTradeLoginModule");
        }

        // Collect username and password via callbacks
        NameCallback nameCallback = new NameCallback("Username: ");
        PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Callback handling failed", e);
            throw new LoginException("Failed to collect authentication credentials: " + e.getMessage());
        }

        String username = nameCallback.getName();
        char[] password = passwordCallback.getPassword();
        passwordCallback.clearPassword();

        if (username == null || username.isBlank() || password == null || password.length == 0) {
            LOGGER.log(Level.WARNING, "SECURITY: Empty credentials supplied");
            throw new LoginException("Username and password are required");
        }

        // Authenticate against database
        try {
            loginSucceeded = authenticateUser(username, password);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database authentication error for user: " + username, e);
            throw new LoginException("Authentication system error: " + e.getMessage());
        } finally {
            // Clear password from memory immediately
            java.util.Arrays.fill(password, '\0');
        }

        if (!loginSucceeded) {
            LOGGER.log(Level.WARNING, "SECURITY: Authentication FAILED for user: {0}", username);
            throw new LoginException("Invalid username or password");
        }

        authenticatedUsername = username;
        LOGGER.log(Level.INFO, "SECURITY: Authentication SUCCEEDED for user: {0}", username);
        return true;
    }

    /**
     * Phase 2: Associates the authenticated principal and roles with the subject.
     */
    @Override
    public boolean commit() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }

        try {
            // Create user principal
            SimplePrincipal userPrincipal = new SimplePrincipal(authenticatedUsername);
            authenticatedPrincipals.add(userPrincipal);

            // Load roles from database
            List<String> roles = loadUserRoles(authenticatedUsername);
            for (String role : roles) {
                SimplePrincipal rolePrincipal = new SimplePrincipal(role);
                authenticatedPrincipals.add(rolePrincipal);
            }

            // Associate all principals with the subject
            subject.getPrincipals().addAll(authenticatedPrincipals);

            commitSucceeded = true;
            LOGGER.log(Level.INFO, "SECURITY: Committed {0} principals for user: {1}",
                    new Object[]{authenticatedPrincipals.size(), authenticatedUsername});

            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to commit authentication for: " + authenticatedUsername, e);
            throw new LoginException("Failed to load user roles: " + e.getMessage());
        }
    }

    /**
     * Aborts the authentication process if login or commit failed.
     */
    @Override
    public boolean abort() throws LoginException {
        if (!loginSucceeded) {
            return false;
        }

        if (commitSucceeded) {
            // Login succeeded but overall authentication failed — clean up
            logout();
        }

        loginSucceeded = false;
        commitSucceeded = false;
        authenticatedUsername = null;
        authenticatedPrincipals.clear();

        LOGGER.log(Level.FINE, "GlobalTradeLoginModule: authentication aborted");
        return true;
    }

    /**
     * Logs out the user by removing associated principals from the subject.
     */
    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(authenticatedPrincipals);
        authenticatedPrincipals.clear();

        loginSucceeded = false;
        commitSucceeded = false;
        authenticatedUsername = null;

        LOGGER.log(Level.FINE, "GlobalTradeLoginModule: user logged out");
        return true;
    }

    // ======================== Private Helpers ========================

    /**
     * Authenticates user credentials against the database using BCrypt hashing
     * and enforces account lockout policy.
     *
     * <p><strong>Critical Analysis — Account Lockout:</strong></p>
     * <ul>
     *   <li><strong>Brute Force Protection:</strong> After 5 failed attempts, account
     *       is locked for 30 minutes, preventing automated credential guessing.</li>
     *   <li><strong>Security vs Usability:</strong> 30-minute lockout balances security
     *       (prevents rapid brute force) with usability (legitimate users can retry).</li>
     *   <li><strong>Threat Model:</strong> Protects against credential stuffing and
     *       brute force attacks on vendor/customs accounts.</li>
     * </ul>
     */
    private boolean authenticateUser(String username, char[] password) throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup(DATASOURCE_JNDI);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(AUTHENTICATE_SQL)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // User not found
                }

                // Check if account is locked
                LocalDateTime lockedUntil = rs.getTimestamp("account_locked_until") != null
                    ? rs.getTimestamp("account_locked_until").toLocalDateTime()
                    : null;

                if (lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil)) {
                    LOGGER.log(Level.WARNING, "SECURITY: Account locked for user: {0}", username);
                    throw new LoginException("Account locked due to multiple failed login attempts");
                }

                String storedHash = rs.getString("password_hash");
                boolean passwordValid = verifyPassword(password, storedHash);

                if (!passwordValid) {
                    // Increment failed login attempts
                    incrementFailedLoginAttempts(username);
                    LOGGER.log(Level.WARNING, "SECURITY: Failed login attempt for user: {0}", username);
                    return false;
                }

                // Reset failed attempts on successful login
                resetFailedLoginAttempts(username);
                return true;
            }
        }
    }

    /**
     * Verifies password using BCrypt.
     *
     * <p><strong>Critical Analysis:</strong> BCrypt.checkpw handles salt extraction
     * and constant-time comparison, providing superior security compared to manual
     * SHA-256 implementation.</p>
     */
    private boolean verifyPassword(char[] password, String storedHash) {
        try {
            return PasswordUtil.verifyPassword(new String(password), storedHash);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Password verification error", e);
            return false;
        }
    }

    /**
     * Increments failed login attempts and locks account if threshold reached.
     */
    private void incrementFailedLoginAttempts(String username) throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup(DATASOURCE_JNDI);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INCREMENT_FAILED_ATTEMPTS_SQL)) {

            stmt.setInt(1, SystemConstants.MAX_LOGIN_ATTEMPTS);
            stmt.setInt(2, SystemConstants.ACCOUNT_LOCK_MINUTES);
            stmt.setString(3, username);
            stmt.executeUpdate();
        }
    }

    /**
     * Resets failed login attempts on successful authentication.
     */
    private void resetFailedLoginAttempts(String username) throws Exception {
        DataSource ds = (DataSource) new InitialContext().lookup(DATASOURCE_JNDI);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(RESET_FAILED_ATTEMPTS_SQL)) {

            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    /**
     * Loads user roles from the database using user_id.
     */
    private List<String> loadUserRoles(String username) throws Exception {
        List<String> roles = new ArrayList<>();
        DataSource ds = (DataSource) new InitialContext().lookup(DATASOURCE_JNDI);

        // First get user_id from username
        Long userId;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?")) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return roles; // User not found, return empty roles
                }
                userId = rs.getLong("id");
            }
        }

        // Then load roles using user_id
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ROLES_SQL)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString("role_name"));
                }
            }
        }

        LOGGER.log(Level.FINE, "Loaded {0} roles for user: {1}",
                new Object[]{roles.size(), username});
        return roles;
    }

    // ======================== SimplePrincipal Inner Class ========================

    /**
     * Simple Principal implementation for JAAS subject association.
     */
    public static class SimplePrincipal implements Principal, java.io.Serializable {

        private static final long serialVersionUID = 1L;
        private final String name;

        public SimplePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimplePrincipal that = (SimplePrincipal) o;
            return name != null ? name.equals(that.name) : that.name == null;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "GlobalTradePrincipal[" + name + "]";
        }
    }
}
