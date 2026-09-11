package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.UserAuditLogEntry;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSuperAdminRecoveryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resetsOnlySuperAdministratorAndWritesAuditRecord() {
        Path databasePath = temporaryDirectory.resolve("recovery.accdb");
        InMemoryAuthenticationService before =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        assertTrue(login(before, "20260000", "123456"));

        char[] newPassword = "new-local-password".toCharArray();
        try {
            LocalSuperAdminRecovery.resetPassword(
                    databasePath, "20260000", newPassword);
        } finally {
            Arrays.fill(newPassword, '\0');
        }

        InMemoryAuthenticationService after =
                UserAuthenticationBootstrap.createAccessBacked(databasePath);
        assertFalse(login(after, "20260000", "123456"));
        assertTrue(login(after, "20260000", "new-local-password"));
        UserAuditLogEntry audit = new AccessUserAuditRepository(
                new AccessDatabase(databasePath)).findAll().get(0);
        assertTrue(audit.isSuccessful());
        assertTrue(LocalSuperAdminRecovery.AUDIT_ACTION.equals(audit.getActionCode()));

        char[] attemptedPassword = "not-allowed".toCharArray();
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> LocalSuperAdminRecovery.resetPassword(
                            databasePath, "20260001", attemptedPassword));
        } finally {
            Arrays.fill(attemptedPassword, '\0');
        }
        UserAuditLogEntry rejectedAudit = new AccessUserAuditRepository(
                new AccessDatabase(databasePath)).findAll().get(0);
        assertFalse(rejectedAudit.isSuccessful());
    }

    private static boolean login(
            InMemoryAuthenticationService authentication,
            String campusCardNumber,
            String passwordText) {
        char[] password = passwordText.toCharArray();
        try {
            return authentication.login(new LoginRequest(
                    campusCardNumber,
                    PasswordProof.create(campusCardNumber, password))).isPresent();
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
