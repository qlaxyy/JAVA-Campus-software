package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.*;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.security.UserDirectory;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server module handling student academic profiles and status change workflows.
 */
public final class StudentServerModule implements ServerModule {

    private final StudentService studentService;

    /**
     * Default constructor for ServerModules automatic assembly.
     */
    public StudentServerModule() {
        this(new StudentService());
    }

    /**
     * Constructor with dependency injection.
     */
    public StudentServerModule(StudentService studentService) {
        this.studentService = Objects.requireNonNull(studentService, "studentService must not be null");
    }

    /** Creates a student module backed by the shared server Access database. */
    public static StudentServerModule createAccessBacked(
            Path databasePath, UserDirectory users) {
        return new StudentServerModule(new StudentService(
                new AccessStudentRepository(new AccessDatabase(databasePath), users)));
    }

    @Override
    public String id() {
        return "student";
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(StudentActions.GET_PROFILE, req -> handleGetProfile(req, context));
        router.register(StudentActions.UPDATE_PROFILE, req -> handleUpdateProfile(req, context));
        router.register(StudentActions.APPLY_STATUS_CHANGE, req -> handleApplyStatusChange(req, context));
        router.register(StudentActions.LIST_STATUS_CHANGES, req -> handleListStatusChanges(req, context));
        router.register(StudentActions.AUDIT_STATUS_CHANGE, req -> handleAuditStatusChange(req, context));
    }

    /**
     * 查询学籍档案
     */
    private Response handleGetProfile(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }

        if (!(request.getData() instanceof StudentProfileRequest req)) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "请求参数错误");
        }

        StudentProfileResponse profileResponse = studentService.getProfile(req);
        return Response.success(request, "查询档案成功", profileResponse);
    }

    /**
     * 更新学生联络补充档案
     * 权限规范：仅允许学生本人或具备学籍管理权限的管理员修改（防止商店管理员越权）
     */
    private Response handleUpdateProfile(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        if (!(request.getData() instanceof StudentUpdateProfileRequest req)) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "请求参数错误");
        }

        boolean isStudentAdmin = session.canAdminister("student");
        boolean isSelf = session.getUsername().equalsIgnoreCase(req.getStudentId());

        if (!isStudentAdmin && !isSelf) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：当前账号无权修改该学生档案");
        }

        boolean updated = studentService.updateProfile(req);
        if (updated) {
            return Response.success(request, "学生档案联络信息已更新", null);
        } else {
            return Response.failure(request.getRequestId(), "NOT_FOUND", "更新失败，未找到对应学号的档案记录");
        }
    }

    /**
     * 发起学籍异动申请
     * 权限规范：仅学生本人可发起自身异动
     */
    private Response handleApplyStatusChange(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        if (!(request.getData() instanceof ApplyStatusChangeRequest req)) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "请求参数错误");
        }

        boolean isStudentAdmin = session.canAdminister("student");
        boolean isSelf = session.getUsername().equalsIgnoreCase(req.getStudentId());

        if (!isStudentAdmin && !isSelf) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：学生仅能提交本人的异动申请");
        }

        StatusChangeDto dto = studentService.applyStatusChange(req);
        if (dto != null) {
            return Response.success(request, "异动申请提交成功", dto);
        } else {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "申请提交失败，请检查学生学号");
        }
    }

    /**
     * 查询学籍异动申请履历
     * 权限规范：学籍管理员可通览或查他人；学生仅限查看本人；禁止教师及非学籍管理员调阅
     */
    private Response handleListStatusChanges(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        boolean isStudentAdmin = session.canAdminister("student");
        String currentStudentNumber = session.getUsername();

        String queryStudentId = null;
        if (request.getData() instanceof String s && !s.isBlank()) {
            queryStudentId = s.trim();
        }

        if (isStudentAdmin) {
            List<StatusChangeDto> list = studentService.listStatusChanges(queryStudentId);
            return Response.success(request, "获取异动列表成功", (Serializable) list);
        }

        // 非学籍管理员且非学生本人（例如教师或商店管理员）直接拦截。
        // 教师资格必须查公共教师表，不能根据 userId 文本猜测。
        boolean isTeacher = context.teachers()
                .findByUserId(session.getUserId())
                .isPresent();
        if (isTeacher || !session.canAdminister("student")) {
            if (queryStudentId != null && !queryStudentId.equalsIgnoreCase(currentStudentNumber)) {
                return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：无权调阅他人学籍异动");
            }
        }

        // 普通学生强制仅查本人
        List<StatusChangeDto> list = studentService.listStatusChanges(currentStudentNumber);
        return Response.success(request, "获取个人异动成功", (Serializable) list);
    }

    /**
     * 审核学籍异动申请
     * 权限规范：必须具备学籍管理权限 (SUPER_ADMIN 或具有 student 作用域的管理员)
     */
    private Response handleAuditStatusChange(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        if (!(request.getData() instanceof AuditStatusChangeRequest req)) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "请求参数错误");
        }

        // 强权限拦截：非学籍管理员直接拒绝
        if (!session.canAdminister("student")) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：当前账号不具备学籍管理审核权限");
        }

        String operator = session.getDisplayName() != null ? session.getDisplayName() : session.getUsername();
        boolean audited = studentService.auditStatusChange(req.getChangeId(), req.isApproved(), operator);

        if (audited) {
            return Response.success(request, "异动审核已完成", null);
        } else {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "审核处理失败，记录不存在或已处理");
        }
    }
}
