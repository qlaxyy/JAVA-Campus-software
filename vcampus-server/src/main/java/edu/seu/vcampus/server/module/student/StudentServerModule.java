package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.StudentActions;
import edu.seu.vcampus.common.student.StudentProfileRequest;
import edu.seu.vcampus.common.student.StudentProfileResponse;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

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

            // 基于规范化后的 ID 判断角色
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
                // 若未填学号，默认查询当前登录本人
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

            // 非管理员（学生）只能修改本人的信息
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
    }
}
