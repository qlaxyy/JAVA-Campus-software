package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/** Request for creating a regular account with a server-generated campus-card number. */
public final class CreateGeneratedUserAccountRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String displayName;
    private final Set<AdminScope> adminScopes;

    public CreateGeneratedUserAccountRequest(String displayName, Set<AdminScope> adminScopes) {
        this.displayName = CreateUserAccountRequest.requireText(displayName, "displayName");
        this.adminScopes = CreateUserAccountRequest.immutableScopes(adminScopes);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<AdminScope> getAdminScopes() {
        return adminScopes;
    }
}
