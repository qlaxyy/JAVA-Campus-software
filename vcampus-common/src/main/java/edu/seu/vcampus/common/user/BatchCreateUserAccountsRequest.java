package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Request for atomically creating multiple regular accounts. */
public final class BatchCreateUserAccountsRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int MAX_ACCOUNTS = 1_000;

    private final List<CreateUserAccountRequest> accounts;

    public BatchCreateUserAccountsRequest(List<CreateUserAccountRequest> accounts) {
        Objects.requireNonNull(accounts, "accounts must not be null");
        if (accounts.isEmpty() || accounts.size() > MAX_ACCOUNTS) {
            throw new IllegalArgumentException(
                    "accounts must contain between 1 and " + MAX_ACCOUNTS + " items");
        }
        this.accounts = List.copyOf(accounts);
    }

    public List<CreateUserAccountRequest> getAccounts() {
        return accounts;
    }
}
