package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;

/** Request for replacing another account's development password proof. */
public final class ResetUserPasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String passwordProof;

    public ResetUserPasswordRequest(String userId, String passwordProof) {
        this.userId = CreateUserAccountRequest.requireText(userId, "userId");
        this.passwordProof = CreateUserAccountRequest.requirePasswordProof(passwordProof);
    }

    public String getUserId() {
        return userId;
    }

    public String getPasswordProof() {
        return passwordProof;
    }
}
