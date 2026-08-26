package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.student.StudentActions;
import edu.seu.vcampus.common.student.StudentProfileRequest;
import edu.seu.vcampus.common.student.StudentProfileResponse;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

/**
 * Server entry point owned by the student-record module.
 */
public final class StudentServerModule implements ServerModule {

    private final StudentService service = new StudentService();

    @Override
    public String id() {
        return ModuleNames.STUDENT;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(StudentActions.GET_PROFILE, request -> {
            // 1. 利用公共 Session 校验用户登录状态
            if (context.sessions().findSession(request.getToken()).isEmpty()) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "请先在用户管理模块登录后再查询学籍！"
                );
            }

            // 2. 校验请求参数
            if (!(request.getData() instanceof StudentProfileRequest req)) {
                return Response.failure(
                    request.getRequestId(),
                    ErrorCodes.AUTH_REQUIRED,
                    "无效的学籍查询请求参数"
                );
            }

            // 3. 执行业务查询
            StudentProfileResponse resp = service.getProfile(req);

            // 4. 返回成功响应
            return Response.success(request, "查询成功", resp);
        });
    }
}
