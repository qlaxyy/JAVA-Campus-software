package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Server entry point owned by the user-management module. */
public final class UserServerModule implements ServerModule {

    private final InMemoryAuthenticationService authentication;
    private final UserAdministrationService administration;

    /**
     * Creates the user module with the shared authentication service.
     *
     * @param authentication development authentication service
     */
    public UserServerModule(InMemoryAuthenticationService authentication) {
        this.authentication = Objects.requireNonNull(
                authentication, "authentication must not be null");
        this.administration = new UserAdministrationService(
                authentication.users(), authentication);
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
        router.register(UserActions.ADMIN_LIST_ACCOUNTS, this::listAccounts);
        router.register(UserActions.ADMIN_CREATE_ACCOUNT, this::createAccount);
        router.register(UserActions.ADMIN_UPDATE_ACCOUNT, this::updateAccount);
        router.register(UserActions.ADMIN_UPDATE_STATUS, this::updateStatus);
        router.register(UserActions.ADMIN_RESET_PASSWORD, this::resetPassword);
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

    private Response listAccounts(Request request) {
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request, "账号列表加载成功。", administration::listAccounts);
    }

    private Response createAccount(Request request) {
        if (!(request.getData() instanceof CreateUserAccountRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request, "账号创建成功。", () -> administration.createAccount(data));
    }

    private Response updateAccount(Request request) {
        if (!(request.getData() instanceof UpdateUserAccountRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request, "账号信息已更新。", () -> administration.updateAccount(data));
    }

    private Response resetPassword(Request request) {
        if (!(request.getData() instanceof ResetUserPasswordRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request, "密码已重置。", () -> administration.resetPassword(data));
    }

    private Response updateStatus(Request request) {
        if (!(request.getData() instanceof UpdateUserStatusRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        String actorUserId = authentication.findSession(request.getToken())
                .orElseThrow()
                .getUserId();
        return executeAdministration(request, "账号状态已更新。",
                () -> administration.updateStatus(actorUserId, data));
    }

    private Response administrationFailure(Request request) {
        Optional<SessionInfo> session = authentication.findSession(request.getToken());
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!session.get().canManageUsers()) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "只有超级管理员可以管理账号。");
        }
        return null;
    }

    private Response executeAdministration(
            Request request,
            String successMessage,
            Supplier<? extends Serializable> operation) {
        try {
            return Response.success(request, successMessage, operation.get());
        } catch (UserAdministrationException exception) {
            return Response.failure(
                    request.getRequestId(), exception.code(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request);
        }
    }

    private Response invalidRequest(Request request) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "账号管理请求数据无效。");
    }

    private Response authenticationRequired(Request request) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in first.");
    }
}
