package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.ReviewDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;

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

    /** Creates the hospital module with persistent doctor onboarding data. */
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
        router.register(HospitalActions.SEARCH_SLOTS,
                request -> searchSlots(request, context));
        router.register(HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                request -> submitDoctorApplication(request, context));
        router.register(HospitalActions.LIST_DOCTOR_APPLICATIONS,
                request -> listDoctorApplications(request, context));
        router.register(HospitalActions.REVIEW_DOCTOR_APPLICATION,
                request -> reviewDoctorApplication(request, context));
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

    private Response searchSlots(Request request, ServerContext context) {
        Response authenticationFailure = authenticationFailure(request, context);
        if (authenticationFailure != null) {
            return authenticationFailure;
        }
        if (!(request.getData() instanceof SearchSlotsRequest searchRequest)) {
            return invalidRequest(request, "Slot-search data is invalid.");
        }
        try {
            return Response.success(request, "Slots loaded.", service.searchSlots(searchRequest));
        } catch (IllegalArgumentException exception) {
            return invalidRequest(request, exception.getMessage());
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
