package edu.seu.vcampus.server.module;

import edu.seu.vcampus.server.security.SessionLookup;

import java.util.Objects;

/**
 * Shared server services exposed to business modules without coupling their DAOs.
 */
public final class ServerContext {

    private final SessionLookup sessions;

    /**
     * Creates a server module context.
     *
     * @param sessions read-only session lookup
     */
    public ServerContext(SessionLookup sessions) {
        this.sessions = Objects.requireNonNull(sessions, "sessions must not be null");
    }

    /** @return read-only session lookup shared by all modules */
    public SessionLookup sessions() {
        return sessions;
    }
}
