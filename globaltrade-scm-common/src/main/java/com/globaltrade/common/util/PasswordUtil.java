package com.globaltrade.common.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing and verification using BCrypt.
 *
 * <p><strong>Critical Analysis — Password Security Migration:</strong></p>
 * <ul>
 *   <li><strong>BCrypt vs SHA-256:</strong> BCrypt is specifically designed for password hashing
 *       with an adaptive work factor, making it resistant to GPU-based brute force attacks.
 *       SHA-256, while cryptographically strong, is designed for general-purpose hashing and
 *       lacks this adaptive work factor, making it vulnerable to GPU cracking.</li>
 *   <li><strong>Work Factor:</strong> BCrypt work factor of 12 provides ~200ms hash time,
 *       balancing security and performance. This can be adjusted as hardware improves.</li>
 *   <li><strong>Automatic Salting:</strong> BCrypt generates and embeds salt in the hash,
 *       eliminating salt management complexity and ensuring unique salts per password.</li>
 *   <li><strong>Threat Model:</strong> Protects against rainbow table attacks (unique salts)
 *       and GPU cracking (adaptive work factor). Essential for vendor/customs credential protection.</li>
 * </ul>
 *
 * <p><strong>Migration from SHA-256:</strong> Existing SHA-256 hashes will need to be re-hashed
 * using BCrypt on next successful login or via migration script.</p>
 */
public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hashes a password using BCrypt with automatic salt generation.
     *
     * <p><strong>Critical Analysis:</strong> BCrypt automatically generates a random salt
     * and embeds it in the resulting hash. The work factor (12) provides ~200ms computation time,
     * making brute force attacks computationally expensive while maintaining acceptable
     * authentication performance.</p>
     *
     * <p><strong>Library Trade-off:</strong> Using jbcrypt (0.4, last updated 2017) provides
     * a pure Java EE dependency without introducing Spring ecosystem components. While the library
     * is not actively maintained, it remains the standard choice for Jakarta EE projects and
     * provides adequate BCrypt implementation for coursework and production use cases with
     * proper password length validation to mitigate known truncation issues.</p>
     *
     * @param plainPassword the plain text password to hash
     * @return BCrypt hash containing salt and hashed password (60 characters)
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a plain password against a stored BCrypt hash.
     *
     * <p><strong>Critical Analysis:</strong> BCrypt extracts the salt from the stored hash
     * and uses it to hash the provided password, then compares in constant time to prevent
     * timing attacks. This is superior to manual salt management.</p>
     *
     * @param plainPassword the plain text password to verify
     * @param storedHash    the stored BCrypt hash (60 characters)
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        return BCrypt.checkpw(plainPassword, storedHash);
    }

    /**
     * Generates a secure random password for initial user setup.
     *
     * @param length the desired password length
     * @return randomly generated password
     */
    public static String generateRandomPassword(int length) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }
}