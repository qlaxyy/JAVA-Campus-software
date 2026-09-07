package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.UserAuditLogEntry;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessUserAuditRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void auditRecordSurvivesRepositoryRestart() {
        Path databasePath = temporaryDirectory.resolve("audit.accdb");
        AccessUserAuditRepository first = repository(databasePath);
        first.append(new UserAuditLogEntry(
                "AUDIT-001",
                1_788_704_400_000L,
                "U-ADMIN-001",
                "20260000",
                "演示超级管理员",
                "USER.ADMIN_UPDATE_STATUS",
                "20260001",
                true,
                "OK：账号状态已更新。"));

        UserAuditLogEntry restored = repository(databasePath).findAll().get(0);

        assertEquals("AUDIT-001", restored.getAuditId());
        assertEquals("20260000", restored.getActorUsername());
        assertEquals("20260001", restored.getTarget());
        assertTrue(restored.isSuccessful());
    }

    private AccessUserAuditRepository repository(Path databasePath) {
        return new AccessUserAuditRepository(new AccessDatabase(databasePath));
    }
}
