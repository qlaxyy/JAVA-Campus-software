package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;
import java.util.ArrayList;
import java.time.Clock;
import java.util.Objects;

/**
 * Server entry point owned by the course-selection module.
 */
public final class CourseServerModule implements ServerModule {

    private final CourseBatchService batchService;

    /**
     * 默认构造器。
     */
    public CourseServerModule() {
        this(createDefaultService());
    }

    CourseServerModule(CourseBatchService batchService) {
        this.batchService = Objects.requireNonNull(
            batchService,
            "batchService must not be null");
    }

    @Override
    public String id() {
        return ModuleNames.COURSE;
    }

    @Override
    public void registerHandlers(
        ActionRouter router,
        ServerContext context) {

        router.register(
            CourseActions.LIST_BATCHES,
            request -> listBatches(request, context)
        );
    }

    /**
     * 查询当前学期选课批次。
     */
    private Response listBatches(
        Request request,
        ServerContext context) {

        // 必须先登录。
        if (context.sessions()
            .findSession(request.getToken())
            .isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before using the course module."
            );
        }

        // LIST_BATCHES 不需要请求参数。
        if (request.getData() != null) {
            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Batch-list data must be empty."
            );
        }

        return Response.success(
            request,
            "Course selection batches loaded.",
            new ArrayList<>(batchService.listBatches())
        );
    }

    /**
     * 创建当前开发阶段默认服务。
     */
    private static CourseBatchService createDefaultService() {
        Clock clock = Clock.systemDefaultZone();

        return new CourseBatchService(
            new InMemoryCourseBatchRepository(clock),
            clock
        );
    }
}
