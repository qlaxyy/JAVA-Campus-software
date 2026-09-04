package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.*;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class StudentServerModule implements ServerModule {

    private final StudentService service = new StudentService();

    @Override
    public String id() {
        return ModuleNames.STUDENT;
    }

    /**
     * 规范化账号字符串：转小写，去除前缀 "u-" 以及连字符 "-"
     * 例如 "U-STUDENT-001" -> "student001"
     */
    private String normalizeId(String id) {
        if (id == null) return "";
        String clean = id.trim().toLowerCase();
        if (clean.startsWith("u-")) {
            clean = clean.substring(2);
        }
        return clean.replace("-", "");
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        // 1. 获取学籍信息
        router.register(StudentActions.GET_PROFILE, request -> {
            var sessionOpt = context.sessions().findSession(request.getToken());
            if (sessionOpt.isEmpty()) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先在用户管理模块登录后再查询学籍！"
                );
            }

            if (!(request.getData() instanceof StudentProfileRequest req)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "无效的学籍查询请求参数"
                );
            }

            var session = sessionOpt.get();
            String rawUserId = session.getUserId() != null ? session.getUserId().trim() : "";
            String cleanUserId = normalizeId(rawUserId);
            String targetStudentId = req.getStudentId() != null ? req.getStudentId().trim() : "";
            String cleanTargetId = normalizeId(targetStudentId);

            System.out.println("[学籍服务] 收到查询请求 -> 登录用户: [" + rawUserId + " -> " + cleanUserId + "], 目标学号: [" + targetStudentId + " -> " + cleanTargetId + "]");

            boolean isAdmin = cleanUserId.contains("admin");
            boolean isTeacher = cleanUserId.contains("teacher");
            boolean isStudent = !isAdmin && !isTeacher;

            // 权限控制：
            // - 管理员：全放行
            // - 教师：全放行（支持查阅）
            // - 学生：只能查本人
            if (isStudent) {
                if (!cleanTargetId.isEmpty() && !cleanUserId.equals(cleanTargetId)) {
                    System.out.println("[学籍服务] 越权拦截：学生 [" + rawUserId + "] 试图查询他人 [" + targetStudentId + "]");
                    return Response.failure(
                        request.getRequestId(),
                        ErrorCodes.AUTH_FORBIDDEN,
                        "权限不足：普通学生仅允许查阅本人的学籍档案！"
                    );
                }
                if (targetStudentId.isEmpty()) {
                    req.setStudentId(cleanUserId);
                }
            }

            StudentProfileResponse resp = service.getProfile(req);
            return Response.success(request, "查询成功", resp);
        });

        // 2. 更新学籍补充信息
        router.register(StudentActions.UPDATE_PROFILE, request -> {
            var sessionOpt = context.sessions().findSession(request.getToken());
            if (sessionOpt.isEmpty()) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "未登录或登录已失效，请重新登录！"
                );
            }

            if (!(request.getData() instanceof StudentUpdateProfileRequest req)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "无效的学籍修改请求参数"
                );
            }

            var session = sessionOpt.get();
            String rawUserId = session.getUserId() != null ? session.getUserId().trim() : "";
            String cleanUserId = normalizeId(rawUserId);
            String targetStudentId = req.getStudentId() != null ? req.getStudentId().trim() : "";
            String cleanTargetId = normalizeId(targetStudentId);

            System.out.println("[学籍服务] 收到更新请求 -> 登录用户: [" + rawUserId + " -> " + cleanUserId + "], 目标学号: [" + targetStudentId + " -> " + cleanTargetId + "]");

            boolean isAdmin = cleanUserId.contains("admin");
            boolean isTeacher = cleanUserId.contains("teacher");

            // 教师账号只读
            if (isTeacher) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "权限不足：教师账号仅具备学籍查阅权限，无权修改档案！"
                );
            }

            if (!isAdmin && !cleanUserId.equals(cleanTargetId)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "权限不足：您只能修改本人的学籍补充信息！"
                );
            }

            boolean updated = service.updateProfile(req);
            if (updated) {
                return Response.success(request, "个人信息修改成功！", true);
            } else {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "修改失败：未找到对应的学籍档案"
                );
            }
        });

        // 3. 发起学籍异动申请
        router.register(StudentActions.APPLY_STATUS_CHANGE, request -> {
            var sessionOpt = context.sessions().findSession(request.getToken());
            if (sessionOpt.isEmpty()) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录后再申请异动！"
                );
            }

            if (!(request.getData() instanceof ApplyStatusChangeRequest req)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "无效的异动申请参数"
                );
            }

            var session = sessionOpt.get();
            String cleanUserId = normalizeId(session.getUserId());
            String cleanTargetId = normalizeId(req.getStudentId());

            boolean isAdmin = cleanUserId.contains("admin");
            if (!isAdmin && !cleanUserId.equals(cleanTargetId)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "权限不足：不能替其他学生申请异动！"
                );
            }

            StatusChangeDto change = service.applyStatusChange(req);
            if (change != null) {
                return Response.success(request, "学籍异动申请已提交", change);
            } else {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "提交失败：找不到对应的学生档案"
                );
            }
        });

        // 4. 获取学籍异动履历列表（仅管理员允许全局通览）
        router.register(StudentActions.LIST_STATUS_CHANGES, request -> {
            var sessionOpt = context.sessions().findSession(request.getToken());
            if (sessionOpt.isEmpty()) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录！"
                );
            }

            var session = sessionOpt.get();
            String cleanUserId = normalizeId(session.getUserId());
            boolean isAdmin = cleanUserId.contains("admin");
            boolean isTeacher = cleanUserId.contains("teacher");

            String studentId = request.getData() instanceof String s ? s : null;

            // 规则控制：
            // 1. 如果请求查看全局（studentId 为空），且当前不是管理员，直接拒绝拦截！教师也无权直接全局通览
            if ((studentId == null || studentId.trim().isEmpty()) && !isAdmin) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "权限不足：仅系统学籍管理员允许直接查看全校异动申请！"
                );
            }

            // 2. 普通学生被严格限制只能查看本人申请
            if (!isAdmin && !isTeacher) {
                studentId = cleanUserId;
            }

            List<StatusChangeDto> list = service.listStatusChanges(studentId);
            return Response.success(request, "获取成功", (Serializable) new ArrayList<>(list));
        });

        // 5. 审核学籍异动申请（仅管理员具备审批权限）
        router.register(StudentActions.AUDIT_STATUS_CHANGE, request -> {
            var sessionOpt = context.sessions().findSession(request.getToken());
            if (sessionOpt.isEmpty()) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先登录！"
                );
            }

            if (!(request.getData() instanceof AuditStatusChangeRequest req)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "无效的审核请求参数"
                );
            }

            var session = sessionOpt.get();
            String cleanUserId = normalizeId(session.getUserId());
            boolean isAdmin = cleanUserId.contains("admin");

            // 关键限制：唯有管理员有审核权，普通老师严禁越权审批
            if (!isAdmin) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_FORBIDDEN,
                    "权限不足：仅限系统学籍管理员执行异动审批操作！"
                );
            }

            boolean ok = service.auditStatusChange(req.getChangeId(), req.isApproved(), cleanUserId);
            if (ok) {
                return Response.success(request, "异动审核成功！", true);
            } else {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "审核失败：记录不存在或已被处理"
                );
            }
        });
    }
}
