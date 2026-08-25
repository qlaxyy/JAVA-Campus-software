package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

import java.util.Objects;

/** Server entry point owned by the user-management module. */
public final class UserServerModule implements ServerModule {

    private final InMemoryAuthenticationService authentication;

    /**
     * Creates the user module with the shared authentication service.
     *
     * @param authentication development authentication service
     */
    public UserServerModule(InMemoryAuthenticationService authentication) {
        this.authentication = Objects.requireNonNull(
                authentication, "authentication must not be null");
    }

    @Override
    public String id() {
        return ModuleNames.USER;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(UserActions.LOGIN, this::login);
        router.register(UserActions.LOGOUT, this::logout);
        router.register(UserActions.CURRENT_SESSION, this::currentSession);
    }

    private Response login(Request request) {
        if (!(request.getData() instanceof LoginRequest loginRequest)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "Login data is invalid.");
        }
        return authentication.login(loginRequest)
                .map(session -> Response.success(request, "Login succeeded.", session))
                .orElseGet(() -> Response.failure(
                        request.getRequestId(),
                        ErrorCodes.AUTH_INVALID_CREDENTIALS,
                        "Username or password is incorrect."));
    }

    private Response logout(Request request) {
        if (!authentication.logout(request.getToken())) {
            return authenticationRequired(request);
        }
        return Response.success(request, "Logout succeeded.", null);
    }

    private Response currentSession(Request request) {
        return authentication.findSession(request.getToken())
                .map(session -> Response.success(request, "Session is valid.", session))
                .orElseGet(() -> authenticationRequired(request));
    }

    private Response authenticationRequired(Request request) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in first.");
    }
}
