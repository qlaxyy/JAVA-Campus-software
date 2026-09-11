package edu.seu.vcampus.server.module.course;
import edu.seu.vcampus.common.course.AdminUpdateOfferingRequest;
import edu.seu.vcampus.common.course.AdminCreateOfferingRequest;
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
import edu.seu.vcampus.server.module.student.AccessStudentRepository;
import edu.seu.vcampus.server.module.student.StudentRepository;
import edu.seu.vcampus.server.security.UserDirectory;
import edu.seu.vcampus.server.security.TeacherDirectory;
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
import edu.seu.vcampus.common.course.AdminListGradesRequest;
import edu.seu.vcampus.common.course.AdminUpdateGradeRequest;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.common.course.TeacherListStudentsRequest;
import java.nio.file.Path;
import edu.seu.vcampus.common.course.CourseGradeInfo;
import edu.seu.vcampus.common.course.TeacherStudentInfo;
import edu.seu.vcampus.common.course.TeacherUpdateGradePolicyRequest;
import edu.seu.vcampus.common.course.AdminTeacherAssignmentRequest;
import edu.seu.vcampus.common.course.OfferingTeacherRequest;
import edu.seu.vcampus.common.user.TeacherProfileView;


/**
 * 选课模块服务器入口。
 */
public final class CourseServerModule
    implements ServerModule {
    /**
     * 教学班成绩比例业务。
     */
    private final CourseGradePolicyService
        gradePolicyService;
    /**
     * 成绩管理业务。
     */
    private final CourseGradeService
        gradeService;
    /**
     * 教务选课数据统计业务。
     */
    private final CourseAdminStatisticsService
        statisticsService;
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
    private final CourseTeacherService
        teacherService;
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
    public CourseServerModule() {

        this(null, null, null);
    }
    /**
     * 创建使用 Access 数据库的课程模块。
     */
    public static CourseServerModule createAccessBacked(
        Path databasePath) {

        return new CourseServerModule(
            new AccessDatabase(
                Objects.requireNonNull(
                    databasePath)),
            new UserDirectory() {
                @Override
                public java.util.Optional<edu.seu.vcampus.server.security.UserIdentity>
                        findByUserId(String userId) {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<edu.seu.vcampus.server.security.UserIdentity>
                        findByCampusCardNumber(String campusCardNumber) {
                    return java.util.Optional.empty();
                }
            },
            null);
    }

    /**
     * 创建使用 Access 数据库和公共用户目录的课程模块。
     */
    public static CourseServerModule createAccessBacked(
        Path databasePath,
        UserDirectory users) {

        return createAccessBacked(databasePath, users, null);
    }

    /** Creates an Access-backed course module with live user and teacher directories. */
    public static CourseServerModule createAccessBacked(
        Path databasePath,
        UserDirectory users,
        TeacherDirectory teachers) {

        return new CourseServerModule(
            new AccessDatabase(
                Objects.requireNonNull(
                    databasePath)),
            Objects.requireNonNull(
                users),
            teachers);
    }
    /**
     * 根据数据库配置创建课程模块。
     *
     * database 为 null 时使用内存仓库，
     * 不为 null 时使用 Access 仓库。
     */
    private CourseServerModule(
        AccessDatabase database,
        UserDirectory users,
        TeacherDirectory teachers) {


            /*
             * =========================
             * 1. 时钟
             * =========================
             */
            Clock clock =
                Clock.systemDefaultZone();
        CourseAdminAuditRepository
            adminAuditRepository =
            database == null
                ? new InMemoryCourseAdminAuditRepository()
                : new AccessCourseAdminAuditRepository(
                database);

        CourseTeacherAssignmentRepository
            teacherAssignmentRepository =
            database == null
                ? new InMemoryCourseTeacherAssignmentRepository()
                : new AccessCourseTeacherAssignmentRepository(
                database);
        /*
         * =========================
         * 教学班成绩比例
         * =========================
         */
        CourseGradePolicyRepository
            gradePolicyRepository =
            database == null
                ? new InMemoryCourseGradePolicyRepository()
                : new AccessCourseGradePolicyRepository(
                database);

        this.gradePolicyService =
            new CourseGradePolicyService(
                gradePolicyRepository,
                clock);
        this.adminAuditService =
            new CourseAdminAuditService(
                clock,
                adminAuditRepository);
            /*
             * =========================
             * 2. 选课批次
             * =========================
             */
        CourseBatchSettingsRepository
            batchSettingsRepository =
            database == null
                ? new InMemoryCourseBatchSettingsRepository()
                : new AccessCourseBatchSettingsRepository(
                database);
        CourseBatchRepository batchRepository =
            database == null
                ? new InMemoryCourseBatchRepository(
                clock)
                : new AccessCourseBatchRepository(
                database,
                clock);

        CourseBatchService batchService =
            new CourseBatchService(
                batchRepository,
                clock,
                batchSettingsRepository);

            /*
             * =========================
             * 3. 方案内课程 Repository
             * =========================
             */
        /*
         * 内存仓库同时作为数据库首次启动时的
         * 演示数据来源。
         */
        CoursePlanRepository
            seedPlanRepository =
            new InMemoryCoursePlanRepository();

        CoursePlanRepository planRepository =
            database == null
                ? seedPlanRepository
                : new AccessCoursePlanRepository(
                database,
                seedPlanRepository);
            /*
             * =========================
             * 4. 方案外课程 Repository
             * =========================
             */
        CourseSubstitutionRepository
            seedSubstitutionRepository =
            new InMemoryCourseSubstitutionRepository();

        CourseSubstitutionRepository
            substitutionRepository =
            database == null
                ? seedSubstitutionRepository
                : new AccessCourseSubstitutionRepository(
                database,
                seedSubstitutionRepository);

            /*
             * =========================
             * 5. 体育课程 Repository
             * =========================
             */
        PeCourseRepository seedPeCourseRepository =
            new InMemoryPeCourseRepository();

        PeCourseRepository peCourseRepository =
            database == null
                ? seedPeCourseRepository
                : new AccessPeCourseRepository(
                database,
                seedPeCourseRepository);
            /*
             * =========================
             * 通选课程 Repository
             * =========================
             */
        GeneralCourseRepository seedGeneralCourseRepository =
            new InMemoryGeneralCourseRepository();

        GeneralCourseRepository generalCourseRepository =
            database == null
                ? seedGeneralCourseRepository
                : new AccessGeneralCourseRepository(
                database,
                seedGeneralCourseRepository);
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
         * 当前选课记录 Repository
         * =========================
         *
         * 普通构造器使用内存仓库，
         * Access 模式使用数据库仓库。
         *
         * 所有选课 Service 必须共享
         * 同一个 enrollmentRepository。
         */
        CourseEnrollmentRepository
            enrollmentRepository =
            database == null
                ? new InMemoryCourseEnrollmentRepository()
                : new AccessCourseEnrollmentRepository(
                database);
        CourseOfferingSettingsRepository
            offeringSettingsRepository =
            database == null
                ? new InMemoryCourseOfferingSettingsRepository()
                : new AccessCourseOfferingSettingsRepository(
                database);


        CourseSettingsRepository
            courseSettingsRepository =
            database == null
                ? new InMemoryCourseSettingsRepository()
                : new AccessCourseSettingsRepository(
                database);
        this.offeringAdministrationService =
            new CourseOfferingAdministrationService(
                planRepository,
                substitutionRepository,
                peCourseRepository,
                generalCourseRepository,
                enrollmentRepository,
                offeringSettingsRepository,
                courseSettingsRepository);
        CourseCatalogRepository catalogRepository =
            new LiveCourseCatalogRepository(
                batchService,
                this.offeringAdministrationService);
        this.teacherService =
            new CourseTeacherService(
                this.offeringAdministrationService,
                enrollmentRepository,
                teacherAssignmentRepository,
                teachers);
            /*
             * =========================
             * 7. 历史修读 Repository
             * =========================
             */
        CourseHistoryRepository historyRepository =
            database == null
                ? new InMemoryCourseHistoryRepository()
                : new AccessCourseHistoryRepository(
                database);

            /*
             * =========================
             * 8. 学籍 Repository
             * =========================
             *
             * 体育课需要读取学生性别。
             *
             * 内存模式使用内存学籍；生产模式
             * 读取同一个 Access 学籍表。
             */
            StudentRepository studentRepository =
                database == null
                    ? new StudentMemoryRepository()
                    : new AccessStudentRepository(
                        database,
                        Objects.requireNonNull(users));

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
            this.statisticsService =
                new CourseAdminStatisticsService(
                    this.batchService,
                    this.offeringAdministrationService);
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
        CourseGradeRepository gradeRepository =
            database == null
                ? new InMemoryCourseGradeRepository()
                : new AccessCourseGradeRepository(
                database);

        this.gradeService =
            new CourseGradeService(
                this.enrollmentService,
                gradeRepository,
                this.gradePolicyService,
                clock);

        // 原来默认构造器的全部内容放在这里
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

        this.gradePolicyService =
            new CourseGradePolicyService(
                new InMemoryCourseGradePolicyRepository(),
                Clock.systemDefaultZone());
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
        this.gradeService =
            new CourseGradeService(
                this.enrollmentService,
                new InMemoryCourseGradeRepository(),
                this.gradePolicyService,
                Clock.systemDefaultZone());
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
                adminPlanRepository,
                adminSubstitutionRepository,
                adminPeRepository,
                adminGeneralRepository,
                adminEnrollmentRepository,
                new InMemoryCourseOfferingSettingsRepository(),
                new InMemoryCourseSettingsRepository());
        this.searchService =
            new CourseSearchService(
                new LiveCourseCatalogRepository(
                    this.batchService,
                    this.offeringAdministrationService));
        this.statisticsService =
            new CourseAdminStatisticsService(
                this.batchService,
                this.offeringAdministrationService);

        CourseTeacherAssignmentRepository
            teacherAssignmentRepository =
            new InMemoryCourseTeacherAssignmentRepository();

        this.teacherService =
            new CourseTeacherService(
                this.offeringAdministrationService,
                adminEnrollmentRepository,
                teacherAssignmentRepository,
                null);

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
         * 教务查询选课统计
         * =========================
         */
        router.register(
            CourseActions.ADMIN_GET_STATISTICS,
            request ->
                adminGetStatistics(
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
         * 教务查询学生成绩
         * =========================
         */
        router.register(
            CourseActions.ADMIN_LIST_GRADES,
            request ->
                adminListGrades(
                    request,
                    context));

        /*
         * =========================
         * 超级管理员修改成绩
         * =========================
         */
        router.register(
            CourseActions.ADMIN_UPDATE_GRADE,
            request ->
                adminUpdateGrade(
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
        /*
         * =========================
         * 教师查询本人教学班
         * =========================
         */
        router.register(
            CourseActions.TEACHER_LIST_OFFERINGS,
            request ->
                teacherListOfferings(
                    request,
                    context));
        router.register(
            CourseActions.ADMIN_CREATE_OFFERING,
            request ->
                adminCreateOffering(
                    request,
                    context));
        router.register(
            CourseActions.ADMIN_UPDATE_OFFERING,
            request ->
                adminUpdateOffering(
                    request,
                    context));
        /*
         * =========================
         * 教师查询教学班学生名单
         * =========================
         */
        router.register(
            CourseActions.TEACHER_LIST_STUDENTS,
            request ->
                teacherListStudents(
                    request,
                    context));
        /*
         * =========================
         * 教师查询教学班成绩
         * =========================
         */
        router.register(
            CourseActions.TEACHER_LIST_GRADES,
            request ->
                teacherListGrades(
                    request,
                    context));

        /*
         * =========================
         * 教师录入或修改成绩
         * =========================
         */
        router.register(
            CourseActions.TEACHER_UPDATE_GRADE,
            request ->
                teacherUpdateGrade(
                    request,
                    context));
        /*
         * =========================
         * 教师查询成绩比例
         * =========================
         */
        router.register(
            CourseActions.TEACHER_GET_GRADE_POLICY,
            request ->
                teacherGetGradePolicy(
                    request,
                    context));

        /*
         * =========================
         * 教师修改成绩比例
         * =========================
         */
        router.register(
            CourseActions.TEACHER_UPDATE_GRADE_POLICY,
            request ->
                teacherUpdateGradePolicy(
                    request,
                    context));

        router.register(
            CourseActions.ADMIN_LIST_ACTIVE_TEACHERS,
            request ->
                adminListActiveTeachers(
                    request,
                    context));

        router.register(
            CourseActions.ADMIN_LIST_OFFERING_TEACHERS,
            request ->
                adminListOfferingTeachers(
                    request,
                    context));

        router.register(
            CourseActions.ADMIN_ASSIGN_TEACHER,
            request ->
                adminAssignTeacher(
                    request,
                    context));

        router.register(
            CourseActions.ADMIN_REMOVE_TEACHER,
            request ->
                adminRemoveTeacher(
                    request,
                    context));

    }

    /**
     * 教务为已有课程新建教学班。
     */
    private Response adminCreateOffering(
        Request request,
        ServerContext context) {

        SessionInfo session = context.sessions()
            .findSession(request.getToken())
            .orElse(null);

        if (session == null) {
            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "请先登录。");
        }
        if (!session.canAdminister(ModuleNames.COURSE)) {
            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "没有选课管理权限。");
        }
        if (!(request.getData()
            instanceof AdminCreateOfferingRequest
            createRequest)) {
            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "教学班新增请求无效。");
        }

        CourseOfferingCreateResult result =
            offeringAdministrationService.createOffering(
                createRequest.getBatchId(),
                createRequest.getCourseId(),
                createRequest.getClassNo(),
                createRequest.getLocationName(),
                createRequest.getCampusName(),
                createRequest.getTeachingLanguage(),
                createRequest.getCapacity(),
                createRequest.getSchedule());

        if (!result.success()) {
            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        adminAuditService.recordCreateOffering(
            session.getUsername(),
            createRequest.getBatchId(),
            result.offeringId(),
            createRequest.getCourseId(),
            createRequest.getClassNo(),
            createRequest.getReason());

        return Response.success(
            request,
            result.message(),
            result.offeringId());
    }
    /**
     * 教师查询本人教学班的成绩比例。
     */
    private Response teacherGetGradePolicy(
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

        if (context.teachers().findByUserId(session.getUserId()).isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "当前账号不是课程教师。");
        }

        if (!(request.getData()
            instanceof TeacherListStudentsRequest
            policyRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "成绩比例查询请求格式错误。");
        }

        long batchId =
            policyRequest.getBatchId();

        long offeringId =
            policyRequest.getOfferingId();

        if (batchId <= 0
            || offeringId <= 0) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "批次 ID 或教学班 ID 不正确。");
        }

        if (batchService.findBatch(
            batchId) == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "未找到指定选课批次。");
        }

        if (!teacherService.canManageOffering(
            session.getUserId(),
            offeringId)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "你没有该教学班的成绩管理权限。");
        }

        return Response.success(
            request,
            "成绩比例加载成功。",
            gradePolicyService.getPolicy(
                offeringId));
    }
    /**
     * 教师修改本人教学班的成绩比例。
     */
    private Response teacherUpdateGradePolicy(
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

        if (context.teachers().findByUserId(session.getUserId()).isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "当前账号不是课程教师。");
        }

        if (!(request.getData()
            instanceof TeacherUpdateGradePolicyRequest
            updateRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "成绩比例修改请求格式错误。");
        }

        long offeringId =
            updateRequest.getOfferingId();

        if (!teacherService.canManageOffering(
            session.getUserId(),
            offeringId)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "你没有该教学班的成绩管理权限。");
        }

        CourseGradePolicyUpdateResult result =
            gradePolicyService.updatePolicy(
                offeringId,
                updateRequest
                    .getUsualWeightPercent(),
                updateRequest
                    .getFinalExamWeightPercent());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        adminAuditService
            .recordUpdateGradePolicy(
                session.getUsername(),
                offeringId,
                updateRequest
                    .getUsualWeightPercent(),
                updateRequest
                    .getFinalExamWeightPercent(),
                updateRequest.getReason());

        return Response.success(
            request,
            result.message(),
            result.policy());
    }
    /**
     * 教师查询本人负责的教学班。
     */
    private Response teacherListOfferings(
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
         * 2. 公共教师资格检查
         * =========================
         */
        if (context.teachers().findByUserId(session.getUserId()).isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "当前账号没有教师权限。");
        }

        /*
         * =========================
         * 3. 请求数据检查
         * =========================
         */
        if (!(request.getData()
            instanceof BatchRequest
            batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "教师教学班查询请求无效。");
        }

        /*
         * =========================
         * 4. 批次检查
         * =========================
         */
        if (batchService.findBatch(
            batchRequest.getBatchId()) == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "选课批次不存在。");
        }

        /*
         * =========================
         * 5. 查询本人负责的教学班
         * =========================
         */
        return Response.success(
            request,
            "教师教学班加载成功。",
            new ArrayList<>(
                teacherService
                    .listTeacherCourses(
                        session.getUserId(),
                        batchRequest.getBatchId())));


    }
    /**
     * 教师查询自己负责教学班中的学生名单。
     */
    private Response teacherListStudents(
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
         * 2. 教师身份检查
         * =========================
         */
        if (context.teachers().findByUserId(session.getUserId()).isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "当前账号不是课程教师。");
        }

        /*
         * =========================
         * 3. 请求格式检查
         * =========================
         */
        if (!(request.getData()
            instanceof TeacherListStudentsRequest
            listRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "学生名单查询请求格式错误。");
        }

        long batchId =
            listRequest.getBatchId();

        long offeringId =
            listRequest.getOfferingId();

        if (batchId <= 0
            || offeringId <= 0) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "批次 ID 或教学班 ID 不正确。");
        }

        /*
         * =========================
         * 4. 批次检查
         * =========================
         */
        if (batchService.findBatch(
            batchId) == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "未找到指定选课批次。");
        }

        /*
         * =========================
         * 5. 教学班权限检查
         * =========================
         */
        if (!teacherService.canManageOffering(
            session.getUserId(),
            offeringId)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "你没有该教学班的管理权限。");
        }

        /*
         * =========================
         * 6. 返回学生名单
         * =========================
         */
        return Response.success(
            request,
            "学生名单加载成功。",
            new ArrayList<>(
                teacherService.listStudents(
                    session.getUserId(),
                    batchId,
                    offeringId)));
    }
    /**
     * 教师查询自己负责教学班的成绩。
     */
    private Response teacherListGrades(
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

        if (context.teachers().findByUserId(session.getUserId()).isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "当前账号不是课程教师。");
        }

        if (!(request.getData()
            instanceof TeacherListStudentsRequest
            listRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "成绩查询请求格式错误。");
        }

        long batchId =
            listRequest.getBatchId();

        long offeringId =
            listRequest.getOfferingId();

        if (batchId <= 0
            || offeringId <= 0) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "批次 ID 或教学班 ID 不正确。");
        }

        if (batchService.findBatch(
            batchId) == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "未找到指定选课批次。");
        }

        if (!teacherService.canManageOffering(
            session.getUserId(),
            offeringId)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "你没有该教学班的成绩管理权限。");
        }

        List<TeacherStudentInfo> students =
            teacherService.listStudents(
                session.getUserId(),
                batchId,
                offeringId);

        List<CourseGradeInfo> grades =
            new ArrayList<>();

        for (TeacherStudentInfo student
            : students) {

            List<CourseGradeInfo> studentGrades =
                gradeService.listGrades(
                    student.getStudentId());

            for (CourseGradeInfo grade
                : studentGrades) {

                if (grade.getEnrollmentId()
                    == student.getEnrollmentId()
                    && grade.getOfferingId()
                    == offeringId) {

                    grades.add(
                        grade);

                    break;
                }
            }
        }

        return Response.success(
            request,
            "成绩加载成功。",
            new ArrayList<>(
                grades));
    }
    /**
     * 教师录入或修改自己负责教学班的成绩。
     */
    private Response teacherUpdateGrade(
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

        if (context.teachers().findByUserId(session.getUserId()).isEmpty()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "当前账号不是课程教师。");
        }

        if (!(request.getData()
            instanceof AdminUpdateGradeRequest
            updateRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "成绩修改请求格式错误。");
        }

        String studentId =
            updateRequest
                .getStudentId()
                .trim();

        long enrollmentId =
            updateRequest
                .getEnrollmentId();

        double usualScore =
            updateRequest
                .getUsualScore();

        double finalExamScore =
            updateRequest
                .getFinalExamScore();

        if (studentId.isBlank()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "学生学号不能为空。");
        }

        if (enrollmentId <= 0) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "选课记录 ID 不正确。");
        }

        if (!validGradeScore(
            usualScore)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "平时成绩必须在 0 到 100 之间。");
        }

        if (!validGradeScore(
            finalExamScore)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "期末成绩必须在 0 到 100 之间。");
        }

        if (!teacherService.canManageEnrollment(
            session.getUserId(),
            studentId,
            enrollmentId)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "该选课记录不属于你负责的教学班。");
        }

        GradeUpdateResult result =
            gradeService.updateGrade(
                studentId,
                enrollmentId,
                usualScore,
                finalExamScore);

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        adminAuditService.recordUpdateGrade(
            session.getUsername(),
            studentId,
            enrollmentId,
            usualScore,
            finalExamScore,
            result.grade()
                .getTotalScore(),
            updateRequest.getReason());

        return Response.success(
            request,
            result.message(),
            result.grade());
    }
    /**
     * 判断成绩是否在合法范围内。
     */
    private boolean validGradeScore(
        double score) {

        return !Double.isNaN(
            score)
            && !Double.isInfinite(
            score)
            && score >= 0.0
            && score <= 100.0;
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
                teacherService.withCurrentEnrollmentTeacherNames(
                    enrollmentService.listAdminEnrollments(
                        adminRequest
                            .getStudentId()))));
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
    /**
     * 教务查询全部课程和教学班。
     *
     * 课程和教学班不按照选课批次区分。
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

        /*
         * 暂时兼容尚未修改的旧客户端。
         * BatchRequest 中的批次不会参与查询。
         */
        if (request.getData() != null
            && !(request.getData()
            instanceof BatchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "教学班查询请求无效。");
        }

        return Response.success(
            request,
            "课程和教学班列表加载成功。",
            new ArrayList<>(
                offeringAdministrationService
                    .listCourses()));
    }
    /**
     * 教务查询指定批次的选课统计。
     */
    private Response adminGetStatistics(
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
                "没有选课数据统计权限。");
        }

        if (!(request.getData()
            instanceof BatchRequest
            batchRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "选课统计请求格式错误。");
        }

        CourseAdminStatisticsResult result =
            statisticsService.statistics(
                batchRequest.getBatchId());

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        return Response.success(
            request,
            result.message(),
            result.statistics());
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
     * 教务查询指定学生成绩。
     */
    private Response adminListGrades(
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
                "没有成绩查询权限。");
        }

        if (!(request.getData()
            instanceof AdminListGradesRequest
            listRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "成绩查询请求格式错误。");
        }

        if (listRequest.getStudentId() == null
            || listRequest.getStudentId()
            .isBlank()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "学生学号不能为空。");
        }

        return Response.success(
            request,
            "学生成绩加载成功。",
            new ArrayList<>(
                gradeService.listGrades(
                    listRequest.getStudentId())));
    }
    /**
     * 超级管理员新增或修改学生成绩。
     */
    private Response adminUpdateGrade(
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

        /*
         * 修改成绩只允许超级管理员。
         */
        if (session.getRole()
            != Role.SUPER_ADMIN) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_FORBIDDEN,
                "只有超级管理员可以修改成绩。");
        }

        if (!(request.getData()
            instanceof AdminUpdateGradeRequest
            updateRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "成绩修改请求格式错误。");
        }

        double usualScore =
            updateRequest
                .getUsualScore();

        double finalExamScore =
            updateRequest
                .getFinalExamScore();

        if (!validGradeScore(
            usualScore)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "平时成绩必须在 0 到 100 之间。");
        }

        if (!validGradeScore(
            finalExamScore)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "期末成绩必须在 0 到 100 之间。");
        }

        GradeUpdateResult result =
            gradeService.updateGrade(
                updateRequest.getStudentId(),
                updateRequest.getEnrollmentId(),
                usualScore,
                finalExamScore);

        if (!result.success()) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                result.message());
        }

        adminAuditService
            .recordUpdateGrade(
                session.getUsername(),
                updateRequest.getStudentId(),
                updateRequest.getEnrollmentId(),
                usualScore,
                finalExamScore,
                result.grade()
                    .getTotalScore(),
                updateRequest.getReason());

        return Response.success(
            request,
            result.message(),
            result.grade());
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
                teacherService.withCurrentTeacherNames(
                planService.listPlanCourses(
                    batchRequest.getBatchId(),
                    studentId(session)))));
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
                teacherService.withCurrentTeacherNames(
                substitutionService
                    .listSubstituteCourses(
                        batchRequest.getBatchId(),
                        studentId(session)))));
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
         * 20260006
         *
         * 生产模式中用于查询 Access 学籍表。
         */
        return Response.success(
            request,
            "PE courses loaded.",
            applyStudentOfferingSettings(
                peRequest.getBatchId(),
                teacherService.withCurrentTeacherNames(
                peCourseService.listPeCourses(
                    peRequest.getBatchId(),
                    peRequest.getSportProject(),
                    studentId(session),
                    studentId(session)))));
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
                teacherService.withCurrentTeacherNames(
                generalCourseService.listGeneralCourses(
                    generalRequest.getBatchId(),
                    generalRequest.getGeneralCategory(),
                    studentId(session)))));
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
            teacherService.withCurrentSearchTeacherNames(
                searchService.search(searchRequest)));
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
                teacherService.withCurrentEnrollmentTeacherNames(
                    enrollmentService.listEnrollments(
                        studentId(session),
                        batchRequest
                            .getBatchId()))));
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
    /**
     * 教务查询公共有效教师名单。
     */
    private Response adminListActiveTeachers(
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
                "没有任课教师管理权限。");
        }

        List<TeacherProfileView> teachers =
            context.teachers()
                .findActiveTeachers()
                .stream()
                .map(teacher ->
                    new TeacherProfileView(
                        teacher.userId(),
                        teacher.campusCardNumber(),
                        teacher.displayName(),
                        teacher.department(),
                        teacher.title(),
                        true))
                .toList();

        return Response.success(
            request,
            "有效教师名单加载成功。",
            new ArrayList<>(
                teachers));
    }

    /**
     * 教务查询教学班已经分配的教师。
     */
    private Response adminListOfferingTeachers(
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
                "没有任课教师管理权限。");
        }

        if (!(request.getData()
            instanceof OfferingTeacherRequest
            teacherRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "教学班教师查询请求格式错误。");
        }

        List<String> teacherUserIds =
            teacherService.listTeacherUserIds(
                teacherRequest.getOfferingId());

        return Response.success(
            request,
            "教学班任课教师加载成功。",
            new ArrayList<>(
                teacherUserIds));
    }

    /**
     * 教务为教学班分配教师。
     */
    private Response adminAssignTeacher(
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
                "没有任课教师管理权限。");
        }

        if (!(request.getData()
            instanceof AdminTeacherAssignmentRequest
            assignmentRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "任课教师分配请求格式错误。");
        }

        var teacher =
            context.teachers()
                .findByUserId(
                    assignmentRequest
                        .getTeacherUserId())
                .orElse(null);

        /*
         * findByUserId 只返回有效教师，
         * 因此停用的教师不能再被分配。
         */
        if (teacher == null) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "未找到有效的教师档案。");
        }

        boolean assigned =
            teacherService.assignTeacher(
                assignmentRequest.getOfferingId(),
                teacher.userId(),
                teacher.displayName());

        if (!assigned) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "该教师已经负责这个教学班。");
        }

        return Response.success(
            request,
            "任课教师分配成功。",
            null);
    }

    /**
     * 教务移除教学班任课教师。
     */
    private Response adminRemoveTeacher(
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
                "没有任课教师管理权限。");
        }

        if (!(request.getData()
            instanceof AdminTeacherAssignmentRequest
            assignmentRequest)) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "任课教师移除请求格式错误。");
        }

        /*
         * 移除时不能要求教师仍处于有效状态，
         * 否则已停用教师的旧任课关系将无法删除。
         */
        boolean removed =
            teacherService.removeTeacher(
                assignmentRequest.getOfferingId(),
                assignmentRequest.getTeacherUserId());

        if (!removed) {

            return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                "未找到对应的任课关系。");
        }

        return Response.success(
            request,
            "任课教师移除成功。",
            null);
    }
    /**
     * 向其他服务器模块提供教师学生范围检查。
     */
    public TeacherStudentAccess teacherStudentAccess() {

        return teacherService::canViewStudent;
    }
}
