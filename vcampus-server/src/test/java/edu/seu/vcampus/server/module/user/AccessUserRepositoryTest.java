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
        assertEquals(8, restarted.users().findAll().size());
        assertFalse(restarted.users().findByUsername("20260001").orElseThrow().enabled());
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

    private AccessUserRepository repository(Path path) {
        return new AccessUserRepository(new AccessDatabase(path));
    }

    private UserAccount account(String userId, String username) {
        return new UserAccount(
                userId, username, "批量测试", Role.USER,
                Set.of(), "0".repeat(64), true);
    }
}
