package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.LoginRequest;
import edu.seu.vcampus.common.user.BatchCreateUserAccountsRequest;
import edu.seu.vcampus.common.user.ChangePasswordRequest;
import edu.seu.vcampus.common.user.CreateUserAccountRequest;
import edu.seu.vcampus.common.user.CreateGeneratedUserAccountRequest;
import edu.seu.vcampus.common.user.ResetUserPasswordRequest;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.common.user.UpdateUserAccountRequest;
import edu.seu.vcampus.common.user.UpdateUserStatusRequest;
import edu.seu.vcampus.common.user.UserActions;
import edu.seu.vcampus.common.user.UserAccountView;
import edu.seu.vcampus.common.user.UserAuditLogEntry;
import edu.seu.vcampus.common.user.UserAuditLogResponse;
import edu.seu.vcampus.common.user.SaveTeacherProfileRequest;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/** Server entry point owned by the user-management module. */
public final class UserServerModule implements ServerModule {

    private final InMemoryAuthenticationService authentication;
    private final UserAdministrationService administration;
    private final UserAuditRepository auditLogs;

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
        this.auditLogs = authentication.auditLogs();
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
        router.register(UserActions.CHANGE_PASSWORD, this::changePassword);
        router.register(UserActions.ADMIN_LIST_ACCOUNTS, this::listAccounts);
        router.register(UserActions.ADMIN_CREATE_ACCOUNT, this::createAccount);
        router.register(UserActions.ADMIN_PREVIEW_NEXT_ACCOUNT, this::previewNextAccount);
        router.register(UserActions.ADMIN_CREATE_GENERATED_ACCOUNT,
                this::createGeneratedAccount);
        router.register(UserActions.ADMIN_BATCH_CREATE_ACCOUNTS, this::createAccounts);
        router.register(UserActions.ADMIN_UPDATE_ACCOUNT, this::updateAccount);
        router.register(UserActions.ADMIN_UPDATE_STATUS, this::updateStatus);
        router.register(UserActions.ADMIN_RESET_PASSWORD, this::resetPassword);
        router.register(UserActions.ADMIN_LIST_AUDIT_LOGS, this::listAuditLogs);
        router.register(UserActions.CURRENT_TEACHER_PROFILE, this::currentTeacherProfile);
        router.register(UserActions.ADMIN_LIST_TEACHERS, this::listTeachers);
        router.register(UserActions.ADMIN_SAVE_TEACHER_PROFILE, this::saveTeacherProfile);
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

