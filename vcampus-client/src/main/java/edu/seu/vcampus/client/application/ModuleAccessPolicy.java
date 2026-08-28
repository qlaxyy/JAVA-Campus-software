package edu.seu.vcampus.client.application;

import edu.seu.vcampus.client.module.ModuleViewMode;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;

import java.util.Objects;
import java.util.Optional;

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
        return modeFor(session, moduleId).isPresent();
    }

    /**
     * Selects normal or management presentation for one module.
     *
     * @param session authenticated session
     * @param moduleId stable module identifier
     * @return empty when the module must not be shown
     */
    public static Optional<ModuleViewMode> modeFor(SessionInfo session, String moduleId) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(moduleId, "moduleId must not be null");

        if (!ModuleNames.isSupported(moduleId) || ModuleNames.COMMON.equals(moduleId)) {
            return Optional.empty();
        }
        if (ModuleNames.USER.equals(moduleId)) {
            return session.canManageUsers()
                    ? Optional.of(ModuleViewMode.MANAGEMENT)
                    : Optional.empty();
        }
        if (session.getRole() == Role.SUPER_ADMIN) {
            return Optional.of(ModuleViewMode.MANAGEMENT);
        }
        if (session.getRole() == Role.MODULE_ADMIN) {
            return session.canAdminister(moduleId)
                    ? Optional.of(ModuleViewMode.MANAGEMENT)
                    : Optional.empty();
        }
        return Optional.of(ModuleViewMode.USER);
    }
}
