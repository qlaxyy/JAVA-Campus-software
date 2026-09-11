package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.AdminAppointmentListResponse;
import edu.seu.vcampus.common.hospital.AdminAppointmentView;
import edu.seu.vcampus.common.hospital.AdminCancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.AdminDepartmentView;
import edu.seu.vcampus.common.hospital.AdminDepartmentWorkspaceView;
import edu.seu.vcampus.common.hospital.AdminDoctorView;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.AppointmentListResponse;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.AppointmentView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.ConsultationListResponse;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.CreateScheduleRequest;
import edu.seu.vcampus.common.hospital.CreateDepartmentRequest;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextView;
import edu.seu.vcampus.common.hospital.DoctorClinicalRecordView;
import edu.seu.vcampus.common.hospital.DoctorAppointmentView;
import edu.seu.vcampus.common.hospital.DoctorFollowUpView;
import edu.seu.vcampus.common.hospital.DoctorScheduleView;
import edu.seu.vcampus.common.hospital.DoctorWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.MarkAppointmentNoShowRequest;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.hospital.PatientBillView;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SetSchedulePublicationRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.DoctorApplicationView;
import edu.seu.vcampus.common.hospital.ReviewDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.PatientHealthProfileView;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.TriageRequest;
import edu.seu.vcampus.common.hospital.TriageResultView;
import edu.seu.vcampus.common.hospital.UpdatePatientHealthProfileRequest;
import edu.seu.vcampus.common.hospital.UpdateDepartmentRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.AccountProvisioning;
import edu.seu.vcampus.server.security.ProvisionedAccount;
import edu.seu.vcampus.server.security.UserDirectory;
import edu.seu.vcampus.server.security.UserIdentity;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Hospital business rules independent from sockets and Swing. */
final class HospitalService {

    private static final int SEARCH_DAYS = 7;
    private static final long DEMO_TREATMENT_FEE_CENTS = 1_800;
    private static final long DEMO_EXAMINATION_FEE_CENTS = 3_000;

    private final HospitalRepository repository;
    private final Clock clock;
    private final HospitalTriageEngine triageEngine = new HospitalTriageEngine();
    private final ConcurrentMap<String, Object> scheduleLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> doctorScheduleLocks =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> episodeLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> billLocks = new ConcurrentHashMap<>();

