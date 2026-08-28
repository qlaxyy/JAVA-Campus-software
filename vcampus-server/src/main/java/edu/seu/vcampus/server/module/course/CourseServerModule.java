package edu.seu.vcampus.server.module.course;

import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.DropCourseRequest;
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

    private final CourseBatchService
        batchService;

    private final CoursePlanService
        planService;

    private final CourseSelectionService
        selectionService;

    private final CourseEnrollmentService
        enrollmentService;

    /**
     * 默认构造器。
     */
    public CourseServerModule() {

        Clock clock =
            Clock.systemDefaultZone();

        CourseBatchService batchService =
            new CourseBatchService(
                new InMemoryCourseBatchRepository(
                    clock),
                clock);

        CoursePlanRepository planRepository =
            new InMemoryCoursePlanRepository();

        /*
         * 所有涉及选课状态的 Service
         * 必须共用同一个 enrollmentRepository。
         */
        CourseEnrollmentRepository enrollmentRepository =
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

        this.enrollmentService =
            new CourseEnrollmentService(
                batchService,
                planRepository,
                enrollmentRepository);
    }

    /**
     * 测试或依赖注入使用。
     */
    CourseServerModule(
        CourseBatchService batchService,
        CoursePlanService planService,
        CourseSelectionService selectionService,
        CourseEnrollmentService enrollmentService) {

        this.batchService =
            Objects.requireNonNull(
                batchService);

        this.planService =
            Objects.requireNonNull(
                planService);

        this.selectionService =
            Objects.requireNonNull(
                selectionService);

        this.enrollmentService =
            Objects.requireNonNull(
                enrollmentService);
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
            request ->
                listBatches(
                    request,
                    context));

        router.register(
            CourseActions.LIST_PLAN_COURSES,
            request ->
                listPlanCourses(
                    request,
                    context));

        router.register(
            CourseActions.SELECT_COURSE,
            request ->
                selectCourse(
                    request,
                    context));

        /*
         * 已选课程。
         */
        router.register(
            CourseActions.LIST_ENROLLMENTS,
            request ->
                listEnrollments(
                    request,
                    context));

        /*
         * 退课。
         */
        router.register(
            CourseActions.DROP_COURSE,
            request ->
                dropCourse(
                    request,
                    context));
    }

    /**
     * 查询选课批次。
     */
    private Response listBatches(
        Request request,
        ServerContext context) {

        if (context.sessions()
            .findSession(
                request.getToken())
            .isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before using the course module.");
        }

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
     * 选择课程。
     */
    private Response selectCourse(
        Request request,
        ServerContext context) {

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

        if (!(request.getData()
            instanceof SelectCourseRequest
            selectRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Course-selection request is invalid.");
        }

        CourseSelectionResult result =
            selectionService.selectCourse(
                session.getUserId(),
                selectRequest.getBatchId(),
                selectRequest.getOfferingId());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        return Response.success(
            request,
            result.message(),
            null);
    }

    /**
     * 查询当前学生已选课程。
     */
    private Response listEnrollments(
        Request request,
        ServerContext context) {

        SessionInfo session =
            context.sessions()
                .findSession(
                    request.getToken())
                .orElse(null);

        if (session == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before viewing enrollments.");
        }

        if (!(request.getData()
            instanceof BatchRequest batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Enrollment request must contain a BatchRequest.");
        }

        return Response.success(
            request,
            "Enrollments loaded.",
            new ArrayList<>(
                enrollmentService.listEnrollments(
                    session.getUserId(),
                    batchRequest.getBatchId())));
    }

    /**
     * 退课。
     */
    private Response dropCourse(
        Request request,
        ServerContext context) {

        SessionInfo session =
            context.sessions()
                .findSession(
                    request.getToken())
                .orElse(null);

        if (session == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before dropping courses.");
        }

        if (!(request.getData()
            instanceof DropCourseRequest
            dropRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Drop-course request is invalid.");
        }

        CourseDropResult result =
            enrollmentService.dropCourse(
                session.getUserId(),
                dropRequest.getBatchId(),
                dropRequest.getEnrollmentId());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        return Response.success(
            request,
            result.message(),
            null);
    }
}
