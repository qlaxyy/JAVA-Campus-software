package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.SelectCourseRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 选课模块服务器入口。
 */
public final class CourseServerModule
    implements ServerModule {

    private final CourseBatchService batchService;
    private final CoursePlanService planService;
    private final CourseSelectionService selectionService;

    /**
     * 默认构造器。
     *
     * 当前开发阶段使用内存 Repository。
     */
    public CourseServerModule() {

        Clock clock =
            Clock.systemDefaultZone();

        /*
         * 批次数据。
         */
        CourseBatchService batchService =
            new CourseBatchService(
                new InMemoryCourseBatchRepository(
                    clock),
                clock);

        /*
         * 方案内课程原始数据。
         */
        CoursePlanRepository planRepository =
            new InMemoryCoursePlanRepository();

        /*
         * 选课记录。
         *
         * CoursePlanService 和 CourseSelectionService
         * 必须共用同一个 Repository，
         * 否则选课之后查询课程时看不到最新状态。
         */
        CourseEnrollmentRepository
            enrollmentRepository =
            new InMemoryCourseEnrollmentRepository();

        this.batchService =
            batchService;

        this.planService =
            new CoursePlanService(
                planRepository,
                enrollmentRepository);

        this.selectionService =
            new CourseSelectionService(
                batchService,
                planRepository,
                enrollmentRepository);
    }

    /**
     * 用于测试或后续依赖注入的构造器。
     */
    CourseServerModule(
        CourseBatchService batchService,
        CoursePlanService planService,
        CourseSelectionService selectionService) {

        this.batchService =
            Objects.requireNonNull(
                batchService,
                "batchService must not be null");

        this.planService =
            Objects.requireNonNull(
                planService,
                "planService must not be null");

        this.selectionService =
            Objects.requireNonNull(
                selectionService,
                "selectionService must not be null");
    }

    @Override
    public String id() {

        return ModuleNames.COURSE;
    }

    /**
     * 注册选课模块所有服务器 Action。
     */
    @Override
    public void registerHandlers(
        ActionRouter router,
        ServerContext context) {

        /*
         * 查询选课批次。
         */
        router.register(
            CourseActions.LIST_BATCHES,
            request ->
                listBatches(
                    request,
                    context));

        /*
         * 查询方案内课程。
         */
        router.register(
            CourseActions.LIST_PLAN_COURSES,
            request ->
                listPlanCourses(
                    request,
                    context));

        /*
         * 选择教学班。
         */
        router.register(
            CourseActions.SELECT_COURSE,
            request ->
                selectCourse(
                    request,
                    context));
    }

    /**
     * 查询当前学期选课批次。
     */
    private Response listBatches(
        Request request,
        ServerContext context) {

        /*
         * 必须登录。
         */
        if (context.sessions()
            .findSession(
                request.getToken())
            .isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before using the course module.");
        }

        /*
         * LIST_BATCHES 不需要请求数据。
         */
        if (request.getData() != null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Batch-list data must be empty.");
        }

        return Response.success(
            request,
            "Course selection batches loaded.",
            new ArrayList<>(
                batchService.listBatches()));
    }

    /**
     * 查询方案内课程。
     */
    private Response listPlanCourses(
        Request request,
        ServerContext context) {

        /*
         * 先根据 token 获取登录用户。
         */
        SessionInfo session =
            context.sessions()
                .findSession(
                    request.getToken())
                .orElse(null);

        if (session == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before using the course module.");
        }

        /*
         * LIST_PLAN_COURSES 必须传 BatchRequest。
         */
        if (!(request.getData()
            instanceof BatchRequest batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Plan-course request must contain a BatchRequest.");
        }

        return Response.success(
            request,
            "Plan courses loaded.",
            new ArrayList<>(
                planService.listPlanCourses(
                    batchRequest.getBatchId(),
                    session.getUserId())));
    }

    /**
     * 学生选择教学班。
     */
    private Response selectCourse(
        Request request,
        ServerContext context) {

        /*
         * 获取当前登录用户。
         */
        SessionInfo session =
            context.sessions()
                .findSession(
                    request.getToken())
                .orElse(null);

        if (session == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before selecting courses.");
        }

        /*
         * 请求数据必须是 SelectCourseRequest。
         */
        if (!(request.getData()
            instanceof SelectCourseRequest
            selectRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Course-selection request is invalid.");
        }

        /*
         * 真正的业务判断交给 Service。
         */
        CourseSelectionResult result =
            selectionService.selectCourse(
                session.getUserId(),
                selectRequest.getBatchId(),
                selectRequest.getOfferingId());

        /*
         * 业务失败。
         */
        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        /*
         * 选课成功。
         */
        return Response.success(
            request,
            result.message(),
            null);
    }
}
