package edu.seu.vcampus.client.application;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.SessionInfo;

import java.util.Objects;

/**
 * Navigation-only policy. Server modules must independently authorize every request.
 */
public final class ModuleAccessPolicy {

    private ModuleAccessPolicy() {
    }

    /**
     * Determines whether a module tile should be shown for the current identity.
     *
     * @param session authenticated session
     * @param moduleId stable client-module identifier
     * @return whether the tile should be visible
     */
    public static boolean isVisible(SessionInfo session, String moduleId) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(moduleId, "moduleId must not be null");

        if (!ModuleNames.isSupported(moduleId) || ModuleNames.COMMON.equals(moduleId)) {
            return false;
        }
        if (ModuleNames.USER.equals(moduleId)) {
            return session.canManageUsers();
        }
        return true;
    }
}
