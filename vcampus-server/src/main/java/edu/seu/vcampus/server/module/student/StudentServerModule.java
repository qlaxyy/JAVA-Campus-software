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

    public StudentServerModule() {
        this(new StudentService());
    }

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
     * 辅助方法：去除 u- 等统一前缀与连字符，兼容不同格式的学号与账号对比
     */
    private String cleanId(String id) {
        if (id == null) return "";
        String s = id.trim().toLowerCase();
        if (s.startsWith("u-")) {
            s = s.substring(2);
        }
        return s.replace("-", "");
    }

    /**
     * 查询学籍档案
     * 权限规范：
     * 1. 学籍管理员（canAdminister("student")）可通览全校学生档案
     * 2. 学生本人仅可查询自身档案
     * 3. 教师需通过 context.teachers() 验证有效资格
     * 4. 非教务人员（纯医生、商店管理员等）直接拦截
     */
    private Response handleGetProfile(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        if (!(request.getData() instanceof StudentProfileRequest req)) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "请求参数错误");
        }

        String targetId = req.getStudentId();
        if (targetId == null || targetId.isBlank()) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "学号不能为空");
        }

        String currentUserId = cleanId(session.getUserId());
        String currentUsername = cleanId(session.getUsername());
        String cleanTargetId = cleanId(targetId);

        boolean isStudentAdmin = session.canAdminister("student");
        boolean isSelf = currentUserId.equalsIgnoreCase(cleanTargetId)
            || currentUsername.equalsIgnoreCase(cleanTargetId);

        boolean isTeacher = context.teachers()
            .findByUserId(session.getUserId())
            .isPresent();
        boolean isAssignedTeacher = isTeacher
            && context.teacherStudentAccess()
                .canViewStudent(session.getUserId(), targetId);

        if (!isStudentAdmin && !isSelf && !isAssignedTeacher) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：非教务管理或教学人员无权查阅该学生学籍档案");
        }

        StudentProfileResponse profileResponse = studentService.getProfile(req);
        return Response.success(request, "查询档案成功", profileResponse);
    }

    /**
     * 更新学生联络补充档案
     * 权限规范：仅允许学生本人或具备学籍管理权限的管理员修改（教师与外部模块无权修改）
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

        String currentUserId = cleanId(session.getUserId());
        String currentUsername = cleanId(session.getUsername());
        String cleanTargetId = cleanId(req.getStudentId());

        boolean isStudentAdmin = session.canAdminister("student");
        boolean isSelf = currentUserId.equalsIgnoreCase(cleanTargetId)
            || currentUsername.equalsIgnoreCase(cleanTargetId);

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
     * 权限与业务规范：
     * 1. 仅学生本人可发起自身异动
     * 2. 状态机校验（如未休学不可复学、待审核不可并发提交等）由 StudentService 校验并抛出具体异常提示
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

        String currentUserId = cleanId(session.getUserId());
        String currentUsername = cleanId(session.getUsername());
        String cleanTargetId = cleanId(req.getStudentId());

        boolean isStudentAdmin = session.canAdminister("student");
        boolean isSelf = currentUserId.equalsIgnoreCase(cleanTargetId)
            || currentUsername.equalsIgnoreCase(cleanTargetId);

        if (!isStudentAdmin && !isSelf) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：学生仅能提交本人的异动申请");
        }

        try {
            StatusChangeDto dto = studentService.applyStatusChange(req);
            return Response.success(request, "异动申请提交成功", dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 返回具体的状态机拦截提示（如：未休学不能复学、已有待审核申请等）
            return Response.failure(request.getRequestId(), "BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            return Response.failure(request.getRequestId(), "INTERNAL_ERROR", "申请提交异常：" + e.getMessage());
        }
    }

    /**
     * 查询学籍异动申请履历
     * 权限规范：学籍管理员可查询全校；学生仅限查本人；教师及外部管理员无权调阅他人异动
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
            queryStudentId = cleanId(s);
        }

        // 学籍管理员可通览全校
        if (isStudentAdmin) {
            List<StatusChangeDto> list = studentService.listStatusChanges(queryStudentId);
            return Response.success(request, "获取异动列表成功", (Serializable) list);
        }

        // 教师资格查公共教师表，教师与非学籍管理账号禁止查阅他人异动记录
        boolean isTeacher = context.teachers()
            .findByUserId(session.getUserId())
            .isPresent();
        if (isTeacher || !session.canAdminister("student")) {
            if (queryStudentId != null
                    && !queryStudentId.equalsIgnoreCase(cleanId(currentStudentNumber))) {
                return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：无权调阅他人学籍异动");
            }
        }

        // 普通学生强制仅查本人
        List<StatusChangeDto> list = studentService.listStatusChanges(currentStudentNumber);
        return Response.success(request, "获取个人异动成功", (Serializable) list);
    }

    /**
     * 审核学籍异动申请
     * 权限规范：严禁教师及非学籍管理员审核，仅限 canAdminister("student")
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
