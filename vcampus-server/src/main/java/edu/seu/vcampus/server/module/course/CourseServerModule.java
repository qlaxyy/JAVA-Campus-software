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

    /**
     * 选课批次业务。
     */
    private final CourseBatchService
        batchService;

    /**
     * 方案内课程业务。
     */
    private final CoursePlanService
        planService;

    /**
     * 选课业务。
     */
    private final CourseSelectionService
        selectionService;

    /**
     * 已选课程 / 退课业务。
     */
    private final CourseEnrollmentService
        enrollmentService;

    /**
     * 方案外课程业务。
     */
    private final CourseSubstitutionService
        substitutionService;

    /**
     * 默认构造器。
     *
     * 当前开发阶段所有数据均使用
     * InMemory Repository。
     */
    public CourseServerModule() {

        /*
         * =========================
         * 1. 时钟
         * =========================
         */
        Clock clock =
            Clock.systemDefaultZone();

        /*
         * =========================
         * 2. 选课批次
         * =========================
         */
        CourseBatchService batchService =
            new CourseBatchService(
                new InMemoryCourseBatchRepository(
                    clock),
                clock);

        /*
         * =========================
         * 3. 方案内课程数据
         * =========================
         */
        CoursePlanRepository planRepository =
            new InMemoryCoursePlanRepository();

        /*
         * =========================
         * 4. 方案外课程及替代关系
         * =========================
         */
        CourseSubstitutionRepository substitutionRepository =
            new InMemoryCourseSubstitutionRepository();

        /*
         * =========================
         * 5. 学生当前选课记录
         * =========================
         *
         * 所有涉及当前选课状态的 Service
         * 必须共享同一个 Repository。
         *
         * 否则：
         * 选课 Service 选了课程，
         * 其他 Service 会看不到。
         */
        CourseEnrollmentRepository enrollmentRepository =
            new InMemoryCourseEnrollmentRepository();

        /*
         * =========================
         * 6. 历史修读数据
         * =========================
         *
         * 用于判断：
         *
         * - 是否允许重修
         * - 是否已经历史修读过课程
         */
        CourseHistoryRepository historyRepository =
            new InMemoryCourseHistoryRepository();

        /*
         * =========================
         * 7. 保存批次 Service
         * =========================
         */
        this.batchService =
            batchService;

        /*
         * =========================
         * 8. 方案内课程 Service
         * =========================
         */
        this.planService =
            new CoursePlanService(
                batchService,
                planRepository,
                enrollmentRepository,
                historyRepository);

        /*
         * =========================
         * 9. 选课 Service
         * =========================
         */
        this.selectionService =
            new CourseSelectionService(
                batchService,
                planRepository,
                enrollmentRepository,
                historyRepository);

        /*
         * =========================
         * 10. 已选课程 / 退课 Service
         * =========================
         */
        this.enrollmentService =
            new CourseEnrollmentService(
                batchService,
                planRepository,
                enrollmentRepository);

        /*
         * =========================
         * 11. 方案外课程 Service
         * =========================
         */
        this.substitutionService =
            new CourseSubstitutionService(
                substitutionRepository,
                planRepository,
                enrollmentRepository);
    }

    /**
     * 测试或依赖注入使用的构造器。
     */
    CourseServerModule(
        CourseBatchService batchService,
        CoursePlanService planService,
        CourseSelectionService selectionService,
        CourseEnrollmentService enrollmentService,
        CourseSubstitutionService substitutionService) {

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

        this.enrollmentService =
            Objects.requireNonNull(
                enrollmentService,
                "enrollmentService must not be null");

        this.substitutionService =
            Objects.requireNonNull(
                substitutionService,
                "substitutionService must not be null");
    }

    /**
     * 模块名称。
     */
    @Override
    public String id() {

        return ModuleNames.COURSE;
    }

    /**
     * 注册选课模块服务器 Action。
     */
    @Override
    public void registerHandlers(
        ActionRouter router,
        ServerContext context) {

        /*
         * =========================
         * 查询当前学期选课批次
         * =========================
         */
        router.register(
            CourseActions.LIST_BATCHES,
            request ->
                listBatches(
                    request,
                    context));

        /*
         * =========================
         * 查询方案内课程
         * =========================
         */
        router.register(
            CourseActions.LIST_PLAN_COURSES,
            request ->
                listPlanCourses(
                    request,
                    context));

        /*
         * =========================
         * 查询方案外课程
         * =========================
         */
        router.register(
            CourseActions.LIST_SUBSTITUTE_COURSES,
            request ->
                listSubstituteCourses(
                    request,
                    context));

        /*
         * =========================
         * 选择教学班
         * =========================
         */
        router.register(
            CourseActions.SELECT_COURSE,
            request ->
                selectCourse(
                    request,
                    context));

        /*
         * =========================
         * 查询已选课程
         * =========================
         */
        router.register(
            CourseActions.LIST_ENROLLMENTS,
            request ->
                listEnrollments(
                    request,
                    context));

        /*
         * =========================
         * 退课
         * =========================
         */
        router.register(
            CourseActions.DROP_COURSE,
            request ->
                dropCourse(
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
         * =========================
         * 1. 获取登录用户
         * =========================
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
         * =========================
         * 2. 校验请求数据
         * =========================
         */
        if (!(request.getData()
            instanceof BatchRequest batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Plan-course request must contain a BatchRequest.");
        }

        /*
         * =========================
         * 3. 查询课程
         * =========================
         */
        return Response.success(
            request,
            "Plan courses loaded.",
            new ArrayList<>(
                planService.listPlanCourses(
                    batchRequest.getBatchId(),
                    session.getUserId())));
    }

    /**
     * 查询方案外课程。
     */
    private Response listSubstituteCourses(
        Request request,
        ServerContext context) {

        /*
         * =========================
         * 1. 获取登录用户
         * =========================
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
         * =========================
         * 2. 校验请求
         * =========================
         */
        if (!(request.getData()
            instanceof BatchRequest batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Substitute-course request must contain a BatchRequest.");
        }

        /*
         * =========================
         * 3. 查询方案外课程
         * =========================
         */
        return Response.success(
            request,
            "Substitute courses loaded.",
            new ArrayList<>(
                substitutionService
                    .listSubstituteCourses(
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
         * =========================
         * 1. 当前登录用户
         * =========================
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
         * =========================
         * 2. 请求必须包含 SelectCourseRequest
         * =========================
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
         * =========================
         * 3. 服务端执行真正业务校验
         * =========================
         */
        CourseSelectionResult result =
            selectionService.selectCourse(
                session.getUserId(),
                selectRequest.getBatchId(),
                selectRequest.getOfferingId());

        /*
         * =========================
         * 4. 业务失败
         * =========================
         */
        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        /*
         * =========================
         * 5. 成功
         * =========================
         */
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

        /*
         * =========================
         * 1. 登录检查
         * =========================
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
                "Please log in before viewing enrollments.");
        }

        /*
         * =========================
         * 2. BatchRequest
         * =========================
         */
        if (!(request.getData()
            instanceof BatchRequest batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Enrollment request must contain a BatchRequest.");
        }

        /*
         * =========================
         * 3. 查询已选课程
         * =========================
         */
        return Response.success(
            request,
            "Enrollments loaded.",
            new ArrayList<>(
                enrollmentService
                    .listEnrollments(
                        session.getUserId(),
                        batchRequest.getBatchId())));
    }

    /**
     * 学生退课。
     */
    private Response dropCourse(
        Request request,
        ServerContext context) {

        /*
         * =========================
         * 1. 登录检查
         * =========================
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
                "Please log in before dropping courses.");
        }

        /*
         * =========================
         * 2. 校验 DropCourseRequest
         * =========================
         */
        if (!(request.getData()
            instanceof DropCourseRequest
            dropRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Drop-course request is invalid.");
        }

        /*
         * =========================
         * 3. 服务端执行退课
         * =========================
         */
        CourseDropResult result =
            enrollmentService.dropCourse(
                session.getUserId(),
                dropRequest.getBatchId(),
                dropRequest.getEnrollmentId());

        /*
         * =========================
         * 4. 业务失败
         * =========================
         */
        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        /*
         * =========================
         * 5. 退课成功
         * =========================
         */
        return Response.success(
            request,
            result.message(),
            null);
    }
}
