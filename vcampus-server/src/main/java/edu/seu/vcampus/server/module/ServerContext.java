package edu.seu.vcampus.server.module;

import edu.seu.vcampus.server.security.SessionLookup;
import edu.seu.vcampus.server.security.AccountProvisioning;
import edu.seu.vcampus.server.security.UserDirectory;

import java.util.Objects;

/**
 * Shared server services exposed to business modules without coupling their DAOs.
 */
public final class ServerContext {

    private final SessionLookup sessions;
    private final UserDirectory users;
    private final AccountProvisioning accounts;

    private static final UserDirectory EMPTY_USER_DIRECTORY = new UserDirectory() {
        @Override
        public java.util.Optional<edu.seu.vcampus.server.security.UserIdentity>
                findByUserId(String userId) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<edu.seu.vcampus.server.security.UserIdentity>
                findByCampusCardNumber(String campusCardNumber) {
            return java.util.Optional.empty();
        }
    };

    /**
     * Creates a server module context.
     *
     * @param sessions read-only session lookup
     */
    public ServerContext(SessionLookup sessions) {
        this(sessions, EMPTY_USER_DIRECTORY, new AccountProvisioning() {
            @Override
            public java.util.Optional<edu.seu.vcampus.server.security.ProvisionedAccount>
                    findAccountByUsername(String username) {
                return java.util.Optional.empty();
            }

            @Override
            public edu.seu.vcampus.server.security.ProvisionedAccount
                    createGeneratedRegularAccount(String displayName) {
                throw new IllegalStateException("Account provisioning is unavailable.");
            }
        });
    }

    /** Creates a context with session lookup and approved account provisioning. */
    public ServerContext(SessionLookup sessions, AccountProvisioning accounts) {
        this(sessions,
                accounts instanceof UserDirectory directory
                        ? directory : EMPTY_USER_DIRECTORY,
                accounts);
    }

    /** Creates a context with session, read-only identity, and provisioning services. */
    public ServerContext(
            SessionLookup sessions,
            UserDirectory users,
            AccountProvisioning accounts) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
        this.users = Objects.requireNonNull(users, "users must not be null");
        this.accounts = Objects.requireNonNull(accounts, "accounts must not be null");
    }

    /** @return read-only session lookup shared by all modules */
    public SessionLookup sessions() {
        return sessions;
    }

    /** @return read-only basic identity directory shared by server modules */
    public UserDirectory users() {
        return users;
    }

    /** @return restricted account provisioning used after super-admin approval */
    public AccountProvisioning accounts() {
        return accounts;
    }
}
