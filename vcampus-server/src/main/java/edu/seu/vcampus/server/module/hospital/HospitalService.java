package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DepartmentView;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
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
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.ModuleNames;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.security.AccountProvisioning;
import edu.seu.vcampus.server.security.ProvisionedAccount;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/** Hospital business rules independent from sockets and Swing. */
final class HospitalService {

    private static final int SEARCH_DAYS = 7;

    private final HospitalRepository repository;
    private final Clock clock;

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
                        department.departmentId(), department.departmentName()))
                .toList());
    }

    synchronized DoctorApplicationView submitDoctorApplication(
            SubmitDoctorApplicationRequest request,
            String requestedByUserId,
            AccountProvisioning accounts) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(accounts, "accounts must not be null");
        HospitalDepartment department = requireDepartment(request.getDepartmentId());
        String username = null;
        String displayName = request.getDisplayName();
        String targetUserId = null;
        if (request.getApplicationType() == DoctorApplicationType.EXISTING_ACCOUNT) {
            ProvisionedAccount account = accounts
                    .findAccountByUsername(request.getExistingUsername())
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
            username = account.username();
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
            AccountProvisioning accounts) {
        Objects.requireNonNull(request, "request must not be null");
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

        ProvisionedAccount account;
        if (application.applicationType() == DoctorApplicationType.EXISTING_ACCOUNT) {
            account = accounts.findAccountByUsername(application.username())
                    .filter(found -> found.userId().equals(application.targetUserId()))
                    .orElseThrow(() -> conflict("原有校园账号已经不存在或发生变化。"));
            if (!account.enabled()) {
                throw conflict("要关联的校园账号已被禁用。请先处理账号状态。");
            }
        } else {
            account = accounts.createGeneratedRegularAccount(
                    "doctor", application.displayName());
        }
        repository.saveDoctorProfile(new DoctorProfile(
                account.userId(), application.departmentId(),
                application.doctorTitle(), true));
        DoctorApplication approved = application.reviewed(
                DoctorApplicationStatus.APPROVED,
                account.username(),
                account.userId(),
                reviewerUserId);
        repository.saveDoctorApplication(approved);
        return toView(approved);
    }

    SlotListResponse searchSlots(SearchSlotsRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.getVisitType() != VisitType.FIRST_VISIT) {
            throw new IllegalArgumentException(
                    "Follow-up slot search is not available in the first submission.");
        }
        if (request.getDepartmentId() == null || request.getDepartmentId().isBlank()) {
            throw new IllegalArgumentException("departmentId is required");
        }
        boolean departmentExists = repository.findActiveDepartments().stream()
                .anyMatch(department -> department.departmentId()
                        .equals(request.getDepartmentId()));
        if (!departmentExists) {
            throw new IllegalArgumentException("departmentId does not exist");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate endDate = today.plusDays(SEARCH_DAYS - 1L);
        LocalDateTime now = LocalDateTime.now(clock);
        return new SlotListResponse(repository.findSlots(today, endDate).stream()
                .filter(HospitalSlot::published)
                .filter(slot -> !slot.startTime().isBefore(now))
                .filter(slot -> slot.departmentId().equals(request.getDepartmentId()))
                .filter(slot -> request.getDoctorId() == null
                        || slot.doctorId().equals(request.getDoctorId()))
                .sorted(Comparator.comparing(HospitalSlot::startTime)
                        .thenComparing(HospitalSlot::doctorName))
                .map(HospitalService::toView)
                .toList());
    }

    private static SlotView toView(HospitalSlot slot) {
        int remaining = slot.capacity() - slot.bookedCount();
        SlotAvailability availability = remaining == 0
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
                availability);
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
