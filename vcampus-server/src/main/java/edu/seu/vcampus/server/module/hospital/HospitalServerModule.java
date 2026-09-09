package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.CreateScheduleRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.MarkAppointmentNoShowRequest;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.ReviewDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.AdminCancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.CreateDepartmentRequest;
import edu.seu.vcampus.common.hospital.UpdateDepartmentRequest;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SetSchedulePublicationRequest;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.UpdatePatientHealthProfileRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/** Server entry point owned by the hospital-appointment module. */
public final class HospitalServerModule implements ServerModule {

    private final HospitalService service;

    public HospitalServerModule() {
        this(createDefaultService());
    }

    /** Creates an Access-backed doctor registry plus the staged clinical repository. */
    public static HospitalServerModule createAccessBacked(Path databasePath) {
        Clock clock = Clock.systemDefaultZone();
        return new HospitalServerModule(new HospitalService(
                new AccessHospitalRepository(new AccessDatabase(databasePath), clock), clock));
    }

    HospitalServerModule(HospitalService service) {
        this.service = Objects.requireNonNull(service, "service must not be null");
    }

    @Override
    public String id() {
        return ModuleNames.HOSPITAL;
    }

    @Override
    public void registerHandlers(ActionRouter router, ServerContext context) {
        router.register(HospitalActions.GET_MODE_ACCESS,
                request -> getModeAccess(request, context));
        router.register(HospitalActions.LIST_DEPARTMENTS,
                request -> listDepartments(request, context));
        router.register(HospitalActions.GET_TRIAGE_RECOMMENDATION,
                request -> getTriageRecommendation(request, context));
        router.register(HospitalActions.SEARCH_SLOTS,
                request -> searchSlots(request, context));
        router.register(HospitalActions.BOOK_APPOINTMENT,
                request -> bookAppointment(request, context));
        router.register(HospitalActions.SEARCH_APPOINTMENTS,
                request -> searchAppointments(request, context));
        router.register(HospitalActions.CANCEL_APPOINTMENT,
                request -> cancelAppointment(request, context));
        router.register(HospitalActions.GET_DOCTOR_WORKSPACE,
                request -> getDoctorWorkspace(request, context));
        router.register(HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                request -> getDoctorConsultationContext(request, context));
        router.register(HospitalActions.MARK_APPOINTMENT_NO_SHOW,
                request -> markAppointmentNoShow(request, context));
        router.register(HospitalActions.SUBMIT_CONSULTATION,
                request -> submitConsultation(request, context));
        router.register(HospitalActions.LIST_MY_CONSULTATIONS,
                request -> listMyConsultations(request, context));
        router.register(HospitalActions.GET_MY_HEALTH_RECORD,
                request -> getMyHealthRecord(request, context));
        router.register(HospitalActions.UPDATE_MY_HEALTH_PROFILE,
                request -> updateMyHealthProfile(request, context));
        router.register(HospitalActions.SUBMIT_EXAMINATION_PLAN,
                request -> submitExaminationPlan(request, context));
        router.register(HospitalActions.PUBLISH_DEMO_EXAMINATION_REPORT,
                request -> publishDemoExaminationReport(request, context));
        router.register(HospitalActions.BOOK_RESULT_REVIEW,
                request -> bookResultReview(request, context));
        router.register(HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                request -> submitDoctorApplication(request, context));
        router.register(HospitalActions.LIST_DOCTOR_APPLICATIONS,
                request -> listDoctorApplications(request, context));
        router.register(HospitalActions.REVIEW_DOCTOR_APPLICATION,
                request -> reviewDoctorApplication(request, context));
        router.register(HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE,
                request -> getAdminScheduleWorkspace(request, context));
        router.register(HospitalActions.CREATE_SCHEDULE,
                request -> createSchedule(request, context));
        router.register(HospitalActions.SET_SCHEDULE_PUBLICATION,
                request -> setSchedulePublication(request, context));
        router.register(HospitalActions.GET_ADMIN_DEPARTMENT_WORKSPACE,
                request -> getAdminDepartmentWorkspace(request, context));
        router.register(HospitalActions.CREATE_DEPARTMENT,
                request -> createDepartment(request, context));
        router.register(HospitalActions.UPDATE_DEPARTMENT,
                request -> updateDepartment(request, context));
        router.register(HospitalActions.LIST_ADMIN_APPOINTMENTS,
                request -> listAdminAppointments(request, context));
        router.register(HospitalActions.ADMIN_CANCEL_APPOINTMENT,
                request -> adminCancelAppointment(request, context));
        router.register(HospitalActions.LIST_MY_BILLS,
                request -> listMyBills(request, context));
        router.register(HospitalActions.PAY_BILL,
                request -> payBill(request, context));
    }

