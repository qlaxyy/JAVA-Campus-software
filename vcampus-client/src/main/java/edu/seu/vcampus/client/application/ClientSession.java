package edu.seu.vcampus.client.application;

import edu.seu.vcampus.common.user.SessionInfo;

import java.util.Optional;

/**
 * Holds the current authenticated session for all client modules.
 */
public final class ClientSession {

    private volatile SessionInfo sessionInfo;

    /**
     * Stores a successful authenticated session.
     *
     * @param sessionInfo session returned by the server
     */
    public void set(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    /** Clears the local session after logout or authentication failure. */
    public void clear() {
        sessionInfo = null;
    }

    /** @return current session when logged in */
    public Optional<SessionInfo> current() {
        return Optional.ofNullable(sessionInfo);
    }

    /** @return current token, or {@code null} before login */
    public String tokenOrNull() {
        SessionInfo current = sessionInfo;
        return current == null ? null : current.getToken();
    }
}
