package edu.seu.vcampus.server.infrastructure;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.database.DatabaseAuditContext;
import edu.seu.vcampus.server.security.SessionLookup;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe action registry used by all server modules.
 */
public final class ActionRouter {

    private final ConcurrentMap<String, RequestHandler> handlers = new ConcurrentHashMap<>();
    private SessionLookup auditSessions = token -> java.util.Optional.empty();

    /** Shares the server-authoritative identity with the JDBC audit boundary. */
    public void configureDatabaseAudit(SessionLookup sessions) {
        auditSessions = Objects.requireNonNull(sessions);
    }

    /**
     * Registers the only handler allowed to own an action name.
     *
     * @param action public action name
     * @param handler handler implementation
     */
    public void register(String action, RequestHandler handler) {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        RequestHandler previous = handlers.putIfAbsent(action, handler);
        if (previous != null) {
            throw new IllegalStateException("duplicate action registration: " + action);
        }
    }

    /**
     * Routes a request without exposing server exception details to the client.
     *
     * @param request validated request
     * @return handler result or a standard error response
     */
    public Response dispatch(Request request) {
        Objects.requireNonNull(request, "request must not be null");
        RequestHandler handler = handlers.get(request.getAction());
        if (handler == null) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_UNKNOWN_ACTION,
                    "Unknown action: " + request.getAction());
        }
        try (var audit = DatabaseAuditContext.open(request,
                auditSessions.findSession(request.getToken()).orElse(null))) {
            return Objects.requireNonNull(handler.handle(request), "handler response must not be null");
        } catch (RuntimeException exception) {
            System.err.printf("[%s] action %s failed: %s%n",
                    request.getRequestId(),
                    request.getAction(),
                    exception.getMessage());
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_SERVER_ERROR,
                    "The server could not complete the request.");
        }
    }
}
