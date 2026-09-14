package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.protocol.ModuleNames;
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
        router.register("student:add", req -> handleAddStudent(req, context));
        router.register("student:delete", req -> handleDeleteStudent(req, context));
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

        boolean isStudentAdmin = session.canAdminister(ModuleNames.STUDENT);
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

        boolean isStudentAdmin = session.canAdminister(ModuleNames.STUDENT);
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
     * 新增学生学籍档案（管理员专属）
     */
    private Response handleAddStudent(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        if (!session.canAdminister(ModuleNames.STUDENT)) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：当前账号不具备录入学生学籍的管理员权限");
        }

        if (!(request.getData() instanceof StudentProfileDto profileDto)) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "请求参数错误");
        }

        try {
            boolean added = studentService.addStudent(profileDto);
            if (added) {
                return Response.success(request, "新生学籍档案录入成功", null);
            } else {
                return Response.failure(request.getRequestId(), "BAD_REQUEST", "录入失败：该学号可能已存在");
            }
        } catch (Exception e) {
            return Response.failure(request.getRequestId(), "INTERNAL_ERROR", "录入异常：" + e.getMessage());
        }
    }

    /**
     * 删除学生学籍档案（管理员专属）
     */
    private Response handleDeleteStudent(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        if (!session.canAdminister(ModuleNames.STUDENT)) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：当前账号不具备删除学生学籍的管理员权限");
        }

        String studentId = null;
        if (request.getData() instanceof String s) {
            studentId = s;
        } else if (request.getData() instanceof StudentProfileRequest req) {
            studentId = req.getStudentId();
        }

        if (studentId == null || studentId.isBlank()) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", "学号不能为空");
        }

        boolean deleted = studentService.deleteStudent(studentId);
        if (deleted) {
            return Response.success(request, "学生学籍档案已成功删除", null);
        } else {
            return Response.failure(request.getRequestId(), "NOT_FOUND", "删除失败：未找到该学号的档案记录");
        }
    }

    /**
     * 发起学籍异动申请
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

        boolean isStudentAdmin = session.canAdminister(ModuleNames.STUDENT);
        boolean isSelf = currentUserId.equalsIgnoreCase(cleanTargetId)
            || currentUsername.equalsIgnoreCase(cleanTargetId);

        if (!isStudentAdmin && !isSelf) {
            return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：学生仅能提交本人的异动申请");
        }

        try {
            StatusChangeDto dto = studentService.applyStatusChange(req);
            return Response.success(request, "异动申请提交成功", dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Response.failure(request.getRequestId(), "BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            return Response.failure(request.getRequestId(), "INTERNAL_ERROR", "申请提交异常：" + e.getMessage());
        }
    }

    /**
     * 查询学籍异动申请履历
     */
    private Response handleListStatusChanges(Request request, ServerContext context) {
        Optional<SessionInfo> sessionOpt = context.sessions().findSession(request.getToken());
        if (sessionOpt.isEmpty()) {
            return Response.failure(request.getRequestId(), "UNAUTHORIZED", "未登录或会话已失效");
        }
        SessionInfo session = sessionOpt.get();

        boolean isStudentAdmin = session.canAdminister(ModuleNames.STUDENT);
        String currentStudentNumber = session.getUsername();

        String queryStudentId = null;
        if (request.getData() instanceof String s && !s.isBlank()) {
            queryStudentId = cleanId(s);
        }

        if (isStudentAdmin) {
            List<StatusChangeDto> list = studentService.listStatusChanges(queryStudentId);
            return Response.success(request, "获取异动列表成功", (Serializable) list);
        }

        boolean isTeacher = context.teachers()
            .findByUserId(session.getUserId())
            .isPresent();
        if (isTeacher || !session.canAdminister(ModuleNames.STUDENT)) {
            if (queryStudentId != null
                && !queryStudentId.equalsIgnoreCase(cleanId(currentStudentNumber))) {
                return Response.failure(request.getRequestId(), "FORBIDDEN", "权限不足：无权调阅他人学籍异动");
            }
        }

        List<StatusChangeDto> list = studentService.listStatusChanges(currentStudentNumber);
        return Response.success(request, "获取个人异动成功", (Serializable) list);
    }

    /**
     * 审核学籍异动申请
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

        if (!session.canAdminister(ModuleNames.STUDENT)) {
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
