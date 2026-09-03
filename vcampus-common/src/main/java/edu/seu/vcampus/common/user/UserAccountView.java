package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Safe account information shown in the super-administrator client. */
public final class UserAccountView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String username;
    private final String displayName;
    private final Role role;
    private final Set<AdminScope> adminScopes;
    private final boolean enabled;

    public UserAccountView(
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> adminScopes,
            boolean enabled) {
        this.userId = requireText(userId, "userId");
        this.username = requireText(username, "username");
        this.displayName = requireText(displayName, "displayName");
        this.role = Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(adminScopes, "adminScopes must not be null");
        EnumSet<AdminScope> scopes = adminScopes.isEmpty()
                ? EnumSet.noneOf(AdminScope.class)
                : EnumSet.copyOf(adminScopes);
        this.adminScopes = Collections.unmodifiableSet(scopes);
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    public Set<AdminScope> getAdminScopes() {
        return adminScopes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
