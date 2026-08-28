package edu.seu.vcampus.common.user;

import edu.seu.vcampus.common.protocol.ModuleNames;

import java.util.Arrays;
import java.util.Optional;

/**
 * Business modules that may be administered by a subsystem administrator.
 */
public enum AdminScope {
    STUDENT(ModuleNames.STUDENT),
    COURSE(ModuleNames.COURSE),
    LIBRARY(ModuleNames.LIBRARY),
    SHOP(ModuleNames.SHOP),
    HOSPITAL(ModuleNames.HOSPITAL);

    private final String moduleId;

    AdminScope(String moduleId) {
        this.moduleId = moduleId;
    }

    /** @return module identifier used by actions and client modules */
    public String moduleId() {
        return moduleId;
    }

    /**
     * Resolves an administrative scope from a module identifier.
     *
     * @param moduleId stable module identifier
     * @return matching scope; USER and COMMON are deliberately not scopes
     */
    public static Optional<AdminScope> fromModuleId(String moduleId) {
        return Arrays.stream(values())
                .filter(scope -> scope.moduleId.equals(moduleId))
                .findFirst();
    }
}
