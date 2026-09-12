package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.UserAccountView;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAdminPanelFilterTest {

    private final UserAccountView student = account(
            "U-STUDENT", "20260006", "周一", Role.USER, Set.of(), true);
    private final UserAccountView courseAdministrator = account(
            "U-COURSE", "20260002", "选课管理员", Role.USER,
            Set.of(AdminScope.COURSE), true);
    private final UserAccountView disabledAccount = account(
            "U-DISABLED", "20260011", "吴上祥", Role.USER, Set.of(), false);
    private final UserAccountView superAdministrator = account(
            "U-ADMIN", "20260000", "演示超级管理员", Role.SUPER_ADMIN,
            Set.of(), true);

    @Test
    void searchesCampusCardNumberAndDisplayName() {
        assertTrue(matches(student, "60006", "全部状态", "全部管理范围"));
        assertTrue(matches(disabledAccount, "吴上", "全部状态", "全部管理范围"));
        assertFalse(matches(student, "医生", "全部状态", "全部管理范围"));
    }

    @Test
    void combinesStatusAndAdministrationScopeFilters() {
        assertTrue(matches(disabledAccount, "", "禁用", "无管理权"));
        assertFalse(matches(student, "", "禁用", "无管理权"));
        assertTrue(matches(courseAdministrator, "", "启用", "选课"));
        assertFalse(matches(student, "", "启用", "选课"));
        assertTrue(matches(superAdministrator, "", "启用", "超级管理员"));
    }

    private static boolean matches(
            UserAccountView account,
            String query,
            String status,
            String scope) {
        return UserAdminPanel.matchesAccount(account, query, status, scope);
    }

    private static UserAccountView account(
            String userId,
            String campusCardNumber,
            String displayName,
            Role role,
            Set<AdminScope> scopes,
            boolean enabled) {
        return new UserAccountView(
                userId, campusCardNumber, displayName, role, scopes, enabled);
    }
}
