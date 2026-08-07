package com.voltaras.authservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Generates cryptographically secure password-reset tokens and
 * computes their SHA-256 hashes.
 *
 * Only the hash is ever persisted; the raw token travels exclusively
 * through the reset email/link.
 */
public final class PasswordResetTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 32 random bytes -> 64 hex characters. */
    private static final int TOKEN_BYTES = 32;

    private PasswordResetTokenUtil() {
    }

    /**
     * Generates a URL-safe raw reset token (64 lowercase hex chars).
     */
    public static String generateRawToken() {

        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);

        return toHex(bytes);
    }

    /**
     * Returns the SHA-256 hex digest of the given raw token.
     */
    public static String hashToken(String rawToken) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return toHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available on this JVM",
                    exception
            );
        }
    }

    private static String toHex(byte[] bytes) {

        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }

        return hex.toString();
    }
}
