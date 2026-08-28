package edu.seu.vcampus.client.application;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleAccessPolicyTest {

    @Test
    void onlySuperAdminSeesUserManagement() {
        assertTrue(ModuleAccessPolicy.isVisible(
                session(Role.SUPER_ADMIN, EnumSet.allOf(AdminScope.class)), ModuleNames.USER));
        assertFalse(ModuleAccessPolicy.isVisible(
                session(Role.MODULE_ADMIN, Set.of(AdminScope.HOSPITAL)), ModuleNames.USER));
        assertFalse(ModuleAccessPolicy.isVisible(
                session(Role.STUDENT, Set.of()), ModuleNames.USER));
    }

    @Test
    void subsystemAdminOnlySeesAssignedBusinessModules() {
        SessionInfo session = session(Role.MODULE_ADMIN, Set.of(AdminScope.HOSPITAL));

        assertTrue(ModuleAccessPolicy.isVisible(session, ModuleNames.HOSPITAL));
        assertFalse(ModuleAccessPolicy.isVisible(session, ModuleNames.SHOP));
        assertFalse(ModuleAccessPolicy.isVisible(session, ModuleNames.COMMON));
        assertFalse(ModuleAccessPolicy.isVisible(session, "UNKNOWN"));
    }

    private static SessionInfo session(Role role, Set<AdminScope> scopes) {
        return new SessionInfo(
                "token-value", "user-id", "username", "display name", role, scopes);
    }
}
