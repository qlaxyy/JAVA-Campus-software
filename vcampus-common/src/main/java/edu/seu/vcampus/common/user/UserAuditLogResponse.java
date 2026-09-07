package edu.seu.vcampus.common.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Recent account-management audit records returned to a super administrator. */
public final class UserAuditLogResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<UserAuditLogEntry> entries;

    public UserAuditLogResponse(List<UserAuditLogEntry> entries) {
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries must not be null"));
    }

    public List<UserAuditLogEntry> getEntries() {
        return entries;
    }
}
