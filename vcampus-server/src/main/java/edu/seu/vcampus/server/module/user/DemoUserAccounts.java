package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/** Creates the public development accounts used by local development and tests. */
final class DemoUserAccounts {

    private DemoUserAccounts() {
    }

    static InMemoryUserRepository createRepository() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        seedIfEmpty(repository);
        return repository;
    }

    /** Seeds a newly created repository without overwriting persisted changes. */
    static void seedIfEmpty(UserRepository repository) {
        if (!repository.findAll().isEmpty()) {
            return;
        }
        repository.save(account("U-STUDENT-001", "20260001", "演示学生",
                Role.USER, Set.of()));
        repository.save(account("U-TEACHER-001", "20260002", "演示教师",
                Role.USER, Set.of()));
        repository.save(account("U-ADMIN-001", "20260003", "演示超级管理员",
                Role.SUPER_ADMIN, EnumSet.allOf(AdminScope.class)));
        repository.save(account("U-STUDENT-ADMIN-001", "20260004", "演示学籍管理员",
                Role.USER, Set.of(AdminScope.STUDENT)));
        repository.save(account("U-COURSE-ADMIN-001", "20260005", "演示选课管理员",
                Role.USER, Set.of(AdminScope.COURSE)));
        repository.save(account("U-LIBRARY-ADMIN-001", "20260006", "演示图书馆管理员",
                Role.USER, Set.of(AdminScope.LIBRARY)));
        repository.save(account("U-SHOP-ADMIN-001", "20260007", "演示商店管理员",
                Role.USER, Set.of(AdminScope.SHOP)));
        repository.save(account("U-HOSPITAL-ADMIN-001", "20260008", "演示医院管理员",
                Role.USER, Set.of(AdminScope.HOSPITAL)));
    }

    private static UserAccount account(
            String userId,
            String username,
            String displayName,
            Role role,
            Set<AdminScope> scopes) {
        char[] password = "123456".toCharArray();
        try {
            return new UserAccount(
                    userId, username, displayName, role, scopes,
                    PasswordProof.create(username, password), true);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
