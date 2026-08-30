package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Account-list payload returned to the super-administrator client. */
public final class UserAccountListResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<UserAccountView> accounts;

    public UserAccountListResponse(List<UserAccountView> accounts) {
        this.accounts = List.copyOf(Objects.requireNonNull(accounts, "accounts must not be null"));
    }

    public List<UserAccountView> getAccounts() {
        return accounts;
    }
}