    HospitalService(HospitalRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    HospitalModeAccessView getModeAccess(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        return new HospitalModeAccessView(
                true,
                repository.isActiveDoctorUser(session.getUserId()),
                session.canAdminister(ModuleNames.HOSPITAL));
    }

    DepartmentListResponse listDepartments() {
        return new DepartmentListResponse(repository.findActiveDepartments().stream()
                .sorted(Comparator.comparing(HospitalDepartment::departmentName))
                .map(department -> new DepartmentView(
                        department.departmentId(),
                        department.departmentName(),
                        department.parentDepartmentId(),
                        department.bookable()))
                .toList());
    }

    TriageResultView getTriageRecommendation(
            SessionInfo session,
            TriageRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        return triageEngine.triage(request, repository.findActiveDepartments());
    }

    AdminDepartmentWorkspaceView getAdminDepartmentWorkspace(SessionInfo session) {
        requireHospitalAdmin(session);
        List<HospitalDepartment> departments = repository.findAllDepartments();
        LocalDateTime now = LocalDateTime.now(clock);
        return new AdminDepartmentWorkspaceView(departments.stream()
                .sorted(Comparator.comparing(HospitalDepartment::active).reversed()
                        .thenComparing(HospitalDepartment::departmentName))
                .map(department -> new AdminDepartmentView(
                        department.departmentId(),
                        department.departmentName(),
                        department.parentDepartmentId(),
                        departments.stream()
                                .filter(parent -> parent.departmentId().equals(
                                        department.parentDepartmentId()))
                                .map(HospitalDepartment::departmentName)
                                .findFirst()
                                .orElse(null),
                        department.bookable(),
                        department.active(),
                        (int) repository.findAllDoctors().stream()
                                .filter(HospitalDoctor::active)
                                .filter(doctor -> doctor.departmentId().equals(
                                        department.departmentId()))
                                .count(),
                        (int) repository.findAllSlots().stream()
                                .filter(slot -> slot.departmentId().equals(
                                        department.departmentId()))
                                .filter(slot -> slot.endTime().isAfter(now))
                                .count()))
                .toList());
    }

    AdminDepartmentView createDepartment(
            SessionInfo session, CreateDepartmentRequest request) {
        requireHospitalAdmin(session);
        Objects.requireNonNull(request, "request must not be null");
        String name = requireDepartmentName(request.getDepartmentName());
        List<HospitalDepartment> departments = repository.findAllDepartments();
        validateDepartmentParent(
                departments, null, request.getParentDepartmentId(), true);
        ensureUniqueDepartmentName(
                departments, null, request.getParentDepartmentId(), name);
        HospitalDepartment department = new HospitalDepartment(
                UUID.randomUUID().toString(),
                name,
                request.getParentDepartmentId(),
                request.isBookable(),
                true);
        repository.insertDepartment(department);
        return adminDepartmentView(department, repository.findAllDepartments());
    }

    AdminDepartmentView updateDepartment(
            SessionInfo session, UpdateDepartmentRequest request) {
        requireHospitalAdmin(session);
        Objects.requireNonNull(request, "request must not be null");
        List<HospitalDepartment> departments = repository.findAllDepartments();
        HospitalDepartment current = departments.stream()
                .filter(department -> department.departmentId().equals(
                        request.getDepartmentId()))
                .findFirst()
                .orElseThrow(() -> businessFailure(
                        ErrorCodes.HOSPITAL_DEPARTMENT_NOT_FOUND,
                        "The selected department does not exist."));
        String name = requireDepartmentName(request.getDepartmentName());
        validateDepartmentParent(
                departments,
                current.departmentId(),
                request.getParentDepartmentId(),
                request.isActive());
        ensureUniqueDepartmentName(
                departments, current.departmentId(), request.getParentDepartmentId(), name);

        boolean hasActiveChildren = departments.stream()
                .filter(HospitalDepartment::active)
                .anyMatch(department -> current.departmentId().equals(
                        department.parentDepartmentId()));
        if (request.isBookable() && hasActiveChildren) {
            throw departmentConflict("A category with active children cannot be bookable.");
        }
        boolean hasActiveDoctors = repository.findAllDoctors().stream()
                .filter(HospitalDoctor::active)
                .anyMatch(doctor -> doctor.departmentId().equals(current.departmentId()));
        boolean hasPublishedFutureSchedules = repository.findAllSlots().stream()
                .filter(HospitalSlot::published)
                .filter(slot -> slot.endTime().isAfter(LocalDateTime.now(clock)))
                .anyMatch(slot -> slot.departmentId().equals(current.departmentId()));
        if (current.bookable() && !request.isBookable()
                && (hasActiveDoctors || hasPublishedFutureSchedules)) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_DEPARTMENT_IN_USE,
                    "Move active doctors and published schedules before changing this clinic to a category.");
        }
        if (!request.isActive()) {
            if (hasActiveChildren || hasActiveDoctors || hasPublishedFutureSchedules) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_DEPARTMENT_IN_USE,
                        "Close child departments, active doctors, and published schedules first.");
            }
        }
        HospitalDepartment updated = new HospitalDepartment(
                current.departmentId(), name, request.getParentDepartmentId(),
                request.isBookable(), request.isActive());
        repository.updateDepartment(updated);
        return adminDepartmentView(updated, repository.findAllDepartments());
    }

    AdminAppointmentListResponse listAdminAppointments(SessionInfo session) {
        requireHospitalAdmin(session);
        return new AdminAppointmentListResponse(repository.findAllSlots().stream()
                .flatMap(slot -> repository.findAppointmentsByScheduleId(
                                slot.scheduleId()).stream()
                        .map(appointment -> adminAppointmentView(
                                slot,
                                repository.findBookingById(appointment.appointmentId())
                                        .orElseThrow(() -> new IllegalStateException(
                                                "appointment booking does not exist")))))
                .sorted(Comparator.comparing(AdminAppointmentView::getStartTime).reversed()
                        .thenComparing(AdminAppointmentView::getQueueNumber))
                .toList());
    }

    AdminAppointmentView cancelAppointmentAsAdmin(
            SessionInfo session, AdminCancelAppointmentRequest request) {
        requireHospitalAdmin(session);
        Objects.requireNonNull(request, "request must not be null");
        HospitalBooking cancelled = cancelAppointmentInternal(
                request.getAppointmentId(), null);
        HospitalSlot slot = repository.findSlotById(cancelled.appointment().scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "appointment schedule does not exist"));
        return adminAppointmentView(slot, cancelled);
    }

    synchronized DoctorApplicationView submitDoctorApplication(
            SubmitDoctorApplicationRequest request,
            String requestedByUserId,
            UserDirectory users) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(users, "users must not be null");
        HospitalDepartment department = requireDepartment(request.getDepartmentId());
        if (!department.active() || !department.bookable()) {
            throw conflict("只能把医生分配到已启用、可挂号的具体科室。");
        }
        String username = null;
        String displayName = request.getDisplayName();
        String targetUserId = null;
        if (request.getApplicationType() == DoctorApplicationType.EXISTING_ACCOUNT) {
            UserIdentity account = users
                    .findByCampusCardNumber(request.getExistingUsername())
                    .orElseThrow(() -> conflict("找不到要关联的校园账号。"));
            if (!account.enabled()) {
                throw conflict("要关联的校园账号已被禁用。请先处理账号状态。");
            }
            if (repository.isActiveDoctorUser(account.userId())) {
                throw conflict("该校园账号已经绑定有效医生档案。");
            }
            boolean duplicateTarget = repository.findDoctorApplications().stream()
                    .anyMatch(application -> application.status()
                                    == DoctorApplicationStatus.PENDING
                            && account.userId().equals(application.targetUserId()));
            if (duplicateTarget) {
                throw conflict("该校园账号已有待审核的医生申请。");
            }
            username = account.campusCardNumber();
            displayName = account.displayName();
            targetUserId = account.userId();
        }

        DoctorApplication application = new DoctorApplication(
                "DAR-" + UUID.randomUUID(),
                request.getApplicationType(),
                username,
                displayName,
                request.getDepartmentId(),
                request.getDoctorTitle(),
                requestedByUserId,
                DoctorApplicationStatus.PENDING,
                targetUserId,
                null,
                LocalDateTime.now(clock));
        repository.saveDoctorApplication(application);
        return toView(application, department.departmentName());
    }

    DoctorApplicationListResponse listDoctorApplications() {
        return new DoctorApplicationListResponse(repository.findDoctorApplications().stream()
                .sorted(Comparator.comparing(DoctorApplication::createdAt).reversed())
                .map(this::toView)
                .toList());
    }

    synchronized DoctorApplicationView reviewDoctorApplication(
            ReviewDoctorApplicationRequest request,
            String reviewerUserId,
            UserDirectory users,
            AccountProvisioning accounts) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(users, "users must not be null");
        Objects.requireNonNull(accounts, "accounts must not be null");
        DoctorApplication application = repository
                .findDoctorApplication(request.getRequestId())
                .orElseThrow(() -> new HospitalWorkflowException(
                        ErrorCodes.HOSPITAL_DOCTOR_APPLICATION_NOT_FOUND,
                        "医生申请不存在。"));
        if (application.status() != DoctorApplicationStatus.PENDING) {
            throw conflict("该申请已经审核，不能重复处理。");
        }
        if (!request.isApproved()) {
            DoctorApplication rejected = application.reviewed(
                    DoctorApplicationStatus.REJECTED,
                    application.username(), application.targetUserId(), reviewerUserId);
            repository.saveDoctorApplication(rejected);
            return toView(rejected);
        }

        String accountUserId;
        String accountUsername;
        if (application.applicationType() == DoctorApplicationType.EXISTING_ACCOUNT) {
            UserIdentity account = users.findByCampusCardNumber(application.username())
                    .filter(found -> found.userId().equals(application.targetUserId()))
                    .orElseThrow(() -> conflict("原有校园账号已经不存在或发生变化。"));
            if (!account.enabled()) {
                throw conflict("要关联的校园账号已被禁用。请先处理账号状态。");
            }
            accountUserId = account.userId();
            accountUsername = account.campusCardNumber();
        } else {
            ProvisionedAccount account = accounts
                    .createGeneratedRegularAccount(application.displayName());
            accountUserId = account.userId();
            accountUsername = account.username();
        }
        repository.saveDoctorProfile(new DoctorProfile(
                accountUserId, application.departmentId(),
                application.displayName(),
                application.doctorTitle(), true));
        DoctorApplication approved = application.reviewed(
                DoctorApplicationStatus.APPROVED,
                accountUsername,
                accountUserId,
                reviewerUserId);
        repository.saveDoctorApplication(approved);
        return toView(approved);
    }

    AdminScheduleWorkspaceView getAdminScheduleWorkspace(SessionInfo session) {
        requireHospitalAdmin(session);
        List<HospitalDepartment> departments = repository.findActiveDepartments();
        List<AdminDoctorView> doctors = repository.findActiveDoctors().stream()
                .sorted(Comparator.comparing(HospitalDoctor::doctorName)
                        .thenComparing(HospitalDoctor::doctorId))
                .map(doctor -> new AdminDoctorView(
                        doctor.doctorId(),
                        doctor.doctorName(),
                        doctor.doctorTitle(),
                        doctor.departmentId(),
                        departments.stream()
                                .filter(department -> department.departmentId()
                                        .equals(doctor.departmentId()))
                                .map(HospitalDepartment::departmentName)
                                .findFirst()
                                .orElse(doctor.departmentId())))
                .toList();
        LocalDateTime now = LocalDateTime.now(clock);
        List<SlotView> schedules = repository.findAllSlots().stream()
                .filter(slot -> slot.endTime().isAfter(now))
                .sorted(Comparator.comparing(HospitalSlot::startTime)
                        .thenComparing(HospitalSlot::doctorName))
                .map(slot -> toView(slot, null))
                .toList();
        return new AdminScheduleWorkspaceView(doctors, schedules);
    }

    SlotView createSchedule(SessionInfo session, CreateScheduleRequest request) {
        requireHospitalAdmin(session);
        Objects.requireNonNull(request, "request must not be null");
        if (!request.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_SCHEDULE_STARTED,
                    "A new schedule must start in the future.");
        }
        HospitalDepartment department = requireDepartment(request.getDepartmentId());
        if (!department.bookable()) {
            throw new IllegalArgumentException("a bookable department is required");
        }
        HospitalDoctor doctor = repository.findActiveDoctors().stream()
                .filter(candidate -> candidate.doctorId().equals(request.getDoctorId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("doctorId does not exist"));
        if (!doctor.departmentId().equals(department.departmentId())) {
            throw new IllegalArgumentException(
                    "the doctor does not belong to the selected department");
        }

        Object doctorLock = doctorScheduleLocks.computeIfAbsent(
                doctor.doctorId(), ignored -> new Object());
        synchronized (doctorLock) {
            if (hasPublishedDoctorConflict(
                    doctor.doctorId(), null,
                    request.getStartTime(), request.getEndTime())) {
                throw scheduleConflict();
            }
            HospitalSlot slot = new HospitalSlot(
                    UUID.randomUUID().toString(),
                    department.departmentId(),
                    department.departmentName(),
                    doctor.doctorId(),
                    doctor.doctorName(),
                    doctor.doctorTitle(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getRegistrationFeeCents(),
                    request.getCapacity(),
                    0,
                    false);
            repository.insertSlot(slot);
            return toView(slot, null);
        }
    }

    SlotView setSchedulePublication(
            SessionInfo session,
            SetSchedulePublicationRequest request) {
        requireHospitalAdmin(session);
        Objects.requireNonNull(request, "request must not be null");
        HospitalSlot initial = repository.findSlotById(request.getScheduleId())
                .orElseThrow(() -> businessFailure(
                        ErrorCodes.HOSPITAL_SCHEDULE_NOT_FOUND,
                        "The selected schedule does not exist."));
        Object doctorLock = doctorScheduleLocks.computeIfAbsent(
                initial.doctorId(), ignored -> new Object());
        synchronized (doctorLock) {
            Object scheduleLock = scheduleLocks.computeIfAbsent(
                    request.getScheduleId(), ignored -> new Object());
            synchronized (scheduleLock) {
                HospitalSlot slot = repository.findSlotById(request.getScheduleId())
                        .orElseThrow(() -> businessFailure(
                                ErrorCodes.HOSPITAL_SCHEDULE_NOT_FOUND,
                                "The selected schedule does not exist."));
                if (!LocalDateTime.now(clock).isBefore(slot.startTime())) {
                    throw businessFailure(
                            ErrorCodes.HOSPITAL_SCHEDULE_STARTED,
                            "A schedule cannot be changed after it starts.");
                }
                if (slot.published() == request.isPublished()) {
                    return toView(slot, null);
                }
                if (request.isPublished()
                        && hasPublishedDoctorConflict(
                                slot.doctorId(), slot.scheduleId(),
                                slot.startTime(), slot.endTime())) {
                    throw scheduleConflict();
                }
                if (!request.isPublished()
                        && repository.findAppointmentsByScheduleId(slot.scheduleId()).stream()
                                .anyMatch(HospitalAppointment::occupiesSlot)) {
                    throw businessFailure(
                            ErrorCodes.HOSPITAL_SCHEDULE_HAS_APPOINTMENTS,
                            "A schedule with active or completed appointments cannot be closed.");
                }
                HospitalSlot updated = new HospitalSlot(
                        slot.scheduleId(),
                        slot.departmentId(),
                        slot.departmentName(),
                        slot.doctorId(),
                        slot.doctorName(),
                        slot.doctorTitle(),
                        slot.startTime(),
                        slot.endTime(),
                        slot.priceCents(),
                        slot.capacity(),
                        slot.bookedCount(),
                        request.isPublished());
                repository.updateSlot(updated);
                return toView(updated, null);
            }
        }
    }

    SlotListResponse searchSlots(SearchSlotsRequest request) {
        return searchSlots(null, request);
    }

    SlotListResponse searchSlots(SessionInfo session, SearchSlotsRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String departmentId;
        String preferredDoctorId = null;
        String sourceScheduleId = null;
        if (request.getVisitType() == VisitType.FIRST_VISIT) {
            departmentId = request.getDepartmentId();
        } else if (request.getVisitType() == VisitType.FOLLOW_UP) {
            FollowUpSource source = requireFollowUpSourceByConsultation(
                    session, request.getSourceConsultationId());
            departmentId = source.slot().departmentId();
            preferredDoctorId = source.consultation().doctorId();
            sourceScheduleId = source.slot().scheduleId();
        } else {
            throw new IllegalArgumentException(
                    "Result-review visits use their dedicated workflow.");
        }
        if (departmentId == null || departmentId.isBlank()) {
            throw new IllegalArgumentException("departmentId is required");
        }
        HospitalDepartment department = repository.findActiveDepartments().stream()
                .filter(candidate -> candidate.departmentId()
                        .equals(departmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "departmentId does not exist"));
        if (!department.bookable()) {
            throw new IllegalArgumentException(
                    "a bookable subdepartment must be selected");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = today.plusDays(SEARCH_DAYS - 1L);
        LocalDateTime now = LocalDateTime.now(clock);
        String finalPreferredDoctorId = preferredDoctorId;
        String finalSourceScheduleId = sourceScheduleId;
        return new SlotListResponse(repository.findSlots(today, endDate).stream()
                .filter(HospitalSlot::published)
                .filter(slot -> !slot.startTime().isBefore(now))
                .filter(slot -> slot.departmentId().equals(departmentId))
                .filter(slot -> finalSourceScheduleId == null
                        || !slot.scheduleId().equals(finalSourceScheduleId))
                .filter(slot -> request.getDoctorId() == null
                        || slot.doctorId().equals(request.getDoctorId()))
                .sorted(finalPreferredDoctorId == null
                        ? Comparator.comparing(HospitalSlot::startTime)
                                .thenComparing(HospitalSlot::doctorName)
                        : Comparator.comparing((HospitalSlot slot) -> !slot.doctorId()
                                        .equals(finalPreferredDoctorId))
                                .thenComparing(slot -> !hasRemainingCapacity(slot))
                                .thenComparing(HospitalSlot::startTime)
                                .thenComparing(HospitalSlot::doctorName))
                .map(slot -> toView(slot, session == null ? null : session.getUserId()))
                .toList());
    }

    AppointmentBookingView bookAppointment(
            SessionInfo session,
            BookAppointmentRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        validateBookingRequest(request);

        String scheduleId = request.getScheduleId();
        Object scheduleLock = scheduleLocks.computeIfAbsent(
                scheduleId, ignored -> new Object());
        synchronized (scheduleLock) {
            HospitalSlot slot = repository.findSlotById(scheduleId)
                    .orElseThrow(() -> businessFailure(
                            ErrorCodes.HOSPITAL_SCHEDULE_NOT_FOUND,
                            "The selected schedule does not exist."));
            validateBookableSlot(slot);
            rejectSelfBooking(session, slot);

            List<HospitalAppointment> appointments =
                    repository.findAppointmentsByScheduleId(scheduleId);
            boolean duplicate = appointments.stream()
                    .filter(HospitalAppointment::occupiesSlot)
                    .anyMatch(appointment -> appointment.patientUserId()
                            .equals(session.getUserId()));
            if (duplicate) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_DUPLICATE_APPOINTMENT,
                        "You have already booked this schedule.");
            }

            if (request.getVisitType() == VisitType.RESULT_REVIEW) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID,
                        "Result-review visits use their dedicated workflow.");
            }
            if (request.getVisitType() == VisitType.FOLLOW_UP) {
                FollowUpSource source = requireFollowUpSourceByAppointment(
                        session, request.getSourceFirstVisitAppointmentId());
                if (!slot.departmentId().equals(source.slot().departmentId())) {
                    throw businessFailure(
                            ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID,
                            "A follow-up schedule must belong to the source department.");
                }
            }

            int queueNumber = allocateQueueNumber(slot, appointments);
            LocalDateTime now = LocalDateTime.now(clock);
            String appointmentId = "appointment-" + UUID.randomUUID();
            String episodeId = "episode-" + UUID.randomUUID();
            String billId = "bill-" + UUID.randomUUID();
            HospitalAppointment appointment = new HospitalAppointment(
                    appointmentId,
                    session.getUserId(),
                    scheduleId,
                    queueNumber,
                    now,
                    null,
                    null,
                    AppointmentStatus.BOOKED,
                    request.getVisitType(),
                    episodeId,
                    request.getSourceFirstVisitAppointmentId());
            HospitalEpisode episode = new HospitalEpisode(
                    episodeId,
                    session.getUserId(),
                    slot.departmentId(),
                    EpisodeStatus.IN_PROGRESS,
                    now,
                    null);
            HospitalBill bill = new HospitalBill(
                    billId,
                    appointmentId,
                    now,
                    now,
                    null,
                    PaymentStatus.PAID);
            HospitalBillItem billItem = new HospitalBillItem(
                    "bill-item-" + UUID.randomUUID(),
                    billId,
                    "挂号费",
                    1,
                    slot.priceCents());
            repository.saveBookingAndEpisode(
                    new HospitalBooking(appointment, bill, billItem), episode);

            return new AppointmentBookingView(
                    appointmentId,
                    queueNumber,
                    AppointmentStatus.BOOKED,
                    billId,
                    PaymentStatus.PAID,
                    billItem.amountCents(),
                    slot.doctorName(),
                    slot.departmentName(),
                    slot.startTime(),
                    slot.endTime(),
                    now,
                    now);
        }
    }

    AppointmentView cancelAppointment(
            SessionInfo session,
            CancelAppointmentRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");

        return toAppointmentView(cancelAppointmentInternal(
                request.getAppointmentId(), session.getUserId()));
    }

    private HospitalBooking cancelAppointmentInternal(
            String appointmentId, String expectedPatientUserId) {
        if (appointmentId == null || appointmentId.isBlank()) {
            throw appointmentNotFound();
        }

        HospitalBooking initial = repository
                .findBookingById(appointmentId)
                .orElseThrow(HospitalService::appointmentNotFound);
        String scheduleId = initial.appointment().scheduleId();
        Object scheduleLock = scheduleLocks.computeIfAbsent(
                scheduleId, ignored -> new Object());
        synchronized (scheduleLock) {
            HospitalBooking current = repository
                    .findBookingById(appointmentId)
                    .orElseThrow(HospitalService::appointmentNotFound);
            HospitalAppointment appointment = current.appointment();
            if (expectedPatientUserId != null
                    && !appointment.patientUserId().equals(expectedPatientUserId)) {
                throw appointmentNotFound();
            }
            if (appointment.status() != AppointmentStatus.BOOKED) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CANCELLABLE,
                        "Only a booked appointment can be cancelled.");
            }
            HospitalSlot slot = repository.findSlotById(scheduleId)
                    .orElseThrow(() -> new IllegalStateException(
                            "appointment schedule does not exist: " + scheduleId));
            LocalDateTime now = LocalDateTime.now(clock);
            if (!now.isBefore(slot.startTime())) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_APPOINTMENT_STARTED,
                        "An appointment cannot be cancelled after it starts.");
            }

            HospitalAppointment cancelledAppointment = new HospitalAppointment(
                    appointment.appointmentId(),
                    appointment.patientUserId(),
                    appointment.scheduleId(),
                    appointment.queueNumber(),
                    appointment.createdAt(),
                    now,
                    null,
                    AppointmentStatus.CANCELLED,
                    appointment.visitType(),
                    appointment.episodeId(),
                    appointment.sourceFirstVisitAppointmentId());
            HospitalBill refundedBill = new HospitalBill(
                    current.bill().billId(),
                    current.bill().appointmentId(),
                    current.bill().createdAt(),
                    current.bill().paidAt(),
                    now,
                    PaymentStatus.REFUNDED);
            HospitalBooking cancelled = new HospitalBooking(
                    cancelledAppointment,
                    refundedBill,
                    current.billItem());
            HospitalEpisode currentEpisode = repository
                    .findEpisodeById(appointment.episodeId())
                    .orElseThrow(() -> new IllegalStateException(
                            "appointment episode does not exist: "
                                    + appointment.episodeId()));
            HospitalEpisode episodeAfterCancellation = appointment.visitType()
                    == VisitType.RESULT_REVIEW
                    ? currentEpisode
                    : new HospitalEpisode(
                            currentEpisode.episodeId(),
                            currentEpisode.patientUserId(),
                            currentEpisode.departmentId(),
                            EpisodeStatus.CANCELLED,
                            currentEpisode.openedAt(),
                            null);
            repository.updateBookingAndEpisode(cancelled, episodeAfterCancellation);
            return cancelled;
        }
    }

    AppointmentListResponse listMyAppointments(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        List<AppointmentView> appointments = repository
                .findBookingsByPatientUserId(session.getUserId()).stream()
                .map(this::toAppointmentView)
                .sorted(Comparator.comparing(AppointmentView::getStartTime))
                .toList();
        return new AppointmentListResponse(appointments);
    }

    PatientBillListResponse listMyBills(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        List<PatientBillView> bills = repository
                .findBillsByPatientUserId(session.getUserId()).stream()
                .sorted(Comparator
                        .comparingInt((HospitalPatientBill bill) ->
                                bill.paymentStatus() == PaymentStatus.UNPAID ? 0 : 1)
                        .thenComparing(HospitalPatientBill::createdAt,
                                Comparator.reverseOrder()))
                .map(this::toPatientBillView)
                .toList();
        return new PatientBillListResponse(bills);
    }

    PatientBillView payBill(SessionInfo session, PayHospitalBillRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Object billLock = billLocks.computeIfAbsent(
                request.getBillId(), ignored -> new Object());
        synchronized (billLock) {
            HospitalPatientBill current = repository.findBillById(request.getBillId())
                    .filter(bill -> bill.patientUserId().equals(session.getUserId()))
                    .orElseThrow(HospitalService::billNotFound);
            if (current.paymentStatus() != PaymentStatus.UNPAID) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_BILL_NOT_PAYABLE,
                        "Only an unpaid bill can be paid.");
            }
            HospitalPatientBill paid = new HospitalPatientBill(
                    current.billId(), current.appointmentId(), current.patientUserId(),
                    current.billType(), PaymentStatus.PAID, current.createdAt(),
                    LocalDateTime.now(clock), null, current.item());
            repository.updatePatientBill(paid);
            return toPatientBillView(paid);
        }
    }

    DoctorWorkspaceView getDoctorWorkspace(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        HospitalDoctor doctor = requireDoctor(session);
        HospitalDepartment department = repository.findActiveDepartments().stream()
                .filter(candidate -> candidate.departmentId().equals(doctor.departmentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "doctor department does not exist: " + doctor.departmentId()));
        LocalDateTime now = LocalDateTime.now(clock);
        List<DoctorScheduleView> schedules = repository
                .findSlotsByDoctorId(doctor.doctorId()).stream()
                .filter(HospitalSlot::published)
                .sorted(Comparator.comparing(HospitalSlot::startTime)
                        .thenComparing(HospitalSlot::scheduleId))
                .map(this::toDoctorScheduleView)
                .filter(schedule -> schedule.getEndTime().isAfter(now)
                        || !schedule.getPendingAppointments().isEmpty())
                .toList();
        List<DoctorFollowUpView> followUps = repository
                .findExaminationOrdersByDoctorId(doctor.doctorId()).stream()
                .filter(order -> order.status() == ExaminationStatus.ORDERED
                        || order.status() == ExaminationStatus.RESULT_READY)
                .sorted(Comparator.comparing(
                        HospitalExaminationOrder::orderedAt).reversed())
                .map(this::toDoctorFollowUpView)
                .toList();
        List<DoctorClinicalRecordView> signedRecords = repository
                .findConsultationsByDoctorId(doctor.doctorId()).stream()
                .sorted(Comparator.comparing(
                        HospitalConsultation::createdAt).reversed())
                .limit(50)
                .map(this::toDoctorClinicalRecordView)
                .toList();
        return new DoctorWorkspaceView(
                doctor.doctorId(),
                doctor.doctorName(),
                doctor.doctorTitle(),
                doctor.departmentId(),
                department.departmentName(),
                schedules,
                followUps,
                signedRecords);
    }

    DoctorConsultationContextView getDoctorConsultationContext(
            SessionInfo session,
            DoctorConsultationContextRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        HospitalDoctor doctor = requireDoctor(session);
        HospitalBooking booking = requireAssignedBooking(
                doctor, request.getAppointmentId());
        HospitalAppointment appointment = booking.appointment();
        if (appointment.status() != AppointmentStatus.BOOKED) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CONSULTABLE,
                    "Only a pending appointment can be opened for consultation.");
        }
        HospitalSlot slot = repository.findSlotById(appointment.scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "appointment schedule does not exist: "
                                + appointment.scheduleId()));
        HospitalPatientProfile profile = repository
                .findPatientProfile(appointment.patientUserId())
                .orElseGet(() -> emptyProfile(appointment.patientUserId()));
        List<HospitalConsultation> previousConsultations = repository
                .findConsultationsByPatientUserId(appointment.patientUserId()).stream()
                .sorted(Comparator.comparing(HospitalConsultation::createdAt).reversed())
                .toList();
        List<ConsultationRecordView> previous = previousConsultations.stream()
                .map(this::toConsultationView)
                .toList();
        return new DoctorConsultationContextView(
                toDoctorAppointmentView(appointment),
                slot.departmentName(),
                slot.doctorName(),
                slot.doctorTitle(),
                slot.startTime(),
                slot.endTime(),
                toProfileView(profile),
                previous,
                repository.findExaminationOrdersByEpisodeId(
                                appointment.episodeId()).stream()
                        .sorted(Comparator.comparing(
                                HospitalExaminationOrder::orderedAt).reversed())
                        .map(this::toExaminationView)
                        .toList(),
                previousConsultations.stream()
                        .map(this::toDoctorClinicalRecordView)
                        .toList());
    }

    DoctorAppointmentView markAppointmentNoShow(
            SessionInfo session,
            MarkAppointmentNoShowRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        HospitalDoctor initialDoctor = requireDoctor(session);
        HospitalBooking initial = requireAssignedBooking(
                initialDoctor, request.getAppointmentId());
        String scheduleId = initial.appointment().scheduleId();
        Object scheduleLock = scheduleLocks.computeIfAbsent(
                scheduleId, ignored -> new Object());
        synchronized (scheduleLock) {
            HospitalDoctor doctor = requireDoctor(session);
            HospitalBooking booking = requireAssignedBooking(
                    doctor, request.getAppointmentId());
            HospitalAppointment appointment = booking.appointment();
            if (appointment.status() != AppointmentStatus.BOOKED
                    || repository.findConsultationByAppointmentId(
                            appointment.appointmentId()).isPresent()) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_APPOINTMENT_NOT_NO_SHOW,
                        "Only a pending appointment without a consultation can be marked no-show.");
            }
            HospitalSlot slot = repository.findSlotById(scheduleId)
                    .orElseThrow(() -> new IllegalStateException(
                            "appointment schedule does not exist: " + scheduleId));
            LocalDateTime now = LocalDateTime.now(clock);
            if (now.isBefore(slot.endTime())) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_APPOINTMENT_NOT_NO_SHOW,
                        "An appointment can be marked no-show only after its schedule ends.");
            }
            HospitalAppointment noShowAppointment = new HospitalAppointment(
                    appointment.appointmentId(),
                    appointment.patientUserId(),
                    appointment.scheduleId(),
                    appointment.queueNumber(),
                    appointment.createdAt(),
                    null,
                    null,
                    AppointmentStatus.NO_SHOW,
                    appointment.visitType(),
                    appointment.episodeId(),
                    appointment.sourceFirstVisitAppointmentId());
            HospitalBooking noShowBooking = new HospitalBooking(
                    noShowAppointment,
                    booking.bill(),
                    booking.billItem());
            HospitalEpisode currentEpisode = repository
                    .findEpisodeById(appointment.episodeId())
                    .orElseThrow(() -> new IllegalStateException(
                            "appointment episode does not exist: "
                                    + appointment.episodeId()));
            HospitalEpisode episodeAfterNoShow = appointment.visitType()
                    == VisitType.RESULT_REVIEW
                    ? currentEpisode
                    : new HospitalEpisode(
                            currentEpisode.episodeId(),
                            currentEpisode.patientUserId(),
                            currentEpisode.departmentId(),
                            EpisodeStatus.CANCELLED,
                            currentEpisode.openedAt(),
                            null);
            repository.updateBookingAndEpisode(noShowBooking, episodeAfterNoShow);
            return toDoctorAppointmentView(noShowAppointment);
        }
    }

    ConsultationRecordView submitConsultation(
            SessionInfo session,
            SubmitConsultationRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        validateConsultationRequest(request);
        HospitalDoctor initialDoctor = requireDoctor(session);
        HospitalBooking initial = requireAssignedBooking(
                initialDoctor, request.getAppointmentId());
        String scheduleId = initial.appointment().scheduleId();
        Object scheduleLock = scheduleLocks.computeIfAbsent(
                scheduleId, ignored -> new Object());
        synchronized (scheduleLock) {
            HospitalDoctor doctor = requireDoctor(session);
            HospitalBooking booking = requireAssignedBooking(
                    doctor, request.getAppointmentId());
            HospitalAppointment appointment = booking.appointment();
            if (appointment.status() != AppointmentStatus.BOOKED) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CONSULTABLE,
                        "Only a pending appointment can be completed.");
            }
            if (repository.findConsultationByAppointmentId(
                    appointment.appointmentId()).isPresent()) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_CONSULTATION_ALREADY_EXISTS,
                        "A consultation already exists for this appointment.");
            }
            LocalDateTime now = LocalDateTime.now(clock);
            HospitalConsultation consultation = new HospitalConsultation(
                    "consultation-" + UUID.randomUUID(),
                    appointment.appointmentId(),
                    doctor.doctorId(),
                    appointment.patientUserId(),
                    ConsultationOutcome.COMPLETED,
                    request.getDiagnosisOpinion(),
                    request.getExaminationAdvice(),
                    request.getTreatmentAdvice(),
                    request.getMedicationAdvice(),
                    request.getFollowUpAdvice(),
                    now);
            HospitalAppointment completedAppointment = new HospitalAppointment(
                    appointment.appointmentId(),
                    appointment.patientUserId(),
                    appointment.scheduleId(),
                    appointment.queueNumber(),
                    appointment.createdAt(),
                    null,
                    now,
                    AppointmentStatus.COMPLETED,
                    appointment.visitType(),
                    appointment.episodeId(),
                    appointment.sourceFirstVisitAppointmentId());
            HospitalBooking completedBooking = new HospitalBooking(
                    completedAppointment,
                    booking.bill(),
                    booking.billItem());
            HospitalEpisode episode = repository.findEpisodeById(appointment.episodeId())
                    .orElseThrow(() -> new IllegalStateException(
                            "appointment episode does not exist: "
                                    + appointment.episodeId()));
            HospitalEpisode completedEpisode = new HospitalEpisode(
                    episode.episodeId(),
                    episode.patientUserId(),
                    episode.departmentId(),
                    EpisodeStatus.COMPLETED,
                    episode.openedAt(),
                    now);
            if (appointment.visitType() == VisitType.RESULT_REVIEW) {
                HospitalExaminationOrder order = repository
                        .findExaminationOrdersByEpisodeId(episode.episodeId()).stream()
                        .filter(candidate -> candidate.status()
                                == ExaminationStatus.RESULT_READY)
                        .findFirst()
                        .orElseThrow(() -> businessFailure(
                                ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                                "No reported examination is waiting for review."));
                HospitalExaminationOrder reviewedOrder = new HospitalExaminationOrder(
                        order.orderId(),
                        order.episodeId(),
                        order.orderedAppointmentId(),
                        order.doctorId(),
                        order.patientUserId(),
                        order.itemName(),
                        order.instructions(),
                        ExaminationStatus.REVIEWED,
                        order.orderedAt(),
                        now);
                repository.saveResultReviewConsultation(
                        consultation, completedBooking, completedEpisode, reviewedOrder);
            } else {
                HospitalPatientBill treatmentBill = unpaidClinicalBill(
                        appointment,
                        HospitalBillType.TREATMENT,
                        "诊疗处置服务（课程演示）",
                        DEMO_TREATMENT_FEE_CENTS,
                        now);
                repository.saveConsultationAndUpdateBooking(
                        consultation, completedBooking, completedEpisode, treatmentBill);
            }
            return toConsultationView(consultation);
        }
    }

    ExaminationOrderView submitExaminationPlan(
            SessionInfo session,
            SubmitExaminationPlanRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        requireConsultationText(
                request.getPreliminaryDiagnosis(), "preliminaryDiagnosis", true);
        requireConsultationText(
                request.getReportInterpretation(), "reportInterpretation", false);
        requireConsultationText(request.getExaminationItem(), "examinationItem", true);
        requireConsultationText(
                request.getExaminationInstructions(), "examinationInstructions", false);
        requireConsultationText(request.getInterimCareAdvice(), "interimCareAdvice", false);
        HospitalDoctor initialDoctor = requireDoctor(session);
        HospitalBooking initial = requireAssignedBooking(
                initialDoctor, request.getAppointmentId());
        String scheduleId = initial.appointment().scheduleId();
        Object scheduleLock = scheduleLocks.computeIfAbsent(
                scheduleId, ignored -> new Object());
        synchronized (scheduleLock) {
            HospitalDoctor doctor = requireDoctor(session);
            HospitalBooking booking = requireAssignedBooking(
                    doctor, request.getAppointmentId());
            HospitalAppointment appointment = booking.appointment();
            if (appointment.status() != AppointmentStatus.BOOKED) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CONSULTABLE,
                        "Only a pending appointment can open an examination.");
            }
            if (repository.findConsultationByAppointmentId(
                    appointment.appointmentId()).isPresent()) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_CONSULTATION_ALREADY_EXISTS,
                        "A consultation already exists for this appointment.");
            }
            HospitalEpisode episode = repository.findEpisodeById(appointment.episodeId())
                    .orElseThrow(() -> new IllegalStateException(
                            "appointment episode does not exist: "
                                    + appointment.episodeId()));
            boolean resultReview = appointment.visitType() == VisitType.RESULT_REVIEW;
            EpisodeStatus expectedEpisodeStatus = resultReview
                    ? EpisodeStatus.RESULT_READY : EpisodeStatus.IN_PROGRESS;
            if (episode.status() != expectedEpisodeStatus) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                        "The clinical episode cannot open an examination now.");
            }
            LocalDateTime now = LocalDateTime.now(clock);
            HospitalExaminationOrder reviewedOrder = null;
            if (resultReview) {
                HospitalExaminationOrder previousOrder = repository
                        .findExaminationOrdersByEpisodeId(episode.episodeId()).stream()
                        .filter(candidate -> candidate.status()
                                == ExaminationStatus.RESULT_READY)
                        .findFirst()
                        .orElseThrow(() -> businessFailure(
                                ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                                "No reported examination is waiting for review."));
                reviewedOrder = new HospitalExaminationOrder(
                        previousOrder.orderId(),
                        previousOrder.episodeId(),
                        previousOrder.orderedAppointmentId(),
                        previousOrder.doctorId(),
                        previousOrder.patientUserId(),
                        previousOrder.itemName(),
                        previousOrder.instructions(),
                        ExaminationStatus.REVIEWED,
                        previousOrder.orderedAt(),
                        now);
            }
            HospitalConsultation consultation = new HospitalConsultation(
                    "consultation-" + UUID.randomUUID(),
                    appointment.appointmentId(),
                    doctor.doctorId(),
                    appointment.patientUserId(),
                    ConsultationOutcome.WAITING_FOR_RESULTS,
                    request.getPreliminaryDiagnosis(),
                    resultReview
                            ? request.getReportInterpretation()
                            : request.getExaminationItem()
                                    + (request.getExaminationInstructions().isBlank()
                                            ? ""
                                            : "：" + request.getExaminationInstructions()),
                    request.getInterimCareAdvice(),
                    "",
                    "检查结果出具后回诊。",
                    now);
            HospitalAppointment completedAppointment = completedAppointment(
                    appointment, now);
            HospitalEpisode waitingEpisode = new HospitalEpisode(
                    episode.episodeId(),
                    episode.patientUserId(),
                    episode.departmentId(),
                    EpisodeStatus.WAITING_FOR_RESULTS,
                    episode.openedAt(),
                    null);
            HospitalExaminationOrder order = new HospitalExaminationOrder(
                    "exam-order-" + UUID.randomUUID(),
                    episode.episodeId(),
                    appointment.appointmentId(),
                    doctor.doctorId(),
                    appointment.patientUserId(),
                    request.getExaminationItem(),
                    request.getExaminationInstructions(),
                    ExaminationStatus.ORDERED,
                    now,
                    null);
            HospitalBooking completedBooking = new HospitalBooking(
                    completedAppointment, booking.bill(), booking.billItem());
            HospitalPatientBill examinationBill = unpaidClinicalBill(
                    appointment,
                    HospitalBillType.EXAMINATION,
                    request.getExaminationItem() + "检查费（课程演示）",
                    DEMO_EXAMINATION_FEE_CENTS,
                    now);
            if (resultReview) {
                repository.saveResultReviewExaminationPlan(
                        consultation,
                        completedBooking,
                        waitingEpisode,
                        reviewedOrder,
                        order,
                        examinationBill);
            } else {
                repository.saveExaminationPlan(
                        consultation,
                        completedBooking,
                        waitingEpisode,
                        order,
                        examinationBill);
            }
            return toExaminationView(order);
        }
    }

    ExaminationOrderView publishDemoExaminationReport(
            SessionInfo session,
            PublishDemoExaminationReportRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        HospitalExaminationOrder current = requireOwnedExamination(
                session, request.getOrderId());
        Object episodeLock = episodeLocks.computeIfAbsent(
                current.episodeId(), ignored -> new Object());
        synchronized (episodeLock) {
            HospitalExaminationOrder order = requireOwnedExamination(
                    session, request.getOrderId());
            if (order.status() != ExaminationStatus.ORDERED) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                        "Only an ordered examination can publish its demo report.");
            }
            HospitalEpisode episode = repository.findEpisodeById(order.episodeId())
                    .orElseThrow(() -> new IllegalStateException(
                            "examination episode does not exist: " + order.episodeId()));
            if (episode.status() != EpisodeStatus.WAITING_FOR_RESULTS) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                        "The clinical episode is not waiting for results.");
            }
            LocalDateTime now = LocalDateTime.now(clock);
            HospitalExaminationOrder reportedOrder = new HospitalExaminationOrder(
                    order.orderId(), order.episodeId(), order.orderedAppointmentId(),
                    order.doctorId(), order.patientUserId(), order.itemName(),
                    order.instructions(), ExaminationStatus.RESULT_READY,
                    order.orderedAt(), null);
            HospitalExaminationReport report = new HospitalExaminationReport(
                    "exam-report-" + UUID.randomUUID(),
                    order.orderId(),
                    order.itemName() + "演示结果：指标已完成采集，"
                            + "请由接诊医生结合病情解读。本内容仅用于课程流程演示。",
                    now);
            HospitalEpisode readyEpisode = new HospitalEpisode(
                    episode.episodeId(), episode.patientUserId(), episode.departmentId(),
                    EpisodeStatus.RESULT_READY, episode.openedAt(), null);
            repository.saveExaminationReport(reportedOrder, report, readyEpisode);
            return toExaminationView(reportedOrder);
        }
    }

    AppointmentBookingView bookResultReview(
            SessionInfo session,
            BookResultReviewRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        HospitalExaminationOrder initial = requireOwnedExamination(
                session, request.getOrderId());
        // Result-review uniqueness belongs to the clinical episode, not one schedule.
        Object episodeLock = episodeLocks.computeIfAbsent(
                initial.episodeId(), ignored -> new Object());
        synchronized (episodeLock) {
            HospitalExaminationOrder order = requireOwnedExamination(
                    session, request.getOrderId());
            if (order.status() != ExaminationStatus.RESULT_READY) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                        "The examination result is not ready for review.");
            }
            HospitalEpisode episode = repository.findEpisodeById(order.episodeId())
                    .orElseThrow(() -> new IllegalStateException(
                            "examination episode does not exist: " + order.episodeId()));
            if (episode.status() != EpisodeStatus.RESULT_READY) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                        "The clinical episode is not ready for result review.");
            }
            boolean alreadyBooked = repository
                    .findBookingsByPatientUserId(session.getUserId()).stream()
                    .map(HospitalBooking::appointment)
                    .filter(appointment -> appointment.status()
                            == AppointmentStatus.BOOKED)
                    .anyMatch(appointment -> appointment.visitType()
                            == VisitType.RESULT_REVIEW
                            && appointment.episodeId().equals(order.episodeId()));
            if (alreadyBooked) {
                throw businessFailure(
                        ErrorCodes.HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED,
                        "A result-review visit is already booked for this episode.");
            }
            String patientDoctorId = repository
                    .findActiveDoctorByUserId(session.getUserId())
                    .map(HospitalDoctor::doctorId)
                    .orElse(null);
            HospitalSlot selectedSlot = selectResultReviewSlot(order, patientDoctorId);
            Object scheduleLock = scheduleLocks.computeIfAbsent(
                    selectedSlot.scheduleId(), ignored -> new Object());
            synchronized (scheduleLock) {
                List<HospitalAppointment> appointments = repository
                        .findAppointmentsByScheduleId(selectedSlot.scheduleId());
                int queueNumber = allocateQueueNumber(selectedSlot, appointments);
                LocalDateTime now = LocalDateTime.now(clock);
                String appointmentId = "appointment-" + UUID.randomUUID();
                String billId = "bill-" + UUID.randomUUID();
                HospitalAppointment appointment = new HospitalAppointment(
                        appointmentId,
                        session.getUserId(),
                        selectedSlot.scheduleId(),
                        queueNumber,
                        now,
                        null,
                        null,
                        AppointmentStatus.BOOKED,
                        VisitType.RESULT_REVIEW,
                        episode.episodeId(),
                        order.orderedAppointmentId());
                HospitalBill bill = new HospitalBill(
                        billId, appointmentId, now, now, null, PaymentStatus.PAID);
                HospitalBillItem billItem = new HospitalBillItem(
                        "bill-item-" + UUID.randomUUID(),
                        billId,
                        "检查结果回诊（续诊减免）",
                        1,
                        0);
                repository.saveBooking(new HospitalBooking(appointment, bill, billItem));
                return new AppointmentBookingView(
                        appointmentId, queueNumber, AppointmentStatus.BOOKED,
                        billId, PaymentStatus.PAID, 0,
                        selectedSlot.doctorName(), selectedSlot.departmentName(),
                        selectedSlot.startTime(), selectedSlot.endTime(), now, now);
            }
        }
    }

    ConsultationListResponse listMyConsultations(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        return new ConsultationListResponse(repository
                .findConsultationsByPatientUserId(session.getUserId()).stream()
                .sorted(Comparator.comparing(HospitalConsultation::createdAt).reversed())
                .map(this::toConsultationView)
                .toList());
    }

    PatientHealthRecordView getMyHealthRecord(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        HospitalPatientProfile profile = repository
                .findPatientProfile(session.getUserId())
                .orElseGet(() -> emptyProfile(session.getUserId()));
        List<ConsultationRecordView> consultations = repository
                .findConsultationsByPatientUserId(session.getUserId()).stream()
                .sorted(Comparator.comparing(HospitalConsultation::createdAt).reversed())
                .map(this::toConsultationView)
                .toList();
        List<ExaminationOrderView> examinations = repository
                .findExaminationOrdersByPatientUserId(session.getUserId()).stream()
                .sorted(Comparator.comparing(
                        HospitalExaminationOrder::orderedAt).reversed())
                .map(this::toExaminationView)
                .toList();
        return new PatientHealthRecordView(
                toProfileView(profile), consultations, examinations);
    }

    PatientHealthProfileView updateMyHealthProfile(
            SessionInfo session,
            UpdatePatientHealthProfileRequest request) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(request, "request must not be null");
        requireProfileText(request.getBloodType(), "bloodType", 30);
        requireProfileText(request.getAllergies(), "allergies", 500);
        requireProfileText(request.getMedicalHistory(), "medicalHistory", 1_000);
        requireProfileText(request.getLongTermMedication(), "longTermMedication", 500);
        requireProfileText(request.getEmergencyContact(), "emergencyContact", 200);
        HospitalPatientProfile profile = new HospitalPatientProfile(
                session.getUserId(),
                request.getBloodType(),
                request.getAllergies(),
                request.getMedicalHistory(),
                request.getLongTermMedication(),
                request.getEmergencyContact(),
                LocalDateTime.now(clock));
        repository.savePatientProfile(profile);
        return toProfileView(profile);
    }

    private DoctorScheduleView toDoctorScheduleView(HospitalSlot slot) {
        List<HospitalAppointment> appointments = repository
                .findAppointmentsByScheduleId(slot.scheduleId());
        List<DoctorAppointmentView> pendingAppointments = appointments.stream()
                .filter(appointment -> appointment.status() == AppointmentStatus.BOOKED)
                .sorted(Comparator.comparingInt(HospitalAppointment::queueNumber)
                        .thenComparing(HospitalAppointment::createdAt))
                .map(appointment -> new DoctorAppointmentView(
                        appointment.appointmentId(),
                        appointment.patientUserId(),
                        appointment.queueNumber(),
                        appointment.status(),
                        appointment.visitType(),
                        appointment.sourceFirstVisitAppointmentId(),
                        appointment.createdAt()))
                .toList();
        int dynamicBookings = (int) appointments.stream()
                .filter(HospitalAppointment::occupiesSlot)
                .count();
        int remaining = Math.max(
                0, slot.capacity() - slot.bookedCount() - dynamicBookings);
        return new DoctorScheduleView(
                slot.scheduleId(),
                slot.departmentId(),
                slot.departmentName(),
                slot.startTime(),
                slot.endTime(),
                slot.capacity(),
                remaining,
                slot.published(),
                pendingAppointments);
    }

    private static DoctorAppointmentView toDoctorAppointmentView(
            HospitalAppointment appointment) {
        return new DoctorAppointmentView(
                appointment.appointmentId(),
                appointment.patientUserId(),
                appointment.queueNumber(),
                appointment.status(),
                appointment.visitType(),
                appointment.sourceFirstVisitAppointmentId(),
                appointment.createdAt());
    }

    private ConsultationRecordView toConsultationView(
            HospitalConsultation consultation) {
        HospitalAppointment appointment = repository.findAppointmentById(
                        consultation.appointmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "consultation appointment does not exist: "
                                + consultation.appointmentId()));
        HospitalSlot slot = repository.findSlotById(appointment.scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "consultation schedule does not exist: "
                                + appointment.scheduleId()));
        return new ConsultationRecordView(
                consultation.consultationId(),
                consultation.appointmentId(),
                consultation.doctorId(),
                slot.doctorName(),
                slot.doctorTitle(),
                consultation.patientUserId(),
                slot.departmentName(),
                appointment.visitType(),
                consultation.outcome(),
                consultation.diagnosisOpinion(),
                consultation.examinationAdvice(),
                consultation.treatmentAdvice(),
                consultation.medicationAdvice(),
                consultation.followUpAdvice(),
                consultation.createdAt());
    }

    private ExaminationOrderView toExaminationView(HospitalExaminationOrder order) {
        HospitalSlot slot = repository.findAppointmentById(order.orderedAppointmentId())
                .flatMap(appointment -> repository.findSlotById(appointment.scheduleId()))
                .orElseThrow(() -> new IllegalStateException(
                        "examination source appointment or schedule does not exist: "
                                + order.orderedAppointmentId()));
        HospitalExaminationReport report = repository
                .findExaminationReportByOrderId(order.orderId())
                .orElse(null);
        return new ExaminationOrderView(
                order.orderId(),
                order.episodeId(),
                order.orderedAppointmentId(),
                slot.doctorName(),
                slot.departmentName(),
                order.itemName(),
                order.instructions(),
                order.status(),
                report == null ? null : report.resultSummary(),
                order.orderedAt(),
                report == null ? null : report.reportedAt(),
                repository.findBookingsByPatientUserId(order.patientUserId()).stream()
                        .map(HospitalBooking::appointment)
                        .filter(appointment -> appointment.status()
                                == AppointmentStatus.BOOKED)
                        .anyMatch(appointment -> appointment.visitType()
                                == VisitType.RESULT_REVIEW
                                && appointment.episodeId().equals(order.episodeId())));
    }

    private DoctorFollowUpView toDoctorFollowUpView(HospitalExaminationOrder order) {
        HospitalSlot sourceSlot = repository.findAppointmentById(order.orderedAppointmentId())
                .flatMap(appointment -> repository.findSlotById(appointment.scheduleId()))
                .orElseThrow(() -> new IllegalStateException(
                        "follow-up source appointment or schedule does not exist: "
                                + order.orderedAppointmentId()));
        HospitalSlot reviewSlot = repository
                .findBookingsByPatientUserId(order.patientUserId()).stream()
                .map(HospitalBooking::appointment)
                .filter(appointment -> appointment.status()
                        == AppointmentStatus.BOOKED)
                .filter(appointment -> appointment.visitType() == VisitType.RESULT_REVIEW
                        && appointment.episodeId().equals(order.episodeId()))
                .findFirst()
                .flatMap(appointment -> repository.findSlotById(appointment.scheduleId()))
                .orElse(null);
        return new DoctorFollowUpView(
                order.orderId(),
                order.episodeId(),
                order.patientUserId(),
                sourceSlot.departmentName(),
                order.itemName(),
                order.status(),
                order.orderedAt(),
                reviewSlot != null,
                reviewSlot == null ? null : reviewSlot.doctorName(),
                reviewSlot == null ? null : reviewSlot.startTime(),
                repository.findConsultationsByPatientUserId(order.patientUserId()).stream()
                        .filter(consultation -> consultationBelongsToEpisode(
                                consultation, order.episodeId()))
                        .sorted(Comparator.comparing(HospitalConsultation::createdAt))
                        .map(this::toDoctorClinicalRecordView)
                        .toList(),
                repository.findExaminationOrdersByEpisodeId(order.episodeId()).stream()
                        .sorted(Comparator.comparing(HospitalExaminationOrder::orderedAt))
                        .map(this::toExaminationView)
                        .toList());
    }

    private DoctorClinicalRecordView toDoctorClinicalRecordView(
            HospitalConsultation consultation) {
        HospitalAppointment appointment = repository
                .findAppointmentById(consultation.appointmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "consultation appointment does not exist: "
                                + consultation.appointmentId()));
        List<ExaminationOrderView> examinations = repository
                .findExaminationOrdersByEpisodeId(appointment.episodeId()).stream()
                .sorted(Comparator.comparing(HospitalExaminationOrder::orderedAt))
                .map(this::toExaminationView)
                .toList();
        return new DoctorClinicalRecordView(
                toConsultationView(consultation), examinations);
    }

    private boolean consultationBelongsToEpisode(
            HospitalConsultation consultation, String episodeId) {
        return repository.findAppointmentById(consultation.appointmentId())
                .map(HospitalAppointment::episodeId)
                .filter(episodeId::equals)
                .isPresent();
    }

    private HospitalExaminationOrder requireOwnedExamination(
            SessionInfo session,
            String orderId) {
        HospitalExaminationOrder order = repository.findExaminationOrderById(orderId)
                .orElseThrow(() -> businessFailure(
                        ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND,
                        "The examination order does not exist."));
        if (!order.patientUserId().equals(session.getUserId())) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND,
                    "The examination order does not exist.");
        }
        return order;
    }

    private HospitalSlot selectResultReviewSlot(
            HospitalExaminationOrder order, String excludedDoctorId) {
        HospitalAppointment sourceAppointment = repository.findAppointmentById(
                        order.orderedAppointmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "examination source appointment does not exist"));
        HospitalSlot sourceSlot = repository.findSlotById(sourceAppointment.scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "examination source appointment or schedule does not exist"));
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate endDate = now.toLocalDate().plusDays(7);
        java.util.Set<String> usedScheduleIds = sourceAppointment.visitType()
                == VisitType.RESULT_REVIEW
                ? repository.findBookingsByPatientUserId(order.patientUserId()).stream()
                        .map(HospitalBooking::appointment)
                        .filter(appointment -> appointment.episodeId().equals(order.episodeId()))
                        .map(HospitalAppointment::scheduleId)
                        .collect(java.util.stream.Collectors.toSet())
                : java.util.Set.of();
        List<HospitalSlot> eligible = repository.findSlots(now.toLocalDate(), endDate).stream()
                .filter(HospitalSlot::published)
                .filter(slot -> slot.endTime().isAfter(now))
                .filter(slot -> slot.departmentId().equals(sourceSlot.departmentId()))
                .filter(slot -> !usedScheduleIds.contains(slot.scheduleId()))
                .filter(this::hasRemainingCapacity)
                .sorted(Comparator
                        .comparing((HospitalSlot slot) -> !slot.doctorId()
                                .equals(sourceSlot.doctorId()))
                        .thenComparing(HospitalSlot::startTime))
                .toList();
        if (eligible.isEmpty()) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_SLOT_FULL,
                    "No continuation schedule is available within seven days.");
        }
        return eligible.stream()
                .filter(slot -> excludedDoctorId == null
                        || !slot.doctorId().equals(excludedDoctorId))
                .findFirst()
                .orElseThrow(HospitalService::selfBookingForbidden);
    }

    private boolean hasRemainingCapacity(HospitalSlot slot) {
        long dynamic = repository.findAppointmentsByScheduleId(slot.scheduleId()).stream()
                .filter(HospitalAppointment::occupiesSlot)
                .count();
        return slot.bookedCount() + dynamic < slot.capacity();
    }

    private static HospitalAppointment completedAppointment(
            HospitalAppointment appointment,
            LocalDateTime completedAt) {
        return new HospitalAppointment(
                appointment.appointmentId(),
                appointment.patientUserId(),
                appointment.scheduleId(),
                appointment.queueNumber(),
                appointment.createdAt(),
                null,
                completedAt,
                AppointmentStatus.COMPLETED,
                appointment.visitType(),
                appointment.episodeId(),
                appointment.sourceFirstVisitAppointmentId());
    }

    private HospitalDoctor requireDoctor(SessionInfo session) {
        return repository.findActiveDoctorByUserId(session.getUserId())
                .orElseThrow(() -> businessFailure(
                        ErrorCodes.AUTH_FORBIDDEN,
                        "This account is not bound to an active hospital doctor."));
    }

    private void requireHospitalAdmin(SessionInfo session) {
        Objects.requireNonNull(session, "session must not be null");
        if (!session.canAdminister(ModuleNames.HOSPITAL)) {
            throw businessFailure(
                    ErrorCodes.AUTH_FORBIDDEN,
                    "This account cannot administer the hospital module.");
        }
    }

    private AdminDepartmentView adminDepartmentView(
            HospitalDepartment department, List<HospitalDepartment> departments) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new AdminDepartmentView(
                department.departmentId(),
                department.departmentName(),
                department.parentDepartmentId(),
                departments.stream()
                        .filter(parent -> parent.departmentId().equals(
                                department.parentDepartmentId()))
                        .map(HospitalDepartment::departmentName)
                        .findFirst()
                        .orElse(null),
                department.bookable(),
                department.active(),
                (int) repository.findAllDoctors().stream()
                        .filter(HospitalDoctor::active)
                        .filter(doctor -> doctor.departmentId().equals(
                                department.departmentId()))
                        .count(),
                (int) repository.findAllSlots().stream()
                        .filter(slot -> slot.departmentId().equals(
                                department.departmentId()))
                        .filter(slot -> slot.endTime().isAfter(now))
                        .count());
    }

    private static String requireDepartmentName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("departmentName is required");
        }
        String name = value.trim();
        if (name.length() > 50) {
            throw new IllegalArgumentException(
                    "departmentName must not exceed 50 characters");
        }
        return name;
    }

    private static void ensureUniqueDepartmentName(
            List<HospitalDepartment> departments,
            String excludedDepartmentId,
            String parentDepartmentId,
            String name) {
        boolean duplicate = departments.stream()
                .filter(department -> excludedDepartmentId == null
                        || !department.departmentId().equals(excludedDepartmentId))
                .filter(department -> Objects.equals(
                        department.parentDepartmentId(), parentDepartmentId))
                .anyMatch(department -> department.departmentName()
                        .equalsIgnoreCase(name));
        if (duplicate) {
            throw departmentConflict(
                    "A department with this name already exists under the selected parent.");
        }
    }

    private static void validateDepartmentParent(
            List<HospitalDepartment> departments,
            String currentDepartmentId,
            String parentDepartmentId,
            boolean active) {
        if (parentDepartmentId == null) {
            return;
        }
        HospitalDepartment parent = departments.stream()
                .filter(department -> department.departmentId().equals(parentDepartmentId))
                .findFirst()
                .orElseThrow(() -> departmentConflict(
                        "The selected parent department does not exist."));
        if (active && !parent.active()) {
            throw departmentConflict(
                    "An active department requires an active parent department.");
        }
        if (parent.bookable()) {
            throw departmentConflict(
                    "A bookable clinic cannot contain child departments.");
        }
        Set<String> visited = new HashSet<>();
        HospitalDepartment cursor = parent;
        while (cursor != null) {
            if (cursor.departmentId().equals(currentDepartmentId)) {
                throw departmentConflict("Department hierarchy cannot contain a cycle.");
            }
            if (!visited.add(cursor.departmentId())) {
                throw departmentConflict("Department hierarchy contains a cycle.");
            }
            String nextId = cursor.parentDepartmentId();
            cursor = nextId == null ? null : departments.stream()
                    .filter(department -> department.departmentId().equals(nextId))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static HospitalBusinessException departmentConflict(String message) {
        return businessFailure(ErrorCodes.HOSPITAL_DEPARTMENT_CONFLICT, message);
    }

    private AdminAppointmentView adminAppointmentView(
            HospitalSlot slot, HospitalBooking booking) {
        HospitalAppointment appointment = booking.appointment();
        return new AdminAppointmentView(
                appointment.appointmentId(),
                appointment.patientUserId(),
                appointment.scheduleId(),
                appointment.queueNumber(),
                appointment.status(),
                appointment.visitType(),
                slot.departmentName(),
                slot.doctorName(),
                slot.startTime(),
                slot.endTime(),
                booking.billItem().amountCents(),
                booking.bill().paymentStatus(),
                appointment.status() == AppointmentStatus.BOOKED
                        && LocalDateTime.now(clock).isBefore(slot.startTime()));
    }

    private boolean hasPublishedDoctorConflict(
            String doctorId,
            String excludedScheduleId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return repository.findSlotsByDoctorId(doctorId).stream()
                .filter(HospitalSlot::published)
                .filter(slot -> excludedScheduleId == null
                        || !slot.scheduleId().equals(excludedScheduleId))
                .anyMatch(slot -> slot.startTime().isBefore(endTime)
                        && startTime.isBefore(slot.endTime()));
    }

    private static HospitalBusinessException scheduleConflict() {
        return businessFailure(
                ErrorCodes.HOSPITAL_SCHEDULE_CONFLICT,
                "The doctor already has a published schedule during this time.");
    }

    private HospitalBooking requireAssignedBooking(
            HospitalDoctor doctor,
            String appointmentId) {
        HospitalBooking booking = repository.findBookingById(appointmentId)
                .orElseThrow(HospitalService::appointmentNotFound);
        HospitalSlot slot = repository.findSlotById(
                        booking.appointment().scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "appointment schedule does not exist: "
                                + booking.appointment().scheduleId()));
        if (!slot.doctorId().equals(doctor.doctorId())) {
            throw appointmentNotFound();
        }
        return booking;
    }

    private HospitalPatientProfile emptyProfile(String patientUserId) {
        return new HospitalPatientProfile(
                patientUserId, "", "", "", "", "", LocalDateTime.now(clock));
    }

    private static PatientHealthProfileView toProfileView(
            HospitalPatientProfile profile) {
        return new PatientHealthProfileView(
                profile.bloodType(),
                profile.allergies(),
                profile.medicalHistory(),
                profile.longTermMedication(),
                profile.emergencyContact(),
                profile.updatedAt());
    }

    private static void validateConsultationRequest(SubmitConsultationRequest request) {
        requireConsultationText(
                request.getDiagnosisOpinion(), "diagnosisOpinion", true);
        requireConsultationText(
                request.getTreatmentAdvice(), "treatmentAdvice", true);
        requireConsultationText(
                request.getExaminationAdvice(), "examinationAdvice", false);
        requireConsultationText(
                request.getMedicationAdvice(), "medicationAdvice", false);
        requireConsultationText(
                request.getFollowUpAdvice(), "followUpAdvice", false);
    }

    private static void requireConsultationText(
            String value,
            String fieldName,
            boolean required) {
        if (required && (value == null || value.isBlank())) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value != null && value.length() > 1_000) {
            throw new IllegalArgumentException(fieldName + " must not exceed 1000 characters");
        }
    }

    private static void requireProfileText(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed " + maxLength + " characters");
        }
    }

    private HospitalPatientBill unpaidClinicalBill(
            HospitalAppointment appointment,
            HospitalBillType billType,
            String itemName,
            long amountCents,
            LocalDateTime createdAt) {
        String billId = "bill-" + UUID.randomUUID();
        return new HospitalPatientBill(
                billId,
                appointment.appointmentId(),
                appointment.patientUserId(),
                billType,
                PaymentStatus.UNPAID,
                createdAt,
                null,
                null,
                new HospitalBillItem(
                        "bill-item-" + UUID.randomUUID(),
                        billId,
                        itemName,
                        1,
                        amountCents));
    }

    private PatientBillView toPatientBillView(HospitalPatientBill bill) {
        HospitalAppointment appointment = repository.findAppointmentById(bill.appointmentId())
                .orElseThrow(() -> new IllegalStateException(
                        "bill appointment does not exist: " + bill.appointmentId()));
        HospitalSlot slot = repository.findSlotById(appointment.scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "bill schedule does not exist: " + appointment.scheduleId()));
        return new PatientBillView(
                bill.billId(),
                bill.appointmentId(),
                bill.billType(),
                bill.item().itemName(),
                bill.amountCents(),
                bill.paymentStatus(),
                slot.departmentName(),
                slot.doctorName(),
                slot.startTime(),
                bill.createdAt(),
                bill.paidAt(),
                bill.refundedAt());
    }

    private static HospitalBusinessException billNotFound() {
        return businessFailure(
                ErrorCodes.HOSPITAL_BILL_NOT_FOUND,
                "The selected bill does not exist for this patient.");
    }

    private AppointmentView toAppointmentView(HospitalBooking booking) {
        HospitalAppointment appointment = booking.appointment();
        HospitalSlot slot = repository.findSlotById(appointment.scheduleId())
                .orElseThrow(() -> new IllegalStateException(
                        "appointment schedule does not exist: "
                                + appointment.scheduleId()));
        return new AppointmentView(
                appointment.appointmentId(),
                appointment.scheduleId(),
                appointment.queueNumber(),
                appointment.status(),
                appointment.visitType(),
                slot.departmentName(),
                slot.doctorName(),
                slot.doctorTitle(),
                slot.startTime(),
                slot.endTime(),
                appointment.createdAt(),
                booking.bill().billId(),
                booking.bill().paymentStatus(),
                booking.billItem().amountCents());
    }

    private SlotView toView(HospitalSlot slot, String currentUserId) {
        List<HospitalAppointment> appointments =
                repository.findAppointmentsByScheduleId(slot.scheduleId());
        int dynamicBookings = (int) appointments.stream()
                .filter(HospitalAppointment::occupiesSlot)
                .count();
        boolean bookedByCurrentUser = currentUserId != null && appointments.stream()
                .filter(HospitalAppointment::occupiesSlot)
                .anyMatch(appointment -> appointment.patientUserId().equals(currentUserId));
        int remaining = Math.max(
                0, slot.capacity() - slot.bookedCount() - dynamicBookings);
        SlotAvailability availability = !slot.published()
                ? SlotAvailability.CLOSED
                : remaining == 0
                        ? SlotAvailability.FULL
                        : SlotAvailability.AVAILABLE;
        return new SlotView(
                slot.scheduleId(),
                slot.departmentId(),
                slot.departmentName(),
                slot.doctorId(),
                slot.doctorName(),
                slot.doctorTitle(),
                slot.startTime(),
                slot.endTime(),
                slot.priceCents(),
                slot.capacity(),
                remaining,
                availability,
                bookedByCurrentUser);
    }

    private void validateBookableSlot(HospitalSlot slot) {
        if (!slot.published()) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_SCHEDULE_CLOSED,
                    "The selected schedule is closed.");
        }
        if (!LocalDateTime.now(clock).isBefore(slot.startTime())) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_SCHEDULE_STARTED,
                    "The selected schedule has already started.");
        }
    }

    private void rejectSelfBooking(SessionInfo session, HospitalSlot slot) {
        repository.findActiveDoctorByUserId(session.getUserId())
                .filter(doctor -> doctor.doctorId().equals(slot.doctorId()))
                .ifPresent(ignored -> {
                    throw selfBookingForbidden();
                });
    }

    private static HospitalBusinessException selfBookingForbidden() {
        return businessFailure(
                ErrorCodes.HOSPITAL_SELF_BOOKING_FORBIDDEN,
                "A doctor cannot book their own schedule.");
    }

    private static void validateBookingRequest(BookAppointmentRequest request) {
        if (request.getScheduleId() == null || request.getScheduleId().isBlank()
                || request.getVisitType() == null) {
            throw new IllegalArgumentException("Booking data is invalid.");
        }
        if (request.getVisitType() == VisitType.FIRST_VISIT
                && request.getSourceFirstVisitAppointmentId() != null) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_FIRST_VISIT_WITH_SOURCE,
                    "A first visit cannot specify a source appointment.");
        }
        if (request.getVisitType() == VisitType.FOLLOW_UP
                && (request.getSourceFirstVisitAppointmentId() == null
                || request.getSourceFirstVisitAppointmentId().isBlank())) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_FOLLOW_UP_WITHOUT_SOURCE,
                    "A follow-up visit requires a source first visit.");
        }
    }

    private FollowUpSource requireFollowUpSourceByConsultation(
            SessionInfo session,
            String consultationId) {
        if (session == null || consultationId == null) {
            throw invalidFollowUpSource();
        }
        HospitalConsultation consultation = repository
                .findConsultationsByPatientUserId(session.getUserId()).stream()
                .filter(candidate -> candidate.consultationId().equals(consultationId))
                .findFirst()
                .orElseThrow(HospitalService::invalidFollowUpSource);
        return validateFollowUpSource(session, consultation);
    }

    private FollowUpSource requireFollowUpSourceByAppointment(
            SessionInfo session,
            String appointmentId) {
        if (session == null || appointmentId == null) {
            throw invalidFollowUpSource();
        }
        HospitalConsultation consultation = repository
                .findConsultationByAppointmentId(appointmentId)
                .filter(candidate -> candidate.patientUserId().equals(session.getUserId()))
                .orElseThrow(HospitalService::invalidFollowUpSource);
        return validateFollowUpSource(session, consultation);
    }

    private FollowUpSource validateFollowUpSource(
            SessionInfo session,
            HospitalConsultation consultation) {
        if (!consultation.patientUserId().equals(session.getUserId())
                || consultation.outcome() != ConsultationOutcome.COMPLETED) {
            throw invalidFollowUpSource();
        }
        HospitalAppointment appointment = repository
                .findAppointmentById(consultation.appointmentId())
                .filter(candidate -> candidate.patientUserId().equals(session.getUserId()))
                .filter(candidate -> candidate.status() == AppointmentStatus.COMPLETED)
                .orElseThrow(HospitalService::invalidFollowUpSource);
        HospitalSlot slot = repository.findSlotById(appointment.scheduleId())
                .orElseThrow(HospitalService::invalidFollowUpSource);
        return new FollowUpSource(consultation, appointment, slot);
    }

    private static HospitalBusinessException invalidFollowUpSource() {
        return businessFailure(
                ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID,
                "The source consultation is unavailable or cannot be used for a follow-up.");
    }

    private record FollowUpSource(
            HospitalConsultation consultation,
            HospitalAppointment appointment,
            HospitalSlot slot) {
    }

    private static int allocateQueueNumber(
            HospitalSlot slot,
            List<HospitalAppointment> appointments) {
        long activeAppointments = appointments.stream()
                .filter(HospitalAppointment::occupiesSlot)
                .count();
        if (slot.bookedCount() + activeAppointments >= slot.capacity()) {
            throw businessFailure(
                    ErrorCodes.HOSPITAL_SLOT_FULL,
                    "The selected schedule is full.");
        }
        int historicalMaximum = appointments.stream()
                .mapToInt(HospitalAppointment::queueNumber)
                .max()
                .orElse(0);
        return Math.max(slot.bookedCount(), historicalMaximum) + 1;
    }

    private static HospitalBusinessException businessFailure(
            String errorCode,
            String message) {
        return new HospitalBusinessException(errorCode, message);
    }

    private static HospitalBusinessException appointmentNotFound() {
        return businessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                "The appointment does not exist or does not belong to this account.");
    }

    private HospitalDepartment requireDepartment(String departmentId) {
        return repository.findActiveDepartments().stream()
                .filter(department -> department.departmentId().equals(departmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "departmentId does not exist"));
    }

    private DoctorApplicationView toView(DoctorApplication application) {
        String departmentName = repository.findActiveDepartments().stream()
                .filter(department -> department.departmentId()
                        .equals(application.departmentId()))
                .map(HospitalDepartment::departmentName)
                .findFirst()
                .orElse(application.departmentId());
        return toView(application, departmentName);
    }

    private static DoctorApplicationView toView(
            DoctorApplication application,
            String departmentName) {
        return new DoctorApplicationView(
                application.requestId(),
                application.applicationType(),
                application.username(),
                application.displayName(),
                application.departmentId(),
                departmentName,
                application.doctorTitle(),
                application.requestedByUserId(),
                application.status(),
                application.targetUserId(),
                application.reviewedByUserId(),
                application.createdAt());
    }

    private static HospitalWorkflowException conflict(String message) {
        return new HospitalWorkflowException(
                ErrorCodes.HOSPITAL_DOCTOR_APPLICATION_CONFLICT, message);
    }
}
