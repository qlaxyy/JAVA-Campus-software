package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.UserAuditLogEntry;

import java.util.List;

/** Append-only storage for account-management audit records. */
interface UserAuditRepository {

    void append(UserAuditLogEntry entry);

    List<UserAuditLogEntry> findAll();
}
