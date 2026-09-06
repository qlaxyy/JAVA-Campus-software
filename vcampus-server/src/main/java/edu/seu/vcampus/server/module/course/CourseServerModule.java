package edu.seu.vcampus.server.module.course;
import edu.seu.vcampus.common.course.AdminUpdateOfferingRequest;
import edu.seu.vcampus.common.course.BatchRequest;
import edu.seu.vcampus.common.course.CourseActions;
import edu.seu.vcampus.common.course.DropCourseRequest;
import edu.seu.vcampus.common.course.PeCourseListRequest;
import edu.seu.vcampus.common.course.SelectCourseRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.student.StudentMemoryRepository;
import edu.seu.vcampus.common.course.GeneralCourseListRequest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Objects;
import edu.seu.vcampus.common.course.CourseSearchRequest;
import edu.seu.vcampus.common.course.AdminForceDropCourseRequest;
import edu.seu.vcampus.common.course.AdminForceSelectCourseRequest;
import edu.seu.vcampus.common.course.AdminListStudentEnrollmentsRequest;
import edu.seu.vcampus.common.course.CourseInfo;
import java.util.List;
import edu.seu.vcampus.common.course.AdminUpdateCourseRequest;
import edu.seu.vcampus.common.course.AdminUpdateBatchRequest;

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
     * 教学班管理业务。
     */
    private final CourseOfferingAdministrationService
        offeringAdministrationService;
    /**
     * 方案内课程业务。
     */
    private final CoursePlanService
        planService;

    /**
     * 方案外课程业务。
     */
    private final CourseSubstitutionService
        substitutionService;

    /**
     * 体育课程业务。
     */
    private final PeCourseService
        peCourseService;
    /**
     * 通选课程业务。
     */
    private final GeneralCourseService
        generalCourseService;
    /**
     * 全校课程查询业务。
     */
    private final CourseSearchService
        searchService;
    /**
     * 选课业务。
     */
    private final CourseSelectionService
        selectionService;
    /**
     * 教务强制操作日志。
     */
    private final CourseAdminAuditService
        adminAuditService;
    /**
     * 已选课程 / 退课业务。
     */
    private final CourseEnrollmentService
        enrollmentService;

    /**
     * 默认构造器。
     *
     * 当前开发阶段主要使用
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
        this.adminAuditService =
            new CourseAdminAuditService(
                clock);
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
         * 3. 方案内课程 Repository
         * =========================
         */
        CoursePlanRepository planRepository =
            new InMemoryCoursePlanRepository();

        /*
         * =========================
         * 4. 方案外课程 Repository
         * =========================
         */
        CourseSubstitutionRepository
            substitutionRepository =
            new InMemoryCourseSubstitutionRepository();

        /*
         * =========================
         * 5. 体育课程 Repository
         * =========================
         */
        PeCourseRepository peCourseRepository =
            new InMemoryPeCourseRepository();
        /*
         * =========================
         * 通选课程 Repository
         * =========================
         */
        GeneralCourseRepository generalCourseRepository =
            new InMemoryGeneralCourseRepository();
        /*
         * =========================
         * 6. 当前选课记录 Repository
         * =========================
         *
         * 所有 Service 必须共享同一个
         * enrollmentRepository。
         *
         * 否则会出现：
         *
         * selectionService 选课成功，
         * 但 planService / peCourseService
         * 看不到这条记录。
         */
        /*
         * =========================
         * 全校课程目录 Repository
         * =========================
         */
        CourseCatalogRepository catalogRepository =
            new InMemoryCourseCatalogRepository();
        CourseEnrollmentRepository
            enrollmentRepository =
            new InMemoryCourseEnrollmentRepository();
        CourseOfferingSettingsRepository
            offeringSettingsRepository =
            new InMemoryCourseOfferingSettingsRepository();

        this.offeringAdministrationService =
            new CourseOfferingAdministrationService(
                batchService,
                planRepository,
                substitutionRepository,
                peCourseRepository,
                generalCourseRepository,
                enrollmentRepository,
                offeringSettingsRepository);

        /*
         * =========================
         * 7. 历史修读 Repository
         * =========================
         */
        CourseHistoryRepository historyRepository =
            new InMemoryCourseHistoryRepository();

        /*
         * =========================
         * 8. 学籍 Repository
         * =========================
         *
         * 体育课需要读取学生性别。
         *
         * 当前直接复用现有
         * StudentMemoryRepository。
         */
        StudentMemoryRepository
            studentRepository =
            new StudentMemoryRepository();

        /*
         * =========================
         * 9. 学生性别适配 Repository
         * =========================
         */
        StudentGenderRepository genderRepository =
            new StudentProfileGenderRepository(
                studentRepository);

        /*
         * =========================
         * 10. 保存批次 Service
         * =========================
         */
        this.batchService =
            batchService;

        /*
         * =========================
         * 11. 体育课程 Service
         * =========================
         *
         * 要先创建体育 Service，
         * 因为 CourseSelectionService
         * 后面需要使用它。
         */
        this.peCourseService =
            new PeCourseService(
                batchService,
                peCourseRepository,
                planRepository,
                substitutionRepository,
                generalCourseRepository,
                enrollmentRepository,
                genderRepository);
        /*
         * =========================
         * 通选课程 Service
         * =========================
         */
        this.generalCourseService =
            new GeneralCourseService(
                batchService,
                generalCourseRepository,
                planRepository,
                substitutionRepository,
                peCourseRepository,
                enrollmentRepository);
        /*
         * =========================
         * 全校课程查询 Service
         * =========================
         */
        this.searchService =
            new CourseSearchService(
                catalogRepository);
        /*
         * =========================
         * 12. 方案内课程 Service
         * =========================
         *
         * peCourseRepository 用于：
         *
         * 体育课已选后，
         * 方案内页面也能检测时间冲突。
         */
        this.planService =
            new CoursePlanService(
                batchService,
                planRepository,
                generalCourseRepository,
                substitutionRepository,
                peCourseRepository,
                enrollmentRepository,
                historyRepository);
        /*
         * =========================
         * 13. 方案外课程 Service
         * =========================
         *
         * 同样加入体育课程，
         * 用于跨页面时间冲突检测。
         */
        this.substitutionService =
            new CourseSubstitutionService(
                batchService,
                substitutionRepository,
                planRepository,
                peCourseRepository,
                generalCourseRepository,
                enrollmentRepository,
                historyRepository);
        /*
         * =========================
         * 14. 选课 Service
         * =========================
         *
         * 当前支持：
         *
         * - 方案内课程
         * - 方案外课程
         * - 体育课程
         */
        this.selectionService =
            new CourseSelectionService(
                batchService,
                planRepository,
                substitutionRepository,
                peCourseService,
                generalCourseService,
                enrollmentRepository,
                historyRepository);
        this.selectionService
            .setOfferingAdministrationService(
                this.offeringAdministrationService);
        /*
         * =========================
         * 15. 已选课程 / 退课 Service
         * =========================
         *
         * peCourseRepository 用于
         * 把体育选课记录解析回
         * CourseInfo / OfferingInfo。
         */
        this.enrollmentService =
            new CourseEnrollmentService(
                batchService,
                planRepository,
                substitutionRepository,
                peCourseRepository,
                generalCourseRepository,
                enrollmentRepository);
    }

    /**
     * 测试或依赖注入使用的构造器。
     */
    CourseServerModule(
        CourseBatchService batchService,
        CoursePlanService planService,
        CourseSubstitutionService substitutionService,
        PeCourseService peCourseService,
        GeneralCourseService generalCourseService,
        CourseSelectionService selectionService,
        CourseEnrollmentService enrollmentService) {
        this.adminAuditService =
            new CourseAdminAuditService(
                Clock.systemDefaultZone());
        this.batchService =
            Objects.requireNonNull(
                batchService,
                "batchService must not be null");

        this.planService =
            Objects.requireNonNull(
                planService,
                "planService must not be null");

        this.substitutionService =
            Objects.requireNonNull(
                substitutionService,
                "substitutionService must not be null");

        this.peCourseService =
            Objects.requireNonNull(
                peCourseService,
                "peCourseService must not be null");
        this.generalCourseService =
            Objects.requireNonNull(
                generalCourseService,
                "generalCourseService must not be null");
        /*
         * 测试构造器暂时使用独立的
         * InMemory 全校课程目录。
         */
        this.searchService =
            new CourseSearchService(
                new InMemoryCourseCatalogRepository());
        this.selectionService =
            Objects.requireNonNull(
                selectionService,
                "selectionService must not be null");

        this.enrollmentService =
            Objects.requireNonNull(
                enrollmentService,
                "enrollmentService must not be null");
        CoursePlanRepository adminPlanRepository =
            new InMemoryCoursePlanRepository();

        CourseSubstitutionRepository
            adminSubstitutionRepository =
            new InMemoryCourseSubstitutionRepository();

        PeCourseRepository adminPeRepository =
            new InMemoryPeCourseRepository();

        GeneralCourseRepository
            adminGeneralRepository =
            new InMemoryGeneralCourseRepository();

        CourseEnrollmentRepository
            adminEnrollmentRepository =
            new InMemoryCourseEnrollmentRepository();

        this.offeringAdministrationService =
            new CourseOfferingAdministrationService(
                this.batchService,
                adminPlanRepository,
                adminSubstitutionRepository,
                adminPeRepository,
                adminGeneralRepository,
                adminEnrollmentRepository,
                new InMemoryCourseOfferingSettingsRepository());
    }

    /**
     * 模块名称。
     */
    @Override
    public String id() {

        return ModuleNames.COURSE;
    }
    /**
     * 课程模块统一使用登录账号作为学号。
     */
    private String studentId(
        SessionInfo session) {

        return session
            .getUsername()
            .trim();
    }
    /**
     * 注册选课模块服务器 Action。
     */
    @Override
    public void registerHandlers(
        ActionRouter router,
        ServerContext context) {
        router.register(
            CourseActions.ADMIN_LIST_AUDIT_LOGS,
            request ->
                adminListAuditLogs(
                    request,
                    context));
        /*
         * =========================
         * 查询选课批次
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
         * 教务修改选课批次
         * =========================
         */
        router.register(
            CourseActions.ADMIN_UPDATE_BATCH,
            request ->
                adminUpdateBatch(
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
         * 查询体育课程
         * =========================
         */
        router.register(
            CourseActions.LIST_PE_COURSES,
            request ->
                listPeCourses(
                    request,
                    context));
        /*
         * =========================
         * 查询通选课程
         * =========================
         */
        router.register(
            CourseActions.LIST_GENERAL_COURSES,
            request ->
                listGeneralCourses(
                    request,
                    context));
        /*
         * =========================
         * 全校课程查询
         * =========================
         */
        router.register(
            CourseActions.SEARCH_OFFERINGS,
            request ->
                searchOfferings(
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
         * 教务查询全部教学班
         * =========================
         */
        router.register(
            CourseActions.ADMIN_LIST_OFFERINGS,
            request ->
                adminListOfferings(
                    request,
                    context));
        /*
         * =========================
         * 教务修改课程基本信息
         * =========================
         */
        router.register(
            CourseActions.ADMIN_UPDATE_COURSE,
            request ->
                adminUpdateCourse(
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
        /*
         * =========================
         * 教务查询学生已选课程
         * =========================
         */
        router.register(
            CourseActions.ADMIN_LIST_STUDENT_ENROLLMENTS,
            request ->
                adminListStudentEnrollments(
                    request,
                    context));

        /*
         * =========================
         * 教务强制选课
         * =========================
         */
        router.register(
            CourseActions.ADMIN_FORCE_SELECT_COURSE,
            request ->
                adminForceSelectCourse(
                    request,
                    context));

        /*
         * =========================
         * 教务强制退课
         * =========================
         */
        router.register(
            CourseActions.ADMIN_FORCE_DROP_COURSE,
            request ->
                adminForceDropCourse(
                    request,
                    context));

        router.register(
            CourseActions.ADMIN_UPDATE_OFFERING,
            request ->
                adminUpdateOffering(
                    request,
                    context));
    }
    /**
     * 教务修改课程基本信息。
     */
    private Response adminUpdateCourse(
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
                "请先登录。");
        }

        /*
         * =========================
         * 2. 教务权限检查
         * =========================
         */
        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有课程管理权限。");
        }

        /*
         * =========================
         * 3. 请求类型检查
         * =========================
         */
        if (!(request.getData()
            instanceof AdminUpdateCourseRequest
            updateRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "课程修改请求格式错误。");
        }

        /*
         * =========================
         * 4. 修改课程
         * =========================
         */
        CourseUpdateResult result =
            offeringAdministrationService
                .updateCourse(
                    updateRequest.getBatchId(),
                    updateRequest.getCourseId(),
                    updateRequest.getCourseCode(),
                    updateRequest.getCourseName(),
                    updateRequest.getCredits(),
                    updateRequest.getCourseType());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }
        /*
         * 记录课程修改日志。
         */
        adminAuditService
            .recordUpdateCourse(
                session.getUsername(),
                updateRequest.getBatchId(),
                updateRequest.getCourseId(),
                updateRequest.getCourseCode(),
                updateRequest.getCourseName(),
                updateRequest.getCredits(),
                updateRequest.getCourseType(),
                updateRequest.getReason());
        /*
         * =========================
         * 5. 返回修改后的课程
         * =========================
         */
        return Response.success(
            request,
            result.message(),
            result.course());
    }
    /**
     * 教务查询指定学生的已选课程。
     */
    private Response adminListStudentEnrollments(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }

        if (!(request.getData()
            instanceof AdminListStudentEnrollmentsRequest
            adminRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "学生选课查询请求无效。");
        }

        return Response.success(
            request,
            "学生已选课程加载成功。",
            new ArrayList<>(
                enrollmentService
                    .listAdminEnrollments(
                        adminRequest
                            .getStudentId())));
    }
    /**
     * 教务修改选课批次。
     */
    private Response adminUpdateBatch(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课批次管理权限。");
        }

        if (!(request.getData()
            instanceof AdminUpdateBatchRequest
            updateRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "选课批次修改请求格式错误。");
        }

        BatchUpdateResult result =
            batchService.updateBatch(
                updateRequest.getBatchId(),
                updateRequest.getSemester(),
                updateRequest.getBatchName(),
                updateRequest.getBatchType(),
                updateRequest.getStartTime(),
                updateRequest.getEndTime(),
                updateRequest.getStatus(),
                updateRequest.isAllowSelect(),
                updateRequest.isAllowDrop());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }
        adminAuditService
            .recordUpdateBatch(
                session.getUsername(),
                updateRequest.getBatchId(),
                updateRequest.getSemester(),
                updateRequest.getBatchName(),
                updateRequest.getBatchType(),
                updateRequest.getStartTime(),
                updateRequest.getEndTime(),
                updateRequest.getStatus(),
                updateRequest.isAllowSelect(),
                updateRequest.isAllowDrop(),
                updateRequest.getReason());
        return Response.success(
            request,
            result.message(),
            result.batch());
    }
    /**
     * 将教务修改的课程信息和教学班设置
     * 应用到学生端课程列表。
     */
    private ArrayList<CourseInfo>
    applyStudentCourseSettings(
        long batchId,
        List<CourseInfo> courses) {

        return new ArrayList<>(
            courses.stream()
                .map(course ->
                    offeringAdministrationService
                        .applyStudentSettings(
                            batchId,
                            course))
                .toList());
    }
    /**
     * 教务查询指定批次的全部教学班。
     */
    private Response adminListOfferings(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }

        if (!(request.getData()
            instanceof BatchRequest batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "教学班查询请求无效。");
        }

        if (batchService.findBatch(
            batchRequest.getBatchId()) == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "选课批次不存在。");
        }

        return Response.success(
            request,
            "教学班列表加载成功。",
            new ArrayList<>(offeringAdministrationService
                .listCourses(
                    batchRequest.getBatchId()))
               );
    }

    /**
     * 教务修改教学班容量和开放状态。
     */
    private Response adminUpdateOffering(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }

        if (!(request.getData()
            instanceof AdminUpdateOfferingRequest
            adminRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "教学班修改请求无效。");
        }

        CourseOfferingUpdateResult result =
            offeringAdministrationService
                .updateOffering(
                    adminRequest.getBatchId(),
                    adminRequest.getOfferingId(),
                    adminRequest.getCapacity(),
                    adminRequest.isOpen());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }
        adminAuditService.recordUpdateOffering(
            session.getUsername(),
            adminRequest.getBatchId(),
            adminRequest.getOfferingId(),
            adminRequest.getCapacity(),
            adminRequest.isOpen(),
            adminRequest.getReason());
        return Response.success(
            request,
            result.message(),
            null);
    }
    /**
     * 教务为指定学生强制选课。
     */
    private Response adminForceSelectCourse(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }

        if (!(request.getData()
            instanceof AdminForceSelectCourseRequest
            adminRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "强制选课请求无效。");
        }

        CourseSelectionResult result =
            selectionService.forceSelectCourse(
                adminRequest.getStudentId(),
                adminRequest.getBatchId(),
                adminRequest.getOfferingId());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }
        adminAuditService.recordForceSelect(
            session.getUsername(),
            adminRequest.getStudentId(),
            adminRequest.getBatchId(),
            adminRequest.getOfferingId(),
            adminRequest.getReason());
        return Response.success(
            request,
            result.message(),
            null);
    }
    /**
     * 查询教务强制操作日志。
     */
    private Response adminListAuditLogs(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }

        if (request.getData() != null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "日志查询请求不需要携带数据。");
        }

        return Response.success(
            request,
            "教务操作日志加载成功。",
            new ArrayList<>(
                adminAuditService
                    .listAuditLogs()));
    }
    /**
     * 教务为指定学生强制退课。
     */
    private Response adminForceDropCourse(
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
                "请先登录。");
        }

        if (!session.canAdminister(
            ModuleNames.COURSE)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }

        if (!(request.getData()
            instanceof AdminForceDropCourseRequest
            adminRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "强制退课请求无效。");
        }

        CourseDropResult result =
            enrollmentService.forceDropCourse(
                adminRequest.getStudentId(),
                adminRequest.getEnrollmentId());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }
        adminAuditService.recordForceDrop(
            session.getUsername(),
            adminRequest.getStudentId(),
            adminRequest.getEnrollmentId(),
            adminRequest.getReason());
        return Response.success(
            request,
            result.message(),
            null);
    }
    /**
     * 查询当前学期选课批次。
     */
    private Response listBatches(
        Request request,
        ServerContext context) {

        /*
         * =========================
         * 1. 登录检查
         * =========================
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
         * =========================
         * 2. 请求数据必须为空
         * =========================
         */
        if (request.getData() != null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Batch-list data must be empty.");
        }

        /*
         * =========================
         * 3. 返回批次
         * =========================
         */
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
                "Please log in before using the course module.");
        }

        /*
         * =========================
         * 2. BatchRequest
         * =========================
         */
        if (!(request.getData()
            instanceof BatchRequest
            batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Plan-course request must contain a BatchRequest.");
        }

        /*
         * =========================
         * 3. 查询方案内课程
         * =========================
         */
        return Response.success(
            request,
            "Plan courses loaded.",
            applyStudentOfferingSettings(
                batchRequest.getBatchId(),
                planService.listPlanCourses(
                    batchRequest.getBatchId(),
                    studentId(session))));
    }

    /**
     * 查询方案外课程。
     */
    private Response listSubstituteCourses(
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
                "Please log in before using the course module.");
        }

        /*
         * =========================
         * 2. BatchRequest
         * =========================
         */
        if (!(request.getData()
            instanceof BatchRequest
            batchRequest)) {

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
            applyStudentOfferingSettings(
                batchRequest.getBatchId(),
                substitutionService
                    .listSubstituteCourses(
                        batchRequest.getBatchId(),
                        studentId(session))));
    }

    /**
     * 查询体育课程。
     */
    private Response listPeCourses(
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
                "Please log in before using the course module.");
        }

        /*
         * =========================
         * 2. PeCourseListRequest
         * =========================
         */
        if (!(request.getData()
            instanceof PeCourseListRequest
            peRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "PE-course request must contain a PeCourseListRequest.");
        }

        /*
         * =========================
         * 3. 查询体育课程
         * =========================
         *
         * session.getUserId()
         *
         * 例如：
         * U-STUDENT-001
         *
         * 用于当前选课记录。
         *
         *
         * session.getUsername()
         *
         * 例如：
         * student001
         *
         * 当前 demo 中用于查询
         * StudentMemoryRepository。
         */
        return Response.success(
            request,
            "PE courses loaded.",
            applyStudentOfferingSettings(
                peRequest.getBatchId(),
                peCourseService.listPeCourses(
                    peRequest.getBatchId(),
                    peRequest.getSportProject(),
                    studentId(session),
                    studentId(session))));
    }
    /**
     * 查询通选课程。
     */
    private Response listGeneralCourses(
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
                "Please log in before using the course module.");
        }

        /*
         * =========================
         * 2. GeneralCourseListRequest
         * =========================
         */
        if (!(request.getData()
            instanceof GeneralCourseListRequest
            generalRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "General-course request must contain a GeneralCourseListRequest.");
        }

        /*
         * =========================
         * 3. 查询通选课程
         * =========================
         */
        return Response.success(
            request,
            "General courses loaded.",
            applyStudentOfferingSettings(
                generalRequest.getBatchId(),
                generalCourseService.listGeneralCourses(
                    generalRequest.getBatchId(),
                    generalRequest.getGeneralCategory(),
                    studentId(session))));
    }
    /**
     * 全校课程查询。
     */
    private Response searchOfferings(
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
                "Please log in before searching courses.");
        }

        /*
         * =========================
         * 2. CourseSearchRequest
         * =========================
         */
        if (!(request.getData()
            instanceof CourseSearchRequest
            searchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "Course-search request must contain a CourseSearchRequest.");
        }

        /*
         * =========================
         * 3. 执行查询
         * =========================
         */
        return Response.success(
            request,
            "Courses loaded.",
            searchService.search(
                searchRequest));
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
         * 2. SelectCourseRequest
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
         * 3. 服务端执行选课
         * =========================
         *
         * userId：
         *
         * 用于选课记录。
         *
         * studentId：
         *
         * 当前使用 username
         * 对应 StudentMemoryRepository
         * 中的 studentId。
         */
        CourseSelectionResult result =
            selectionService
                .selectCourse(
                    studentId(session),
                    studentId(session),
                    selectRequest
                        .getBatchId(),
                    selectRequest
                        .getOfferingId());

        /*
         * =========================
         * 4. 选课失败
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
         * 5. 选课成功
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
            instanceof BatchRequest
            batchRequest)) {

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
                        studentId(session),
                        batchRequest
                            .getBatchId())));
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
         * 2. DropCourseRequest
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
            enrollmentService
                .dropCourse(
                    session.getUsername(),
                    dropRequest
                        .getBatchId(),
                    dropRequest
                        .getEnrollmentId());

        /*
         * =========================
         * 4. 退课失败
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

    /**
     * 把教务修改后的教学班设置
     * 应用到学生端课程列表。
     */
    private ArrayList<CourseInfo>
    applyStudentOfferingSettings(
        long batchId,
        List<CourseInfo> courses) {

        return new ArrayList<>(
            courses.stream()
                .map(course ->
                    offeringAdministrationService
                        .applyStudentSettings(
                            batchId,
                            course))
                .toList());
    }
}
