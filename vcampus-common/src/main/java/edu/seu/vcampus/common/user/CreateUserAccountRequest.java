package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Request for creating a regular account and assigning subsystem scopes. */
public final class CreateUserAccountRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String displayName;
    private final String passwordProof;
    private final Set<AdminScope> adminScopes;

    public CreateUserAccountRequest(
            String username,
            String displayName,
            String passwordProof,
            Set<AdminScope> adminScopes) {
        this.username = normalizeUsername(username);
        this.displayName = requireText(displayName, "displayName");
        this.passwordProof = requirePasswordProof(passwordProof);
        this.adminScopes = immutableScopes(adminScopes);
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordProof() {
        return passwordProof;
    }

    public Set<AdminScope> getAdminScopes() {
        return adminScopes;
    }

    static String normalizeUsername(String value) {
        Objects.requireNonNull(value, "username must not be null");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]{3,32}")) {
            throw new IllegalArgumentException("username format is invalid");
        }
        return normalized;
    }

    static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException(fieldName + " format is invalid");
        }
        return normalized;
    }

    static String requirePasswordProof(String value) {
        Objects.requireNonNull(value, "passwordProof must not be null");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("passwordProof must be a lowercase SHA-256 value");
        }
        return value;
    }

    static Set<AdminScope> immutableScopes(Set<AdminScope> value) {
        Objects.requireNonNull(value, "adminScopes must not be null");
        EnumSet<AdminScope> scopes = value.isEmpty()
                ? EnumSet.noneOf(AdminScope.class)
                : EnumSet.copyOf(value);
        return Collections.unmodifiableSet(scopes);
    }
}
