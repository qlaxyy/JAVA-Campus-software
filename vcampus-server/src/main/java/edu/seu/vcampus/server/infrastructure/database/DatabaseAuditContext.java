package edu.seu.vcampus.server.infrastructure.database;

import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.user.SessionInfo;

/** Request-local identity snapshots, never tokens or request payloads. */
public final class DatabaseAuditContext implements AutoCloseable {
    private static final ThreadLocal<Identity> CURRENT = new ThreadLocal<>();
    private final Identity previous;

    private DatabaseAuditContext(Identity identity) {
        previous = CURRENT.get();
        CURRENT.set(identity);
    }

    public static DatabaseAuditContext open(Request request, SessionInfo actor) {
        return new DatabaseAuditContext(actor == null
                ? new Identity("ANONYMOUS", "ANONYMOUS", "未登录请求", request.getAction(), request.getRequestId())
                : new Identity(actor.getUserId(), actor.getUsername(), actor.getDisplayName(),
                        request.getAction(), request.getRequestId()));
    }

    static Identity current() {
        Identity identity = CURRENT.get();
        return identity == null ? new Identity("SYSTEM", "SYSTEM", "服务器", "SYSTEM.INITIALIZE", "LOCAL")
                : identity;
    }

    @Override public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    record Identity(String userId, String number, String name, String action, String requestId) { }
}