    private Response changePassword(Request request) {
        if (!(request.getData() instanceof ChangePasswordRequest data)) {
            return invalidPasswordRequest(request);
        }
        Optional<SessionInfo> session = authentication.findSession(request.getToken());
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!authentication.changePassword(
                session.get().getUserId(),
                data.getCurrentPasswordProof(),
                data.getNewPasswordProof())) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_INVALID_CREDENTIALS,
                    "当前密码不正确。");
        }
        return Response.success(request, "密码修改成功，请重新登录。", null);
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
        return executeAuditedAdministration(
                request, "账号创建成功。", UserActions.ADMIN_CREATE_ACCOUNT,
                data.getUsername(), () -> administration.createAccount(data));
    }

    private Response previewNextAccount(Request request) {
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request, "下一张一卡通号已生成。",
                administration::previewNextAccountNumber);
    }

    private Response createGeneratedAccount(Request request) {
        if (!(request.getData() instanceof CreateGeneratedUserAccountRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAuditedAdministration(
                request, "账号创建成功。", UserActions.ADMIN_CREATE_GENERATED_ACCOUNT,
                data.getDisplayName(), () -> administration.createGeneratedAccount(data));
    }

    private Response createAccounts(Request request) {
        if (!(request.getData() instanceof BatchCreateUserAccountsRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAuditedAdministration(
                request,
                "成功导入 " + data.getAccounts().size() + " 个账号。",
                UserActions.ADMIN_BATCH_CREATE_ACCOUNTS,
                "批量账号：" + data.getAccounts().size() + " 个",
                () -> administration.createAccounts(data));
    }

    private Response updateAccount(Request request) {
        if (!(request.getData() instanceof UpdateUserAccountRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAuditedAdministration(
                request, "账号信息已更新。", UserActions.ADMIN_UPDATE_ACCOUNT,
                accountTarget(data.getUserId()), () -> administration.updateAccount(data));
    }

    private Response resetPassword(Request request) {
        if (!(request.getData() instanceof ResetUserPasswordRequest data)) {
            return invalidRequest(request);
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAuditedAdministration(
                request, "密码已重置。", UserActions.ADMIN_RESET_PASSWORD,
                accountTarget(data.getUserId()), () -> administration.resetPassword(data));
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
        return executeAuditedAdministration(
                request, "账号状态已更新。", UserActions.ADMIN_UPDATE_STATUS,
                accountTarget(data.getUserId()),
                () -> administration.updateStatus(actorUserId, data));
    }

    private Response listAuditLogs(Request request) {
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request,
                "操作记录加载成功。",
                () -> new UserAuditLogResponse(auditLogs.findAll()));
    }

    private Response currentTeacherProfile(Request request) {
        Optional<SessionInfo> session = authentication.findSession(request.getToken());
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        return authentication.teachers().profileView(session.get().getUserId())
                .filter(edu.seu.vcampus.common.user.TeacherProfileView::isActive)
                .map(profile -> Response.success(
                        request, "教师信息加载成功。", profile))
                .orElseGet(() -> Response.failure(
                        request.getRequestId(),
                        ErrorCodes.AUTH_FORBIDDEN,
                        "当前账号没有教师资格。"));
    }

    private Response listTeachers(Request request) {
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        return executeAdministration(
                request, "教师名单加载成功。", authentication.teachers()::listProfiles);
    }

    private Response saveTeacherProfile(Request request) {
        if (!(request.getData() instanceof SaveTeacherProfileRequest data)) {
            return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.COMMON_INVALID_REQUEST,
                    "教师档案请求数据无效。");
        }
        Response denied = administrationFailure(request);
        if (denied != null) {
            return denied;
        }
        SessionInfo actor = authentication.findSession(request.getToken()).orElseThrow();
        return executeAuditedAdministration(
                request,
                data.isActive() ? "教师档案已保存。" : "教师资格已停用。",
                UserActions.ADMIN_SAVE_TEACHER_PROFILE,
                accountTarget(data.getUserId()),
                () -> authentication.teachers().saveProfile(data, actor.getUserId()));
    }

    private Response executeAuditedAdministration(
            Request request,
            String successMessage,
            String actionCode,
            String requestedTarget,
            Supplier<? extends Serializable> operation) {
        SessionInfo actor = authentication.findSession(request.getToken()).orElseThrow();
        Response response = executeAdministration(request, successMessage, operation);
        String target = response.getData() instanceof UserAccountView account
                ? account.getUsername()
                : requestedTarget;
        appendAudit(actor, actionCode, target, response);
        return response;
    }

    private void appendAudit(
            SessionInfo actor,
            String actionCode,
            String target,
            Response response) {
        try {
            String detail = response.getCode() + "：" + response.getMessage();
            auditLogs.append(new UserAuditLogEntry(
                    UUID.randomUUID().toString(),
                    Instant.now().toEpochMilli(),
                    actor.getUserId(),
                    actor.getUsername(),
                    actor.getDisplayName(),
                    actionCode,
                    limit(target, 120),
                    response.isSuccess(),
                    limit(detail, 255)));
        } catch (RuntimeException exception) {
            System.err.println("Failed to save user audit record: " + exception.getMessage());
        }
    }

    private static String limit(String value, int maximumLength) {
        String text = value == null || value.isBlank() ? "未知目标" : value.trim();
        return text.length() <= maximumLength ? text : text.substring(0, maximumLength);
    }

    private String accountTarget(String userId) {
        return authentication.users().findById(userId)
                .map(UserAccount::username)
                .orElse(userId);
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

    private Response invalidPasswordRequest(Request request) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "修改密码请求数据无效。");
    }

    private Response authenticationRequired(Request request) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in first.");
    }
}
