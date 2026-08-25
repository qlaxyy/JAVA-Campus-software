package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Minimal authenticated session shared with client modules.
 */
public final class SessionInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String token;
    private final String userId;
    private final String username;
    private final String displayName;
    private final Role role;

    /**
     * Creates immutable session information.
     *
     * @param token random session token
     * @param userId stable user identifier
     * @param username login name
     * @param displayName safe display name
     * @param role current baseline role
     */
    public SessionInfo(
            String token,
            String userId,
            String username,
            String displayName,
            Role role) {
        this.token = requireText(token, "token");
        this.userId = requireText(userId, "userId");
        this.username = requireText(username, "username");
        this.displayName = requireText(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role must not be null");
    }

    /** @return random session token */
    public String getToken() {
        return token;
    }

    /** @return stable user identifier */
    public String getUserId() {
        return userId;
    }

    /** @return login name */
    public String getUsername() {
        return username;
    }

    /** @return display name */
    public String getDisplayName() {
        return displayName;
    }

    /** @return baseline role */
    public Role getRole() {
        return role;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
