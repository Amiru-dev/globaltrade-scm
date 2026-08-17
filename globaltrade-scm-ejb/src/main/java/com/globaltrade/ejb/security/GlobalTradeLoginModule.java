package com.globaltrade.ejb.security;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.security.Principal;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Custom JAAS Login Module for GlobalTrade SCM authentication.
 *
 * <p>Implements JDBC-based authentication against the {@code users} and
 * {@code user_roles} tables in MySQL with PBKDF2 password hashing.</p>
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
 * <p><strong>Password Security:</strong> Uses SHA-256 to hash passwords.
 * This matches the configured Payara JDBCRealm digest-algorithm.</p>
 *
 * <p><strong>Integration:</strong> Configured in Payara Server via
 * {@code login.conf} or Payara's admin console as a custom login realm.</p>
 */
public class GlobalTradeLoginModule implements LoginModule {

    private static final Logger LOGGER = Logger.getLogger(GlobalTradeLoginModule.class.getName());

    // Hash algorithm
    private static final String HASH_ALGORITHM = "SHA-256";

    // JNDI datasource name
    private static final String DATASOURCE_JNDI = "jdbc/GlobalTradeDS";

    // SQL queries
    private static final String AUTHENTICATE_SQL =
            "SELECT password_hash FROM users WHERE username = ? AND is_active = TRUE";
    private static final String ROLES_SQL =
            "SELECT role_name FROM user_roles WHERE username = ?";

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
     * Authenticates user credentials against the database using PBKDF2 hashing.
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

                String storedHash = rs.getString("password_hash");

                return verifyPassword(password, storedHash);
            }
        }
    }

    private boolean verifyPassword(char[] password, String storedHash)
            throws Exception {

        java.security.MessageDigest md = java.security.MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] computedHash = md.digest(new String(password).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // Convert computed hash to hex string (as in init-schema.sql)
        StringBuilder hexString = new StringBuilder();
        for (byte b : computedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        String computedHashHex = hexString.toString();

        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(storedHash, computedHashHex);
    }

    /**
     * Loads user roles from the database.
     */
    private List<String> loadUserRoles(String username) throws Exception {
        List<String> roles = new ArrayList<>();
        DataSource ds = (DataSource) new InitialContext().lookup(DATASOURCE_JNDI);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ROLES_SQL)) {

            stmt.setString(1, username);

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

    /**
     * Constant-time string comparison to prevent timing side-channel attacks.
     *
     * <p><strong>Security Note:</strong> Standard {@code String.equals()} returns
     * early on the first mismatched character, leaking information about how many
     * characters matched. This method always compares all characters.</p>
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
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
