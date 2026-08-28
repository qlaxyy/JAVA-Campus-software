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

    /**
     * Checks a token against the server-side session before an admin operation.
     * Client-side navigation filtering is never a substitute for this check.
     *
     * @param token request token
     * @param moduleId business module being administered
     * @return whether the authenticated identity has the required scope
     */
    default boolean canAdminister(String token, String moduleId) {
        return findSession(token)
                .map(session -> session.canAdminister(moduleId))
                .orElse(false);
    }

    /**
     * Checks whether a token belongs to the super administrator responsible
     * for accounts, roles and subsystem grants.
     *
     * @param token request token
     * @return whether the authenticated identity may manage users
     */
    default boolean canManageUsers(String token) {
        return findSession(token)
                .map(SessionInfo::canManageUsers)
                .orElse(false);
    }
}
