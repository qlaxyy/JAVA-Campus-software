package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;

/** Request for the current user to replace their own password. */
public final class ChangePasswordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String currentPasswordProof;
    private final String newPasswordProof;

    public ChangePasswordRequest(String currentPasswordProof, String newPasswordProof) {
        this.currentPasswordProof =
                CreateUserAccountRequest.requirePasswordProof(currentPasswordProof);
        this.newPasswordProof =
                CreateUserAccountRequest.requirePasswordProof(newPasswordProof);
    }

    public String getCurrentPasswordProof() {
        return currentPasswordProof;
    }

    public String getNewPasswordProof() {
        return newPasswordProof;
    }
}
