package edu.seu.vcampus.server.security;

import edu.seu.vcampus.common.user.SessionInfo;

import java.util.Optional;

/**
 * Read-only session service available to every server business module.
 */
@FunctionalInterface
public interface SessionLookup {

    /**
     * Finds an authenticated session by token.
     *
     * @param token request token, possibly {@code null}
     * @return session when the token is valid
     */
    Optional<SessionInfo> findSession(String token);
}
