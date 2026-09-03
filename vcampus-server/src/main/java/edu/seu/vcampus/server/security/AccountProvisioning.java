package edu.seu.vcampus.server.security;

import java.util.Optional;

/**
 * Restricted internal account operation used only after a super-administrator
 * approves a subsystem onboarding request.
 */
public interface AccountProvisioning {
    /** Finds an account only when the hospital explicitly chose the existing-account path. */
    Optional<ProvisionedAccount> findAccountByUsername(String username);

    /** Creates a new regular account with a server-generated unique login name. */
    ProvisionedAccount createGeneratedRegularAccount(String usernamePrefix, String displayName);
}
