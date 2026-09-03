package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.UserAccountView;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable server-side account record. Password proof is never exposed to clients. */
final class UserAccount {

    private final String userId;
    private final String username;
    private final String displayName;
    private final Role role;
    private final Set<AdminScope> adminScopes;
    private final String passwordProof;
    private final boolean enabled;

    UserAccount(
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> adminScopes,
            String passwordProof,
            boolean enabled) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(adminScopes, "adminScopes must not be null");
        EnumSet<AdminScope> scopes = adminScopes.isEmpty()
                ? EnumSet.noneOf(AdminScope.class)
                : EnumSet.copyOf(adminScopes);
        this.adminScopes = Collections.unmodifiableSet(scopes);
        this.passwordProof = Objects.requireNonNull(passwordProof, "passwordProof must not be null");
        this.enabled = enabled;
    }

    String userId() { return userId; }
    String username() { return username; }
    String displayName() { return displayName; }
    Role role() { return role; }
    Set<AdminScope> adminScopes() { return adminScopes; }
    String passwordProof() { return passwordProof; }
    boolean enabled() { return enabled; }

    UserAccount withProfile(String newDisplayName, Set<AdminScope> newScopes) {
        Set<AdminScope> effectiveScopes = role == Role.SUPER_ADMIN
                ? EnumSet.allOf(AdminScope.class) : newScopes;
        return new UserAccount(userId, username, newDisplayName, role,
                effectiveScopes, passwordProof, enabled);
    }

    UserAccount withEnabled(boolean newEnabled) {
        return new UserAccount(userId, username, displayName, role,
                adminScopes, passwordProof, newEnabled);
    }

    UserAccount withPasswordProof(String newPasswordProof) {
        return new UserAccount(userId, username, displayName, role,
                adminScopes, newPasswordProof, enabled);
    }

    UserAccountView toView() {
        return new UserAccountView(
                userId, username, displayName, role, adminScopes, enabled);
    }
}
