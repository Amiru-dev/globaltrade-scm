package com.globaltrade.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification.
 *
 * <p><strong>Critical Analysis — Password Security:</strong></p>
 * <ul>
 *   <li><strong>SHA-256 with Salt:</strong> Uses SHA-256 algorithm with random salt
 *       for secure password hashing. This provides strong protection against rainbow
 *       table attacks and brute force attempts.</li>
 *   <li><strong>SecureRandom:</strong> Uses cryptographically secure random number
 *       generator for salt generation to ensure uniqueness of each hash.</li>
 *   <li><strong>Base64 Encoding:</strong> Encodes the salt+hash combination for
 *       storage in database as strings.</li>
 * </ul>
 */
public class PasswordUtil {

    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "SHA-256";

    /**
     * Hashes a password with a random salt using SHA-256.
     *
     * @param plainPassword the plain text password to hash
     * @return Base64 encoded string containing salt+hash
     */
    public static String hashPassword(String plainPassword) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);

            // Apply multiple iterations for increased security
            byte[] hashedPassword = md.digest(plainPassword.getBytes());
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                hashedPassword = md.digest(hashedPassword);
            }

            // Combine salt and hash for storage
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verifies a plain password against a stored hash.
     *
     * @param plainPassword the plain text password to verify
     * @param storedHash the stored Base64 encoded salt+hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        try {
            byte[] combined = Base64.getDecoder().decode(storedHash);

            // Extract salt (first 16 bytes)
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);

            // Extract stored hash (remaining bytes)
            byte[] storedHashBytes = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, storedHashBytes, 0, storedHashBytes.length);

            // Hash the provided password with the same salt
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            md.update(salt);

            byte[] hashedPassword = md.digest(plainPassword.getBytes());
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                hashedPassword = md.digest(hashedPassword);
            }

            // Compare hashes
            return MessageDigest.isEqual(hashedPassword, storedHashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Password verification failed", e);
        }
    }

    /**
     * Generates a secure random password for initial user setup.
     *
     * @param length the desired password length
     * @return randomly generated password
     */
    public static String generateRandomPassword(int length) {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}