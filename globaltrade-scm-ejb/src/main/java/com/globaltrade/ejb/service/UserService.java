package com.globaltrade.ejb.service;

import com.globaltrade.common.entity.User;
import com.globaltrade.common.util.PasswordUtil;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for user management and authentication operations.
 *
 * <p><strong>Critical Analysis — User Service:</strong></p>
 * <ul>
 *   <li><strong>Authentication:</strong> Provides secure password verification
 *       with account lockout protection against brute force attacks.</li>
 *   <li><strong>User Management:</strong> CRUD operations for user accounts with
 *       role assignment and account status management.</li>
 *   <li><strong>Security:</strong> Integrates with EJB security context for proper
 *       authorization of user management operations.</li>
 * </ul>
 */
@Stateless
public class UserService {

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    /**
     * Authenticates a user with username and password.
     *
     * @param username the username to authenticate
     * @param plainPassword the plain text password to verify
     * @return authenticated User object, or null if authentication fails
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User authenticate(String username, String plainPassword) {
        try {
            TypedQuery<User> query = em.createNamedQuery("User.findByUsername", User.class);
            query.setParameter("username", username);
            List<User> results = query.getResultList();

            if (results.isEmpty()) {
                LOGGER.log(Level.WARNING, "Authentication failed: User not found - {0}", username);
                return null;
            }

            User user = results.get(0);

            // Check if account is active
            if (!user.isActive()) {
                LOGGER.log(Level.WARNING, "Authentication failed: Account inactive - {0}", username);
                return null;
            }

            // Check if account is locked
            if (user.isAccountLocked()) {
                LOGGER.log(Level.WARNING, "Authentication failed: Account locked - {0}", username);
                return null;
            }

            // Verify password
            if (PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
                // Reset failed login attempts and update last login
                user.resetFailedLoginAttempts();
                user.updateLastLogin();
                em.merge(user);

                LOGGER.log(Level.INFO, "Authentication successful: {0}", username);
                return user;
            } else {
                // Record failed login attempt
                user.recordFailedLogin();
                em.merge(user);

                LOGGER.log(Level.WARNING, "Authentication failed: Invalid password - {0} (attempts: {1})",
                        new Object[]{username, user.getFailedLoginAttempts()});
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Authentication error for user: " + username, e);
            return null;
        }
    }

    /**
     * Creates a new user account.
     *
     * @param user the user to create (password should be plain text, will be hashed)
     * @return the created user with hashed password
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User createUser(User user) {
        LOGGER.log(Level.INFO, "Creating user: {0}", user.getUsername());

        // Hash the password before storing
        String hashedPassword = PasswordUtil.hashPassword(user.getPasswordHash());
        user.setPasswordHash(hashedPassword);

        // Set creation timestamp
        user.setCreatedAt(LocalDateTime.now());

        em.persist(user);
        return user;
    }

    /**
     * Updates an existing user account.
     *
     * @param user the user to update (password should be plain text if being changed, will be hashed)
     * @return the updated user
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public User updateUser(User user) {
        LOGGER.log(Level.INFO, "Updating user: {0}", user.getUsername());
        
        // Hash the password if it's being updated
        String currentPassword = user.getPasswordHash();
        if (currentPassword != null && !currentPassword.isEmpty()) {
            // Check if this is a plain text password (not a hash)
            // A proper SHA-256 hash with salt will be much longer than typical passwords
            // Base64 encoded salt(16) + hash(32) = 48 bytes minimum
            if (currentPassword.length() < 40) {
                String hashedPassword = PasswordUtil.hashPassword(currentPassword);
                user.setPasswordHash(hashedPassword);
                LOGGER.log(Level.INFO, "Password hashed for user: {0}", user.getUsername());
            }
        }
        
        return em.merge(user);
    }

    /**
     * Changes a user's password.
     *
     * @param username the username of the user
     * @param oldPassword the current password (for verification)
     * @param newPassword the new password to set
     * @return true if password changed successfully, false otherwise
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        try {
            TypedQuery<User> query = em.createNamedQuery("User.findByUsername", User.class);
            query.setParameter("username", username);
            List<User> results = query.getResultList();

            if (results.isEmpty()) {
                return false;
            }

            User user = results.get(0);

            // Verify old password
            if (!PasswordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
                LOGGER.log(Level.WARNING, "Password change failed: Invalid old password for {0}", username);
                return false;
            }

            // Set new password
            String hashedNewPassword = PasswordUtil.hashPassword(newPassword);
            user.setPasswordHash(hashedNewPassword);
            em.merge(user);

            LOGGER.log(Level.INFO, "Password changed successfully for: {0}", username);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error changing password for user: " + username, e);
            return false;
        }
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return the user, or null if not found
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findByUsername(String username) {
        TypedQuery<User> query = em.createNamedQuery("User.findByUsername", User.class);
        query.setParameter("username", username);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Finds a user by email.
     *
     * @param email the email to search for
     * @return the user, or null if not found
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public User findByEmail(String email) {
        TypedQuery<User> query = em.createNamedQuery("User.findByEmail", User.class);
        query.setParameter("email", email);
        List<User> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Finds all users with a specific role.
     *
     * @param role the role to search for
     * @return list of users with the specified role
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> findByRole(String role) {
        TypedQuery<User> query = em.createNamedQuery("User.findByRole", User.class);
        query.setParameter("role", role);
        return query.getResultList();
    }

    /**
     * Finds all active users.
     *
     * @return list of active users
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<User> findActiveUsers() {
        return em.createNamedQuery("User.findActiveUsers", User.class).getResultList();
    }

    /**
     * Deactivates a user account.
     *
     * @param username the username of the user to deactivate
     * @return true if deactivation successful, false otherwise
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean deactivateUser(String username) {
        try {
            User user = findByUsername(username);
            if (user != null) {
                user.setActive(false);
                em.merge(user);
                LOGGER.log(Level.INFO, "User deactivated: {0}", username);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating user: " + username, e);
            return false;
        }
    }

    /**
     * Activates a user account.
     *
     * @param username the username of the user to activate
     * @return true if activation successful, false otherwise
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean activateUser(String username) {
        try {
            User user = findByUsername(username);
            if (user != null) {
                user.setActive(true);
                user.resetFailedLoginAttempts();
                em.merge(user);
                LOGGER.log(Level.INFO, "User activated: {0}", username);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating user: " + username, e);
            return false;
        }
    }

    /**
     * Unlocks a user account (resets failed login attempts).
     *
     * @param username the username of the user to unlock
     * @return true if unlock successful, false otherwise
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean unlockUser(String username) {
        try {
            User user = findByUsername(username);
            if (user != null) {
                user.resetFailedLoginAttempts();
                em.merge(user);
                LOGGER.log(Level.INFO, "User unlocked: {0}", username);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error unlocking user: " + username, e);
            return false;
        }
    }

    /**
     * Adds a role to a user.
     *
     * @param username the username of the user
     * @param role the role to add
     * @return true if role added successfully, false otherwise
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean addRole(String username, String role) {
        try {
            User user = findByUsername(username);
            if (user != null) {
                user.addRole(role);
                em.merge(user);
                LOGGER.log(Level.INFO, "Role added to user: {0} -> {1}", new Object[]{username, role});
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding role to user: " + username, e);
            return false;
        }
    }

    /**
     * Removes a role from a user.
     *
     * @param username the username of the user
     * @param role the role to remove
     * @return true if role removed successfully, false otherwise
     */
    @RolesAllowed({"SYSTEM_ADMIN"})
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean removeRole(String username, String role) {
        try {
            User user = findByUsername(username);
            if (user != null) {
                user.getRoles().remove(role);
                em.merge(user);
                LOGGER.log(Level.INFO, "Role removed from user: {0} -> {1}", new Object[]{username, role});
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error removing role from user: " + username, e);
            return false;
        }
    }

    /**
     * Gets all roles for a user.
     *
     * @param username the username of the user
     * @return set of roles, or empty set if user not found
     */
    @PermitAll
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Set<String> getUserRoles(String username) {
        try {
            User user = findByUsername(username);
            if (user != null) {
                return user.getRoles();
            }
            return Set.of();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting roles for user: " + username, e);
            return Set.of();
        }
    }
}