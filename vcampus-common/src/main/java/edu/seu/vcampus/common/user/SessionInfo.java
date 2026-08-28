package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal authenticated session shared with client modules.
 */
public final class SessionInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private final String token;
    private final String userId;
    private final String username;
    private final String displayName;
    private final Role role;
    private final Set<AdminScope> adminScopes;

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
        this(token, userId, username, displayName, role, defaultScopes(role));
    }

    /**
     * Creates immutable session information with explicit subsystem scopes.
     *
     * @param token random session token
     * @param userId stable user identifier
     * @param username login name
     * @param displayName safe display name
     * @param role current role
     * @param adminScopes modules managed by a subsystem administrator
     */
    public SessionInfo(
            String token,
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> adminScopes) {
        this.token = requireText(token, "token");
        this.userId = requireText(userId, "userId");
        this.username = requireText(username, "username");
        this.displayName = requireText(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.adminScopes = validateScopes(role, adminScopes);
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

    /** @return immutable subsystem scopes granted to this session */
    public Set<AdminScope> getAdminScopes() {
        return adminScopes;
    }

    /**
     * Checks server-authoritative administrative access for a business module.
     *
     * @param moduleId stable module identifier
     * @return whether this identity may perform that module's admin operations
     */
    public boolean canAdminister(String moduleId) {
        if (role == Role.SUPER_ADMIN) {
            return AdminScope.fromModuleId(moduleId).isPresent();
        }
        return AdminScope.fromModuleId(moduleId).map(adminScopes::contains).orElse(false);
    }

    /** @return whether this identity may manage accounts and administrator grants */
    public boolean canManageUsers() {
        return role == Role.SUPER_ADMIN;
    }

    private static Set<AdminScope> defaultScopes(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        return role == Role.SUPER_ADMIN
                ? EnumSet.allOf(AdminScope.class)
                : EnumSet.noneOf(AdminScope.class);
    }

    private static Set<AdminScope> validateScopes(Role role, Set<AdminScope> scopes) {
        Objects.requireNonNull(scopes, "adminScopes must not be null");
        EnumSet<AdminScope> copy = scopes.isEmpty()
                ? EnumSet.noneOf(AdminScope.class)
                : EnumSet.copyOf(scopes);

        if (role == Role.SUPER_ADMIN && !copy.equals(EnumSet.allOf(AdminScope.class))) {
            throw new IllegalArgumentException("SUPER_ADMIN must have all admin scopes");
        }
        return Collections.unmodifiableSet(copy);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