    private Response getModeAccess(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "Mode-access data must be empty.");
        }
        return Response.success(
                request,
                "Hospital mode access loaded.",
                service.getModeAccess(session.get()));
    }

    private Response listDepartments(Request request, ServerContext context) {
        Response authenticationFailure = authenticationFailure(request, context);
        if (authenticationFailure != null) {
            return authenticationFailure;
        }
        if (request.getData() != null) {
            return invalidRequest(request, "Department-list data must be empty.");
        }
        return Response.success(request, "Departments loaded.", service.listDepartments());
    }

    private Response getTriageRecommendation(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof TriageRequest triageRequest)) {
            return invalidRequest(request, "Triage data is invalid.");
        }
        return Response.success(
                request,
                "Department guidance generated.",
                service.getTriageRecommendation(session.get(), triageRequest));
    }

    private Response searchSlots(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof SearchSlotsRequest searchRequest)) {
            return invalidRequest(request, "Slot-search data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Slots loaded.",
                    service.searchSlots(session.get(), searchRequest));
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response bookAppointment(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof BookAppointmentRequest bookingRequest)) {
            return invalidRequest(request, "Booking data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Appointment booked and registration payment completed.",
                    service.bookAppointment(session.get(), bookingRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.errorCode(),
                    exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response searchAppointments(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "Appointment-list data must be empty.");
        }
        return Response.success(
                request,
                "Appointments loaded.",
                service.listMyAppointments(session.get()));
    }

    private Response cancelAppointment(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof CancelAppointmentRequest cancelRequest)) {
            return invalidRequest(request, "Cancellation data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Appointment cancelled and registration payment refunded.",
                    service.cancelAppointment(session.get(), cancelRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.errorCode(),
                    exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response getDoctorWorkspace(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "Doctor-workspace data must be empty.");
        }
        try {
            return Response.success(
                    request,
                    "Doctor workspace loaded.",
                    service.getDoctorWorkspace(session.get()));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.errorCode(),
                    exception.getMessage());
        }
    }

    private Response getDoctorConsultationContext(
            Request request,
            ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof DoctorConsultationContextRequest contextRequest)) {
            return invalidRequest(request, "Consultation-context data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Consultation context loaded.",
                    service.getDoctorConsultationContext(session.get(), contextRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.errorCode(),
                    exception.getMessage());
        }
    }

    private Response markAppointmentNoShow(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof MarkAppointmentNoShowRequest noShowRequest)) {
            return invalidRequest(request, "No-show data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Appointment marked as not attended.",
                    service.markAppointmentNoShow(session.get(), noShowRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.errorCode(),
                    exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response submitConsultation(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof SubmitConsultationRequest submitRequest)) {
            return invalidRequest(request, "Consultation data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Consultation completed.",
                    service.submitConsultation(session.get(), submitRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(),
                    exception.errorCode(),
                    exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response listMyConsultations(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "Consultation-list data must be empty.");
        }
        return Response.success(
                request,
                "Consultation records loaded.",
                service.listMyConsultations(session.get()));
    }

    private Response getMyHealthRecord(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "Health-record data must be empty.");
        }
        return Response.success(
                request,
                "Patient health record loaded.",
                service.getMyHealthRecord(session.get()));
    }

    private Response updateMyHealthProfile(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof UpdatePatientHealthProfileRequest profileRequest)) {
            return invalidRequest(request, "Health-profile data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Patient-authored health profile updated.",
                    service.updateMyHealthProfile(session.get(), profileRequest));
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response submitExaminationPlan(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof SubmitExaminationPlanRequest planRequest)) {
            return invalidRequest(request, "Examination-plan data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Examination ordered; the episode is waiting for results.",
                    service.submitExaminationPlan(session.get(), planRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response publishDemoExaminationReport(
            Request request,
            ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData()
                instanceof PublishDemoExaminationReportRequest reportRequest)) {
            return invalidRequest(request, "Demo-report data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Demo examination report published.",
                    service.publishDemoExaminationReport(session.get(), reportRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private Response bookResultReview(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof BookResultReviewRequest reviewRequest)) {
            return invalidRequest(request, "Result-review data is invalid.");
        }
        try {
            return Response.success(
                    request,
                    "Zero-fee result-review visit booked.",
                    service.bookResultReview(session.get(), reviewRequest));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private Response submitDoctorApplication(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!session.get().canAdminister(ModuleNames.HOSPITAL)) {
            return forbidden(request, "只有医院管理员可以提交医生新增申请。");
        }
        if (!(request.getData() instanceof SubmitDoctorApplicationRequest data)) {
            return invalidRequest(request, "医生申请数据无效。");
        }
        try {
            return Response.success(
                    request,
                    "医生申请已提交，等待超级管理员审核。",
                    service.submitDoctorApplication(
                            data, session.get().getUserId(), context.accounts()));
        } catch (HospitalWorkflowException exception) {
            return workflowFailure(request, exception);
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response listDoctorApplications(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!session.get().canManageUsers()
                && !session.get().canAdminister(ModuleNames.HOSPITAL)) {
            return forbidden(request, "只有医院管理员或超级管理员可以查看医生申请。");
        }
        if (request.getData() != null) {
            return invalidRequest(request, "医生申请列表请求不应包含数据。");
        }
        return Response.success(
                request,
                "医生申请列表加载成功。",
                service.listDoctorApplications());
    }

    private Response reviewDoctorApplication(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!session.get().canManageUsers()) {
            return forbidden(request, "只有超级管理员可以审核医生申请。");
        }
        if (!(request.getData() instanceof ReviewDoctorApplicationRequest data)) {
            return invalidRequest(request, "医生申请审核数据无效。");
        }
        try {
            String message = data.isApproved()
                    ? "医生申请已通过，账号和医生档案已完成绑定。"
                    : "医生申请已拒绝。";
            return Response.success(
                    request,
                    message,
                    service.reviewDoctorApplication(
                            data, session.get().getUserId(), context.accounts()));
        } catch (HospitalWorkflowException exception) {
            return workflowFailure(request, exception);
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response getAdminScheduleWorkspace(
            Request request,
            ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "排班工作区请求不应包含数据。");
        }
        try {
            return Response.success(
                    request,
                    "排班工作区加载成功。",
                    service.getAdminScheduleWorkspace(session.get()));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private Response createSchedule(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof CreateScheduleRequest data)) {
            return invalidRequest(request, "新建排班数据无效。");
        }
        try {
            return Response.success(
                    request,
                    "未发布排班已建立。",
                    service.createSchedule(session.get(), data));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response setSchedulePublication(
            Request request,
            ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof SetSchedulePublicationRequest data)) {
            return invalidRequest(request, "排班发布状态数据无效。");
        }
        try {
            String message = data.isPublished() ? "排班已发布。" : "排班已关闭。";
            return Response.success(
                    request,
                    message,
                    service.setSchedulePublication(session.get(), data));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response getAdminDepartmentWorkspace(
            Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "科室工作区请求不应包含数据。");
        }
        try {
            return Response.success(
                    request,
                    "科室工作区加载成功。",
                    service.getAdminDepartmentWorkspace(session.get()));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private Response createDepartment(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof CreateDepartmentRequest data)) {
            return invalidRequest(request, "新建科室数据无效。");
        }
        try {
            return Response.success(
                    request,
                    "科室已建立。",
                    service.createDepartment(session.get(), data));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response updateDepartment(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof UpdateDepartmentRequest data)) {
            return invalidRequest(request, "科室更新数据无效。");
        }
        try {
            return Response.success(
                    request,
                    "科室资料已保存。",
                    service.updateDepartment(session.get(), data));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
        }
    }

    private Response listAdminAppointments(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "预约管理列表请求不应包含数据。");
        }
        try {
            return Response.success(
                    request,
                    "预约管理列表加载成功。",
                    service.listAdminAppointments(session.get()));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private Response adminCancelAppointment(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof AdminCancelAppointmentRequest data)) {
            return invalidRequest(request, "管理员取消预约数据无效。");
        }
        try {
            return Response.success(
                    request,
                    "异常预约已取消，挂号费已模拟退款。",
                    service.cancelAppointmentAsAdmin(session.get(), data));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private Response listMyBills(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (request.getData() != null) {
            return invalidRequest(request, "费用清单请求不应包含数据。");
        }
        return Response.success(
                request,
                "费用清单加载成功。",
                service.listMyBills(session.get()));
    }

    private Response payBill(Request request, ServerContext context) {
        Optional<SessionInfo> session = authenticatedSession(request, context);
        if (session.isEmpty()) {
            return authenticationRequired(request);
        }
        if (!(request.getData() instanceof PayHospitalBillRequest data)) {
            return invalidRequest(request, "缴费请求数据无效。");
        }
        try {
            return Response.success(
                    request,
                    "模拟缴费成功。",
                    service.payBill(session.get(), data));
        } catch (HospitalBusinessException exception) {
            return Response.failure(
                    request.getRequestId(), exception.errorCode(), exception.getMessage());
        }
    }

    private static Response authenticationFailure(Request request, ServerContext context) {
        if (authenticatedSession(request, context).isPresent()) {
            return null;
        }
        return authenticationRequired(request);
    }

    private static Optional<SessionInfo> authenticatedSession(
            Request request,
            ServerContext context) {
        return context.sessions().findSession(request.getToken());
    }

    private static Response authenticationRequired(Request request) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before using the hospital module.");
    }

    private static Response forbidden(Request request, String message) {
        return Response.failure(request.getRequestId(), ErrorCodes.AUTH_FORBIDDEN, message);
    }

    private static Response workflowFailure(
            Request request,
            HospitalWorkflowException exception) {
        return Response.failure(request.getRequestId(), exception.code(), exception.getMessage());
    }

    private static Response invalidRequest(Request request, String message) {
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.COMMON_INVALID_REQUEST,
                message);
    }

    private static HospitalService createDefaultService() {
        Clock clock = Clock.systemDefaultZone();
        return new HospitalService(new InMemoryHospitalRepository(clock), clock);
    }
}
