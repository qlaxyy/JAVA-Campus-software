package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.CampusCardNumber;
import edu.seu.vcampus.common.user.PasswordProof;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.UserAuditLogEntry;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/** Offline recovery entry point for a super administrator on the server host. */
public final class LocalSuperAdminRecovery {

    public static final String AUDIT_ACTION =
            "USER.LOCAL_RESET_SUPER_ADMIN_PASSWORD";
    private static final int MINIMUM_PASSWORD_LENGTH = 6;

    private LocalSuperAdminRecovery() {
    }

    /**
     * Resets one super administrator password directly in the server database.
     * The normal server process must be stopped before this method is called.
     *
     * @param databasePath Access database owned by the server
     * @param campusCardNumber super administrator campus-card number
     * @param newPassword caller-owned password characters
     * @throws IllegalArgumentException if the account is missing, is not a super
     *                                  administrator, or the password is too short
     */
    public static void resetPassword(
            Path databasePath,
            String campusCardNumber,
            char[] newPassword) {
        Objects.requireNonNull(databasePath, "databasePath must not be null");
        Objects.requireNonNull(newPassword, "newPassword must not be null");
        if (!CampusCardNumber.isValid(campusCardNumber)) {
            throw new IllegalArgumentException("一卡通号必须是 8 位数字。");
        }
        if (newPassword.length < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("新密码至少需要 6 个字符。");
        }

        String normalized = CampusCardNumber.normalize(campusCardNumber);
        AccessDatabase database = new AccessDatabase(databasePath);
        AccessUserRepository users = new AccessUserRepository(database);
        AccessUserAuditRepository auditLogs = new AccessUserAuditRepository(database);
        UserAccount account = users.findByUsername(normalized).orElse(null);
        if (account == null || account.role() != Role.SUPER_ADMIN) {
            appendAudit(auditLogs, normalized, false,
                    "FAILED：目标不存在或不是超级管理员。");
            throw new IllegalArgumentException("目标账号不存在或不是超级管理员。");
        }

        char[] passwordCopy = Arrays.copyOf(newPassword, newPassword.length);
        try {
            String proof = PasswordProof.create(account.username(), passwordCopy);
            users.save(account.withPasswordProof(proof));
            appendAudit(auditLogs, normalized, true,
                    "OK：服务器本地紧急重置超级管理员密码。");
        } finally {
            Arrays.fill(passwordCopy, '\0');
        }
    }

    private static void appendAudit(
            AccessUserAuditRepository auditLogs,
            String target,
            boolean successful,
            String detail) {
        auditLogs.append(new UserAuditLogEntry(
                UUID.randomUUID().toString(),
                Clock.systemUTC().millis(),
                "LOCAL-SERVER-MAINTENANCE",
                "LOCAL",
                "服务器本地维护",
                AUDIT_ACTION,
                target,
                successful,
                detail));
    }
}
