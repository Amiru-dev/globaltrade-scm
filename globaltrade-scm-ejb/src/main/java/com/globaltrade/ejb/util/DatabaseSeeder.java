package com.globaltrade.ejb.util;

import com.globaltrade.common.entity.User;
import com.globaltrade.common.util.PasswordUtil;
import com.globaltrade.ejb.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database seeder for initial user accounts.
 *
 * <p><strong>Critical Analysis — Database Seeding:</strong></p>
 * <ul>
 *   <li><strong>Startup Initialization:</strong> Uses @Startup to ensure users
 *       are created when the application first deploys.</li>
 *   <li><strong>Default Users:</strong> Creates standard users for each role
 *       with default passwords for initial system access.</li>
 *   <li><strong>Security Note:</strong> Default passwords should be changed
 *       immediately after first deployment in production.</li>
 * </ul>
 */
@Singleton
@Startup
public class DatabaseSeeder {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    @Inject
    private UserService userService;

    @PostConstruct
    public void seedDatabase() {
        LOGGER.log(Level.INFO, "Starting database seeding...");

        try {
            // Create default users for each role
            createDefaultUsers();
            LOGGER.log(Level.INFO, "Database seeding completed successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database seeding failed", e);
        }
    }

    private void createDefaultUsers() {
        // Create admin user
        createUserIfNotExists(
                "admin",
                "admin123", // Default password - CHANGE IN PRODUCTION
                "admin@globaltrade.com",
                "System Administrator",
                Set.of("SYSTEM_ADMIN")
        );

        // Create logistics coordinator
        createUserIfNotExists(
                "logistics",
                "logistics123", // Default password - CHANGE IN PRODUCTION
                "logistics@globaltrade.com",
                "Logistics Coordinator",
                Set.of("LOGISTICS_COORDINATOR")
        );

        // Create warehouse manager
        createUserIfNotExists(
                "warehouse",
                "warehouse123", // Default password - CHANGE IN PRODUCTION
                "warehouse@globaltrade.com",
                "Warehouse Manager",
                Set.of("WAREHOUSE_MANAGER")
        );

        // Create customs agent
        createUserIfNotExists(
                "customs",
                "customs123", // Default password - CHANGE IN PRODUCTION
                "customs@globaltrade.com",
                "Customs Agent",
                Set.of("CUSTOMS_AGENT")
        );

        // Create vendor representative
        createUserIfNotExists(
                "vendor",
                "vendor123", // Default password - CHANGE IN PRODUCTION
                "vendor@globaltrade.com",
                "Vendor Representative",
                Set.of("VENDOR_REPRESENTATIVE")
        );
    }

    private void createUserIfNotExists(String username, String password, String email,
                                       String fullName, Set<String> roles) {
        try {
            // Check if user already exists
            User existingUser = userService.findByUsername(username);
            if (existingUser != null) {
                LOGGER.log(Level.INFO, "User already exists: {0}", username);
                // Update password for existing users to fix password hash issues
                LOGGER.log(Level.INFO, "Updating password for existing user: {0}", username);
                existingUser.setPasswordHash(password); // Will be hashed by UserService
                existingUser.setEmail(email);
                existingUser.setFullName(fullName);
                existingUser.setRoles(new HashSet<>(roles));
                userService.updateUser(existingUser);
                LOGGER.log(Level.INFO, "Updated user: {0} with new password hash", username);
                return;
            }

            // Create new user with plain password (UserManager will hash it)
            User user = new User(username, password, email, fullName);
            user.setRoles(new HashSet<>(roles));

            User createdUser = userService.createUser(user);
            LOGGER.log(Level.INFO, "Created user: {0} with roles: {1}",
                    new Object[]{username, roles});

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create/update user: " + username, e);
        }
    }
}