package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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
                "U-TEST-001", "test001", "测试用户", Role.USER,
                Set.of(AdminScope.COURSE, AdminScope.LIBRARY),
                "0".repeat(64), true);

        repository.save(account);
        repository.save(account.withProfile(
                "修改后的用户", Set.of(AdminScope.HOSPITAL)));

        UserAccount persisted = repository(databasePath)
                .findByUsername("test001").orElseThrow();
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
        UserAccount student = first.users().findByUsername("student001").orElseThrow();
        first.users().save(student.withEnabled(false));

        InMemoryAuthenticationService restarted =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        assertEquals(8, restarted.users().findAll().size());
        assertFalse(restarted.users().findByUsername("student001").orElseThrow().enabled());
    }

    @Test
    void batchWriteRollsBackWhenOneAccountViolatesUniqueUsername() {
        Path databasePath = temporaryDirectory.resolve("batch.accdb");
        AccessUserRepository repository = repository(databasePath);
        UserAccount first = account("U-BATCH-001", "sameuser");
        UserAccount duplicate = account("U-BATCH-002", "sameuser");

        assertThrows(UserPersistenceException.class,
                () -> repository.saveAll(List.of(first, duplicate)));

        assertTrue(repository.findAll().isEmpty());
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
