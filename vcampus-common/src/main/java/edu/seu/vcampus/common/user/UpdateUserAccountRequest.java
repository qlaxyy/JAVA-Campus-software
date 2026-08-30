package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/** Request for changing an account display name and subsystem scopes. */
public final class UpdateUserAccountRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String displayName;
    private final Set<AdminScope> adminScopes;

    public UpdateUserAccountRequest(
            String userId,
            String displayName,
            Set<AdminScope> adminScopes) {
        this.userId = CreateUserAccountRequest.requireText(userId, "userId");
        this.displayName = CreateUserAccountRequest.requireText(displayName, "displayName");
        this.adminScopes = CreateUserAccountRequest.immutableScopes(adminScopes);
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<AdminScope> getAdminScopes() {
        return adminScopes;
    }
}
