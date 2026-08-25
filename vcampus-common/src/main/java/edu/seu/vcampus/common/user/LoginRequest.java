package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Development-baseline login request. It carries a proof instead of raw password text.
 */
public final class LoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String passwordProof;

    /**
     * Creates a login request.
     *
     * @param username demo account name
     * @param passwordProof lowercase SHA-256 proof created by {@link PasswordProof}
     */
    public LoginRequest(String username, String passwordProof) {
        this.username = normalizeUsername(username);
        this.passwordProof = Objects.requireNonNull(
                passwordProof, "passwordProof must not be null");
        if (!passwordProof.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("passwordProof must be a lowercase SHA-256 value");
        }
    }

    /** @return normalized login name */
    public String getUsername() {
        return username;
    }

    /** @return development login proof, never the raw password */
    public String getPasswordProof() {
        return passwordProof;
    }

    private static String normalizeUsername(String value) {
        Objects.requireNonNull(value, "username must not be null");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{3,32}")) {
            throw new IllegalArgumentException("username format is invalid");
        }
        return normalized;
    }
}
