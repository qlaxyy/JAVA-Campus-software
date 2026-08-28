package edu.seu.vcampus.server.security;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationModelTest {

    @Test
    void superAdminCanManageUsersAndEveryBusinessModule() {
        SessionInfo session = session(Role.SUPER_ADMIN, EnumSet.allOf(AdminScope.class));

        assertTrue(session.canManageUsers());
        assertTrue(session.canAdminister(ModuleNames.COURSE));
        assertTrue(session.canAdminister(ModuleNames.HOSPITAL));
        assertFalse(session.canAdminister(ModuleNames.USER));
    }

    @Test
    void moduleAdminIsLimitedToAssignedScopes() {
        SessionInfo session = session(Role.MODULE_ADMIN, Set.of(AdminScope.HOSPITAL));

        assertFalse(session.canManageUsers());
        assertTrue(session.canAdminister(ModuleNames.HOSPITAL));
        assertFalse(session.canAdminister(ModuleNames.COURSE));
    }

    @Test
    void invalidRoleAndScopeCombinationsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> session(Role.MODULE_ADMIN, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> session(Role.STUDENT, Set.of(AdminScope.HOSPITAL)));
    }

    private static SessionInfo session(Role role, Set<AdminScope> scopes) {
        return new SessionInfo(
                "token-value", "user-id", "username", "display name", role, scopes);
    }
}
