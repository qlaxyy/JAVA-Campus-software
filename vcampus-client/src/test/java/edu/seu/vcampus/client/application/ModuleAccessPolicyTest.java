package edu.seu.vcampus.client.application;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleAccessPolicyTest {

    @Test
    void onlySuperAdminSeesUserManagement() {
        SessionInfo superAdmin = new SessionInfo(
                "token-value", "user-id", "username", "display name", Role.SUPER_ADMIN);
        assertTrue(ModuleAccessPolicy.isVisible(superAdmin, ModuleNames.USER));
        assertFalse(ModuleAccessPolicy.isVisible(
                session(Role.STUDENT, Set.of(AdminScope.HOSPITAL)), ModuleNames.USER));
        assertFalse(ModuleAccessPolicy.isVisible(
                session(Role.STUDENT, Set.of()), ModuleNames.USER));
    }

    @Test
    void subsystemAdminStillSeesOrdinaryBusinessModules() {
        SessionInfo session = session(Role.STUDENT, Set.of(AdminScope.HOSPITAL));

        assertTrue(ModuleAccessPolicy.isVisible(session, ModuleNames.HOSPITAL));
        assertTrue(ModuleAccessPolicy.isVisible(session, ModuleNames.SHOP));
        assertFalse(ModuleAccessPolicy.isVisible(session, ModuleNames.COMMON));
        assertFalse(ModuleAccessPolicy.isVisible(session, "UNKNOWN"));
    }

    @Test
    void studentsAndTeachersSeeBusinessModules() {
        assertTrue(ModuleAccessPolicy.isVisible(
                session(Role.STUDENT, Set.of()), ModuleNames.HOSPITAL));
        assertTrue(ModuleAccessPolicy.isVisible(
                session(Role.TEACHER, Set.of()), ModuleNames.COURSE));
    }

    private static SessionInfo session(Role role, Set<AdminScope> scopes) {
        return new SessionInfo(
                "token-value", "user-id", "username", "display name", role, scopes);
    }
}
