package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessUserRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndReloadsAccountAndAdminScopes() {
        Path databasePath = temporaryDirectory.resolve("accounts.accdb");
        AccessUserRepository repository = repository(databasePath);
        UserAccount account = new UserAccount(
                "U-TEST-001", "20261001", "测试用户", Role.USER,
                Set.of(AdminScope.COURSE, AdminScope.LIBRARY),
                "0".repeat(64), true);

        repository.save(account);
        repository.save(account.withProfile(
                "修改后的用户", Set.of(AdminScope.HOSPITAL)));

        UserAccount persisted = repository(databasePath)
                .findByUsername("20261001").orElseThrow();
        assertTrue(Files.exists(databasePath));
        assertEquals("修改后的用户", persisted.displayName());
        assertEquals(Set.of(AdminScope.HOSPITAL), persisted.adminScopes());
        assertEquals(1, repository(databasePath).findAll().size());
    }

    @Test
    void seedsDemoAccountsOnlyWhenDatabaseIsEmpty() {
        Path databasePath = temporaryDirectory.resolve("seeded.accdb");
        InMemoryAuthenticationService first =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        UserAccount student = first.users().findByUsername("20260001").orElseThrow();
        first.users().save(student.withEnabled(false));

        InMemoryAuthenticationService restarted =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        assertEquals(9, restarted.users().findAll().size());
        assertFalse(restarted.users().findByUsername("20260001").orElseThrow().enabled());
        UserAccount teacher = restarted.users()
                .findByUsername("20260008").orElseThrow();
        assertEquals("U-COURSE-TEACHER-001", teacher.userId());
        assertEquals("演示教师", teacher.displayName());
        assertEquals(Role.USER, teacher.role());
        assertTrue(teacher.adminScopes().isEmpty());
    }

    @Test
    void batchWriteRollsBackWhenOneAccountViolatesUniqueUsername() {
        Path databasePath = temporaryDirectory.resolve("batch.accdb");
        AccessUserRepository repository = repository(databasePath);
        UserAccount first = account("U-BATCH-001", "20261002");
        UserAccount duplicate = account("U-BATCH-002", "20261002");

        assertThrows(UserPersistenceException.class,
                () -> repository.saveAll(List.of(first, duplicate)));

        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void normalizesAbandonedTeacherAndDoctorRoleValues() throws Exception {
        Path databasePath = temporaryDirectory.resolve("legacy-roles.accdb");
        AccessUserRepository repository = repository(databasePath);
        repository.save(account("U-LEGACY-001", "legacydoctor"));
        try (Connection connection = new AccessDatabase(databasePath).openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE tblUser SET roleCode = 'DOCTOR' WHERE userId = 'U-LEGACY-001'");
        }

        UserAccount migrated = repository(databasePath)
                .findById("U-LEGACY-001").orElseThrow();

        assertEquals(Role.USER, migrated.role());
    }

    @Test
    void migratesLegacyDemoLoginToCampusCardNumberAndKeepsIdentity() {
        Path databasePath = temporaryDirectory.resolve("legacy-login.accdb");
        AccessUserRepository repository = repository(databasePath);
        repository.save(new UserAccount(
                "U-LEGACY-STUDENT", "student001", "演示学生", Role.USER,
                Set.of(), "0".repeat(64), true));

        UserAccount migrated = repository(databasePath)
                .findByUsername("20260001").orElseThrow();

        assertEquals("U-LEGACY-STUDENT", migrated.userId());
        assertEquals("演示学生", migrated.displayName());
        assertEquals(
                PasswordProof.create("20260001", "123456".toCharArray()),
                migrated.passwordProof());
    }

    @Test
    void migratesExistingDemoAccountsToContiguousCampusCardNumbers() {
        Path databasePath = temporaryDirectory.resolve("legacy-admin-number.accdb");
        AccessUserRepository initial = repository(databasePath);
        char[] password = "123456".toCharArray();
        try {
            initial.saveAll(List.of(
                    demoAccount("U-STUDENT-001", "20260001", Role.USER,
                            Set.of(), password),
                    demoAccount("U-TEACHER-001", "20260002", Role.USER,
                            Set.of(), password),
                    demoAccount("U-ADMIN-001", "20260003", Role.SUPER_ADMIN,
                            Set.of(), password),
                    demoAccount("U-STUDENT-ADMIN-001", "20260004", Role.USER,
                            Set.of(AdminScope.STUDENT), password),
                    demoAccount("U-COURSE-ADMIN-001", "20260005", Role.USER,
                            Set.of(AdminScope.COURSE), password),
                    demoAccount("U-LIBRARY-ADMIN-001", "20260006", Role.USER,
                            Set.of(AdminScope.LIBRARY), password),
                    demoAccount("U-SHOP-ADMIN-001", "20260007", Role.USER,
                            Set.of(AdminScope.SHOP), password),
                    demoAccount("U-HOSPITAL-ADMIN-001", "20260008", Role.USER,
                            Set.of(AdminScope.HOSPITAL), password)));
        } finally {
            java.util.Arrays.fill(password, '\0');
        }

        AccessUserRepository migrated = repository(databasePath);

        assertEquals("U-ADMIN-001",
                migrated.findByUsername("20260000").orElseThrow().userId());
        assertEquals("U-STUDENT-001",
                migrated.findByUsername("20260001").orElseThrow().userId());
        assertEquals("U-TEACHER-001",
                migrated.findByUsername("20260002").orElseThrow().userId());
        assertEquals("U-STUDENT-ADMIN-001",
                migrated.findByUsername("20260003").orElseThrow().userId());
        assertEquals("U-COURSE-ADMIN-001",
                migrated.findByUsername("20260004").orElseThrow().userId());
        assertEquals("U-LIBRARY-ADMIN-001",
                migrated.findByUsername("20260005").orElseThrow().userId());
        assertEquals("U-SHOP-ADMIN-001",
                migrated.findByUsername("20260006").orElseThrow().userId());
        UserAccount hospitalAdministrator =
                migrated.findByUsername("20260007").orElseThrow();
        assertEquals("U-HOSPITAL-ADMIN-001", hospitalAdministrator.userId());
        assertEquals(Set.of(AdminScope.HOSPITAL), hospitalAdministrator.adminScopes());
        assertTrue(migrated.findByUsername("20260008").isEmpty());
        char[] migratedPassword = "123456".toCharArray();
        try {
            assertEquals(
                    PasswordProof.create("20260000", migratedPassword),
                    migrated.findByUsername("20260000").orElseThrow().passwordProof());
            assertEquals(
                    PasswordProof.create("20260007", migratedPassword),
                    hospitalAdministrator.passwordProof());
        } finally {
            java.util.Arrays.fill(migratedPassword, '\0');
        }
    }

    private AccessUserRepository repository(Path path) {
        return new AccessUserRepository(new AccessDatabase(path));
    }

    private UserAccount account(String userId, String username) {
        return new UserAccount(
                userId, username, "批量测试", Role.USER,
                Set.of(), "0".repeat(64), true);
    }

    private UserAccount demoAccount(
            String userId,
            String username,
            Role role,
            Set<AdminScope> scopes,
            char[] password) {
        return new UserAccount(
                userId, username, "旧演示账号", role, scopes,
                PasswordProof.create(username, password), true);
    }
}
