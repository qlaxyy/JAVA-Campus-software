package edu.seu.vcampus.server.module.user;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Server-side password credential; the reusable client proof is retained only for migration. */
final class PasswordCredential {

    static final int CURRENT_ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String legacyProof;
    private final String hash;
    private final String salt;
    private final int iterations;

    private PasswordCredential(
            String legacyProof,
            String hash,
            String salt,
            int iterations) {
        this.legacyProof = legacyProof;
        this.hash = hash;
        this.salt = salt;
        this.iterations = iterations;
    }

    static PasswordCredential create(String passwordProof) {
        requireProof(passwordProof);
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        return create(passwordProof, saltBytes, CURRENT_ITERATIONS);
    }

    static PasswordCredential legacy(String passwordProof) {
        requireProof(passwordProof);
        return new PasswordCredential(passwordProof, null, null, 0);
    }

    static PasswordCredential persisted(String hash, String salt, int iterations) {
        Objects.requireNonNull(hash, "password hash must not be null");
        Objects.requireNonNull(salt, "password salt must not be null");
        if (hash.isBlank() || salt.isBlank() || iterations <= 0) {
            throw new IllegalArgumentException("Invalid persisted password credential.");
        }
        return new PasswordCredential(null, hash, salt, iterations);
    }

    boolean matches(String passwordProof) {
        if (passwordProof == null || !passwordProof.matches("[0-9a-f]{64}")) {
            return false;
        }
        if (legacyProof != null) {
            return MessageDigest.isEqual(
                    legacyProof.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    passwordProof.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        byte[] expected = Base64.getDecoder().decode(hash);
        byte[] actual = derive(
                passwordProof,
                Base64.getDecoder().decode(salt),
                iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    boolean needsUpgrade() {
        return legacyProof != null || iterations < CURRENT_ITERATIONS;
    }

    String legacyProofOrSentinel() {
        return legacyProof == null ? "0".repeat(64) : legacyProof;
    }

    String hash() {
        return hash;
    }

    String salt() {
        return salt;
    }

    int iterations() {
        return iterations;
    }

    private static PasswordCredential create(
            String passwordProof,
            byte[] saltBytes,
            int iterations) {
        String encodedHash = Base64.getEncoder().encodeToString(
                derive(passwordProof, saltBytes, iterations));
        String encodedSalt = Base64.getEncoder().encodeToString(saltBytes);
        return new PasswordCredential(null, encodedHash, encodedSalt, iterations);
    }

    private static byte[] derive(String passwordProof, byte[] salt, int iterations) {
        char[] proofCharacters = passwordProof.toCharArray();
        PBEKeySpec specification = new PBEKeySpec(
                proofCharacters, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is unavailable.", exception);
        } finally {
            specification.clearPassword();
            java.util.Arrays.fill(proofCharacters, '\0');
        }
    }

    private static void requireProof(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "passwordProof must be a lowercase SHA-256 value");
        }
    }
}
