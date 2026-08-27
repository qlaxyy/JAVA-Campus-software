package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.ActionRouter;
import edu.seu.vcampus.server.module.ServerModule;
import edu.seu.vcampus.server.module.ServerContext;

import java.time.Clock;
import java.util.Objects;

/** Server entry point owned by the hospital-appointment module. */
public final class HospitalServerModule implements ServerModule {

    private final HospitalService service;

    public HospitalServerModule() {
        this(createDefaultService());
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
        router.register(HospitalActions.LIST_DEPARTMENTS,
                request -> listDepartments(request, context));
        router.register(HospitalActions.SEARCH_SLOTS,
                request -> searchSlots(request, context));
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

    private static Response authenticationFailure(Request request, ServerContext context) {
        if (context.sessions().findSession(request.getToken()).isPresent()) {
            return null;
        }
        return Response.failure(
                request.getRequestId(),
                ErrorCodes.AUTH_REQUIRED,
                "Please log in before using the hospital module.");
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
