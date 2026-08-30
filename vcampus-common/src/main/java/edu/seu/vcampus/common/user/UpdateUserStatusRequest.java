package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;

/** Request for enabling or disabling an account without deleting it. */
public final class UpdateUserStatusRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final boolean enabled;

    public UpdateUserStatusRequest(String userId, boolean enabled) {
        this.userId = CreateUserAccountRequest.requireText(userId, "userId");
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
