package com.flowchain.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Wraps jBCrypt for password hashing and verification.
 *
 * Hashes use BCrypt with cost factor 12 (~250ms on commodity hardware).
 * The resulting string includes the salt and cost factor inline, so it
 * fits in users.password (VARCHAR(255)) and self-describes for verify().
 */
public final class PasswordUtil {

    private static final int COST = 12;

    private PasswordUtil() {
        // utility class
    }

    /**
     * Hash a plaintext password. Returns a 60-character bcrypt string.
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("password must not be empty");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(COST));
    }

    /**
     * Verify a plaintext password against a stored bcrypt hash.
     * Returns false on any malformed input rather than throwing — callers
     * should treat false as "invalid credentials" without leaking why.
     */
    public static boolean verify(String plaintext, String storedHash) {
        if (plaintext == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, storedHash);
        } catch (IllegalArgumentException e) {
            // storedHash is not a valid bcrypt string (e.g. plaintext mock data)
            return false;
        }
    }
}
