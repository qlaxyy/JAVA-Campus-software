package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.user.UserAuditLogEntry;

import java.util.ArrayList;
import java.util.List;

/** Thread-safe audit storage used by tests and the non-persistent demo router. */
final class InMemoryUserAuditRepository implements UserAuditRepository {

    private final List<UserAuditLogEntry> entries = new ArrayList<>();

    @Override
    public synchronized void append(UserAuditLogEntry entry) {
        entries.add(entry);
    }

    @Override
    public synchronized List<UserAuditLogEntry> findAll() {
        List<UserAuditLogEntry> result = new ArrayList<>(entries.size());
        for (int index = entries.size() - 1; index >= 0; index--) {
            result.add(entries.get(index));
        }
        return List.copyOf(result);
    }
}
