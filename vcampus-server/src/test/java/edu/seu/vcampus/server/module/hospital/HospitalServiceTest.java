package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.AppointmentListResponse;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.CreateScheduleRequest;
import edu.seu.vcampus.common.hospital.CreateDepartmentRequest;
import edu.seu.vcampus.common.hospital.UpdateDepartmentRequest;
import edu.seu.vcampus.common.hospital.AdminCancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.ConsultationListResponse;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextView;
import edu.seu.vcampus.common.hospital.DoctorWorkspaceView;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.MarkAppointmentNoShowRequest;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SetSchedulePublicationRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.hospital.UpdatePatientHealthProfileRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-27T00:00:00Z"),
            ZoneId.of("Asia/Shanghai"));

    private final HospitalService service = new HospitalService(
            new InMemoryHospitalRepository(FIXED_CLOCK), FIXED_CLOCK);

    @Test
    void calculatesModesFromDoctorListAndHospitalAdminScope() {
        HospitalModeAccessView student = service.getModeAccess(
                session("U-STUDENT-001", Role.USER));
        HospitalModeAccessView doctor = service.getModeAccess(
                session("U-DOCTOR-001", Role.USER));
        HospitalModeAccessView administrator = service.getModeAccess(
                session("U-HOSPITAL-ADMIN-001", Role.USER, Set.of(AdminScope.HOSPITAL)));

        assertTrue(student.canAccess(HospitalMode.PATIENT));
        assertFalse(student.canAccess(HospitalMode.DOCTOR));
        assertFalse(student.canAccess(HospitalMode.ADMIN));
        assertTrue(doctor.canAccess(HospitalMode.PATIENT));
        assertTrue(doctor.canAccess(HospitalMode.DOCTOR));
        assertFalse(doctor.canAccess(HospitalMode.ADMIN));
        assertTrue(administrator.canAccess(HospitalMode.PATIENT));
        assertFalse(administrator.canAccess(HospitalMode.DOCTOR));
        assertTrue(administrator.canAccess(HospitalMode.ADMIN));
    }

    @Test
    void administratorCreatesDraftThenPublishesItToPatientsAndDoctor() {
        SessionInfo administrator = session(
                "U-HOSPITAL-ADMIN-001", Role.USER, Set.of(AdminScope.HOSPITAL));
        SessionInfo patient = session("U-ADMIN-SCHEDULE-PATIENT", Role.USER);
        LocalDateTime start = LocalDateTime.now(FIXED_CLOCK)
                .plusDays(5).withHour(16).withMinute(0).withSecond(0).withNano(0);

        var draft = service.createSchedule(
                administrator,
                new CreateScheduleRequest(
                        "doctor-chen", "dept-general",
                        start, start.plusMinutes(30), 1_500, 6));

        assertEquals(SlotAvailability.CLOSED, draft.getAvailability());
        assertFalse(service.searchSlots(
                patient,
                SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"))
                .getSlots().stream()
                .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));
        assertFalse(service.getDoctorWorkspace(session("U-DOCTOR-001", Role.USER))
                .getSchedules().stream()
                .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));

        service.setSchedulePublication(
                administrator,
                new SetSchedulePublicationRequest(draft.getScheduleId(), true));

        assertTrue(service.searchSlots(
                patient,
                SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"))
                .getSlots().stream()
                .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));
        assertTrue(service.getDoctorWorkspace(session("U-DOCTOR-001", Role.USER))
                .getSchedules().stream()
                .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));

        service.setSchedulePublication(
                administrator,
                new SetSchedulePublicationRequest(draft.getScheduleId(), false));
        assertFalse(service.searchSlots(
                patient,
                SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"))
                .getSlots().stream()
                .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));
    }

    @Test
    void protectsScheduleAdministrationAndRefusesClosingBookedSchedule() {
        SessionInfo administrator = session(
                "U-HOSPITAL-ADMIN-001", Role.USER, Set.of(AdminScope.HOSPITAL));
        SessionInfo patient = session("U-ADMIN-SCHEDULE-BOOKER", Role.USER);
        LocalDateTime start = LocalDateTime.now(FIXED_CLOCK)
                .plusDays(5).withHour(17).withMinute(0).withSecond(0).withNano(0);

        assertBusinessFailure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.getAdminScheduleWorkspace(patient));
        assertBusinessFailure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.createSchedule(patient, new CreateScheduleRequest(
                        "doctor-chen", "dept-general",
                        start, start.plusMinutes(30), 1_500, 6)));

        var schedule = service.createSchedule(
                administrator,
                new CreateScheduleRequest(
                        "doctor-chen", "dept-general",
                        start, start.plusMinutes(30), 1_500, 6));
        service.setSchedulePublication(
                administrator,
                new SetSchedulePublicationRequest(schedule.getScheduleId(), true));
        service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit(schedule.getScheduleId()));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SCHEDULE_HAS_APPOINTMENTS,
                () -> service.setSchedulePublication(
                        administrator,
                        new SetSchedulePublicationRequest(schedule.getScheduleId(), false)));
        AdminScheduleWorkspaceView workspace =
                service.getAdminScheduleWorkspace(administrator);
        assertTrue(workspace.getDoctors().stream()
                .anyMatch(doctor -> doctor.getDoctorId().equals("doctor-chen")));
        assertTrue(workspace.getSchedules().stream()
                .anyMatch(slot -> slot.getScheduleId().equals(schedule.getScheduleId())
                        && slot.getRemaining() == 5));
    }

    @Test
    void administratorMaintainsDepartmentHierarchyWithUsageGuards() {
        SessionInfo administrator = session(
                "U-HOSPITAL-ADMIN-001", Role.USER, Set.of(AdminScope.HOSPITAL));
        SessionInfo patient = session("U-DEPARTMENT-PATIENT", Role.USER);

        assertEquals(15, service.getAdminDepartmentWorkspace(administrator)
                .getDepartments().size());
        assertBusinessFailure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.getAdminDepartmentWorkspace(patient));

        var category = service.createDepartment(
                administrator, new CreateDepartmentRequest("康复医学", null, false));
        var clinic = service.createDepartment(
                administrator,
                new CreateDepartmentRequest(
                        "运动康复", category.getDepartmentId(), true));
        assertTrue(clinic.isBookable());
        assertEquals(category.getDepartmentId(), clinic.getParentDepartmentId());

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_DEPARTMENT_CONFLICT,
                () -> service.createDepartment(
                        administrator,
                        new CreateDepartmentRequest(
                                "运动康复", category.getDepartmentId(), true)));

        var renamed = service.updateDepartment(
                administrator,
                new UpdateDepartmentRequest(
                        clinic.getDepartmentId(), "运动损伤康复",
                        category.getDepartmentId(), true, true));
        assertEquals("运动损伤康复", renamed.getDepartmentName());

        service.updateDepartment(
                administrator,
                new UpdateDepartmentRequest(
                        clinic.getDepartmentId(), "运动损伤康复",
                        category.getDepartmentId(), true, false));
        assertFalse(service.listDepartments().getDepartments().stream()
                .anyMatch(item -> item.getDepartmentId().equals(clinic.getDepartmentId())));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_DEPARTMENT_IN_USE,
                () -> service.updateDepartment(
                        administrator,
                        new UpdateDepartmentRequest(
                                "dept-general", "全科门诊", "dept-general-category",
                                false, true)));
    }

    @Test
    void administratorCancelsFutureAbnormalAppointmentAndRefundsRegistration() {
        SessionInfo administrator = session(
                "U-HOSPITAL-ADMIN-001", Role.USER, Set.of(AdminScope.HOSPITAL));
        SessionInfo patient = session("U-ADMIN-CANCEL-PATIENT", Role.USER);
        SessionInfo otherPatient = session("U-ADMIN-CANCEL-OTHER", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));

        var listed = service.listAdminAppointments(administrator).getAppointments().stream()
                .filter(item -> item.getAppointmentId().equals(booking.getAppointmentId()))
                .findFirst()
                .orElseThrow();
        assertEquals(AppointmentStatus.BOOKED, listed.getAppointmentStatus());
        assertEquals(PaymentStatus.PAID, listed.getPaymentStatus());
        assertTrue(listed.isCancellable());
        assertBusinessFailure(ErrorCodes.AUTH_FORBIDDEN,
                () -> service.listAdminAppointments(otherPatient));

        var cancelled = service.cancelAppointmentAsAdmin(
                administrator,
                new AdminCancelAppointmentRequest(booking.getAppointmentId()));
        assertEquals(AppointmentStatus.CANCELLED, cancelled.getAppointmentStatus());
        assertEquals(PaymentStatus.REFUNDED, cancelled.getPaymentStatus());
        assertFalse(cancelled.isCancellable());
        assertEquals(AppointmentStatus.CANCELLED,
                service.listMyAppointments(patient).getAppointments().stream()
                        .filter(item -> item.getAppointmentId().equals(booking.getAppointmentId()))
                        .findFirst().orElseThrow().getAppointmentStatus());

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CANCELLABLE,
                () -> service.cancelAppointmentAsAdmin(
                        administrator,
                        new AdminCancelAppointmentRequest(booking.getAppointmentId())));
    }

    @Test
    void doctorCanUsePatientModeButCannotBookOwnSchedule() {
        SessionInfo doctorAsPatient = session("U-DOCTOR-001", Role.USER);

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SELF_BOOKING_FORBIDDEN,
                () -> service.bookAppointment(
                        doctorAsPatient,
                        BookAppointmentRequest.firstVisit("slot-general-1")));

        AppointmentBookingView otherDoctor = service.bookAppointment(
                doctorAsPatient,
                BookAppointmentRequest.firstVisit("slot-general-3"));
        assertEquals("刘医生", otherDoctor.getDoctorName());
    }

    @Test
    void legacySelfVisitResultReviewSelectsAnotherDoctorSchedule() {
        InMemoryHospitalRepository repository = new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService legacyService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo doctorAsPatient = session("U-DOCTOR-001", Role.USER);
        LocalDateTime now = LocalDateTime.now(FIXED_CLOCK);
        HospitalAppointment legacyAppointment = new HospitalAppointment(
                "appointment-legacy-self",
                doctorAsPatient.getUserId(),
                "slot-general-1",
                5,
                now.minusDays(1),
                null,
                null,
                AppointmentStatus.BOOKED,
                VisitType.FIRST_VISIT,
                "episode-legacy-self",
                null);
        HospitalBill bill = new HospitalBill(
                "bill-legacy-self",
                legacyAppointment.appointmentId(),
                now.minusDays(1),
                now.minusDays(1),
                null,
                PaymentStatus.PAID);
        repository.saveBookingAndEpisode(
                new HospitalBooking(
                        legacyAppointment,
                        bill,
                        new HospitalBillItem(
                                "bill-item-legacy-self",
                                bill.billId(),
                                "挂号费",
                                1,
                                1_200)),
                new HospitalEpisode(
                        legacyAppointment.episodeId(),
                        doctorAsPatient.getUserId(),
                        "dept-general",
                        EpisodeStatus.IN_PROGRESS,
                        now.minusDays(1),
                        null));

        ExaminationOrderView order = legacyService.submitExaminationPlan(
                doctorAsPatient,
                new SubmitExaminationPlanRequest(
                        legacyAppointment.appointmentId(),
                        "课程演示初步判断",
                        "血常规",
                        "按检查部门要求准备",
                        ""));
        legacyService.publishDemoExaminationReport(
                doctorAsPatient,
                new PublishDemoExaminationReportRequest(order.getOrderId()));

        AppointmentBookingView review = legacyService.bookResultReview(
                doctorAsPatient,
                new BookResultReviewRequest(order.getOrderId()));
        HospitalAppointment savedReview = repository
                .findAppointmentById(review.getAppointmentId())
                .orElseThrow();
        HospitalSlot selectedSlot = repository.findSlotById(savedReview.scheduleId())
                .orElseThrow();
        assertEquals("doctor-liu", selectedSlot.doctorId());
    }

    @Test
    void listsHierarchicalDepartmentsAndMarksOnlyClinicsBookable() {
        DepartmentListResponse response = service.listDepartments();

        assertEquals(15, response.getDepartments().size());
        var surgery = response.getDepartments().stream()
                .filter(department -> department.getDepartmentId().equals("dept-surgery"))
                .findFirst()
                .orElseThrow();
        var orthopedics = response.getDepartments().stream()
                .filter(department -> department.getDepartmentId().equals("dept-orthopedics"))
                .findFirst()
                .orElseThrow();
        var jointSurgery = response.getDepartments().stream()
                .filter(department -> department.getDepartmentId()
                        .equals("dept-joint-surgery"))
                .findFirst()
                .orElseThrow();

        assertFalse(surgery.isBookable());
        assertEquals("dept-surgery", orthopedics.getParentDepartmentId());
        assertFalse(orthopedics.isBookable());
        assertEquals("dept-orthopedics", jointSurgery.getParentDepartmentId());
        assertTrue(jointSurgery.isBookable());
    }

    @Test
    void searchesPublishedSlotsAndKeepsFullSlotsVisible() {
        SlotListResponse response = service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-psychology", null));

        assertEquals(2, response.getSlots().size());
        assertTrue(response.getSlots().stream()
                .anyMatch(slot -> slot.getAvailability() == SlotAvailability.AVAILABLE));
        assertTrue(response.getSlots().stream()
                .anyMatch(slot -> slot.getAvailability() == SlotAvailability.FULL));
    }

    @Test
    void appliesDoctorFilterAndIncludesSeventhCalendarDay() {
        SlotListResponse response = service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-dental", "doctor-zhao"));

        assertEquals(2, response.getSlots().size());
        assertEquals(
                FIXED_CLOCK.instant().atZone(FIXED_CLOCK.getZone())
                        .toLocalDate().plusDays(6),
                response.getSlots().get(1).getStartTime().toLocalDate());
    }

    @Test
    void returnsMultipleDailySchedulesForTheSelectedDoctor() {
        SlotListResponse response = service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-joint-surgery", "doctor-lin"));

        assertEquals(2, response.getSlots().size());
        assertEquals(
                response.getSlots().get(0).getStartTime().toLocalDate(),
                response.getSlots().get(1).getStartTime().toLocalDate());
        assertTrue(response.getSlots().get(0).getStartTime()
                .isBefore(response.getSlots().get(1).getStartTime()));
    }

    @Test
    void hidesUnpublishedSlots() {
        SlotListResponse response = service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-eye", null));

        assertEquals(1, response.getSlots().size());
        assertEquals("slot-eye-1", response.getSlots().getFirst().getScheduleId());
    }

    @Test
    void rejectsUnknownDepartmentAndFollowUpWithoutAuthenticatedSource() {
        assertThrows(IllegalArgumentException.class, () -> service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-missing", null)));
        assertThrows(IllegalArgumentException.class, () -> service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-surgery", null)));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID,
                () -> service.searchSlots(SearchSlotsRequest.followUp("consultation-1")));
    }

    @Test
    void booksFirstVisitWithPaidBillQueueNumberAndUpdatedAvailability() {
        SessionInfo patient = session("U-PATIENT-001", Role.USER);

        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        assertEquals(AppointmentStatus.BOOKED, booking.getAppointmentStatus());
        assertEquals(PaymentStatus.PAID, booking.getPaymentStatus());
        assertEquals(5, booking.getQueueNumber());
        assertEquals(1_200, booking.getAmountCents());
        assertEquals("陈医生", booking.getDoctorName());
        SlotListResponse afterBooking = service.searchSlots(patient,
                SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"));
        assertEquals(7, afterBooking.getSlots().getFirst().getRemaining());
        assertTrue(afterBooking.getSlots().getFirst().isBookedByCurrentUser());
    }

    @Test
    void createsPatientTreatmentBillAndAllowsOnlyItsOwnerToPay() {
        SessionInfo patient = session("U-BILL-PATIENT-001", Role.USER);
        SessionInfo otherPatient = session("U-BILL-PATIENT-OTHER", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        assertEquals(1, service.listMyBills(patient).getBills().size());
        assertEquals(PaymentStatus.PAID,
                service.listMyBills(patient).getBills().getFirst().getPaymentStatus());

        service.submitConsultation(doctor, consultationRequest(booking.getAppointmentId()));

        var unpaid = service.listMyBills(patient).getBills().stream()
                .filter(bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID)
                .findFirst()
                .orElseThrow();
        assertEquals(HospitalBillType.TREATMENT, unpaid.getBillType());
        assertEquals(1_800, unpaid.getAmountCents());
        assertEquals("全科门诊", unpaid.getDepartmentName());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_BILL_NOT_FOUND,
                () -> service.payBill(
                        otherPatient, new PayHospitalBillRequest(unpaid.getBillId())));

        var paid = service.payBill(
                patient, new PayHospitalBillRequest(unpaid.getBillId()));
        assertEquals(PaymentStatus.PAID, paid.getPaymentStatus());
        assertTrue(paid.getPaidAt() != null);
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_BILL_NOT_PAYABLE,
                () -> service.payBill(
                        patient, new PayHospitalBillRequest(unpaid.getBillId())));
        assertEquals(PaymentStatus.PAID,
                service.listMyBills(patient).getBills().getFirst().getPaymentStatus());
    }

    @Test
    void createsUnpaidExaminationBillWhenDoctorOpensExamination() {
        SessionInfo patient = session("U-BILL-EXAM-PATIENT", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        service.submitExaminationPlan(
                doctor,
                new SubmitExaminationPlanRequest(
                        booking.getAppointmentId(),
                        "初步判断（课程演示）",
                        "血常规",
                        "请按演示流程完成检查。",
                        "检查期间注意休息。"));

        var unpaid = service.listMyBills(patient).getBills().stream()
                .filter(bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID)
                .findFirst()
                .orElseThrow();
        assertEquals(HospitalBillType.EXAMINATION, unpaid.getBillType());
        assertEquals(3_000, unpaid.getAmountCents());
        assertTrue(unpaid.getItemName().contains("血常规"));
    }

    @Test
    void listsOnlyAppointmentsOwnedByTheAuthenticatedPatient() {
        SessionInfo firstPatient = session("U-PATIENT-LIST-1", Role.USER);
        SessionInfo secondPatient = session("U-PATIENT-LIST-2", Role.USER);
        AppointmentBookingView firstBooking = service.bookAppointment(
                firstPatient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        service.bookAppointment(
                secondPatient,
                BookAppointmentRequest.firstVisit("slot-general-3"));

        AppointmentListResponse firstPatientAppointments =
                service.listMyAppointments(firstPatient);

        assertEquals(1, firstPatientAppointments.getAppointments().size());
        var appointment = firstPatientAppointments.getAppointments().getFirst();
        assertEquals(firstBooking.getAppointmentId(), appointment.getAppointmentId());
        assertEquals("全科门诊", appointment.getDepartmentName());
        assertEquals("陈医生", appointment.getDoctorName());
        assertEquals(PaymentStatus.PAID, appointment.getPaymentStatus());
        assertEquals(1_200, appointment.getAmountCents());
    }

    @Test
    void doctorWorkspaceUsesAccountBindingAndReturnsOnlyOwnedSchedulesAndPatients() {
        SessionInfo patient = session("U-DOCTOR-QUEUE-PATIENT", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        DoctorWorkspaceView workspace = service.getDoctorWorkspace(
                session("U-DOCTOR-001", Role.USER));

        assertEquals("doctor-chen", workspace.getDoctorId());
        assertEquals("陈安", workspace.getDoctorName());
        assertEquals("全科门诊", workspace.getDepartmentName());
        assertEquals(3, workspace.getSchedules().size());
        assertEquals("slot-general-1", workspace.getSchedules().getFirst().getScheduleId());
        assertEquals("slot-general-4", workspace.getSchedules().getLast().getScheduleId());
        assertEquals(5, workspace.getSchedules().getFirst()
                .getPendingAppointments().size());
        var appointment = workspace.getSchedules().getFirst()
                .getPendingAppointments().stream()
                .filter(candidate -> candidate.getAppointmentId()
                        .equals(booking.getAppointmentId()))
                .findFirst()
                .orElseThrow();
        assertEquals(booking.getAppointmentId(), appointment.getAppointmentId());
        assertEquals("U-DOCTOR-QUEUE-PATIENT", appointment.getPatientUserId());
        assertEquals(5, appointment.getQueueNumber());
        assertEquals(AppointmentStatus.BOOKED, appointment.getAppointmentStatus());
        assertTrue(workspace.getSchedules().stream()
                .allMatch(schedule -> schedule.getScheduleId().startsWith("slot-general-")));
    }

    @Test
    void doctorWorkspaceRejectsAccountsWithoutAnActiveDoctorBinding() {
        assertBusinessFailure(
                ErrorCodes.AUTH_FORBIDDEN,
                () -> service.getDoctorWorkspace(
                        session("U-STUDENT-001", Role.USER)));
        assertBusinessFailure(
                ErrorCodes.AUTH_FORBIDDEN,
                () -> service.getDoctorWorkspace(
                        session("U-ADMIN-001", Role.SUPER_ADMIN)));
    }

    @Test
    void doctorWorkspaceKeepsExpiredScheduleWhileBookedPatientsStillNeedHandling() {
        InMemoryHospitalRepository repository = new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService bookingService = new HospitalService(repository, FIXED_CLOCK);
        AppointmentBookingView booking = bookingService.bookAppointment(
                session("U-EXPIRED-QUEUE-PATIENT", Role.USER),
                BookAppointmentRequest.firstVisit("slot-general-1"));
        HospitalService afterSchedule = new HospitalService(
                repository,
                Clock.offset(FIXED_CLOCK, Duration.ofDays(2)));

        DoctorWorkspaceView workspace = afterSchedule.getDoctorWorkspace(
                session("U-DOCTOR-001", Role.USER));

        assertTrue(workspace.getSchedules().stream()
                .filter(schedule -> schedule.getScheduleId().equals("slot-general-1"))
                .flatMap(schedule -> schedule.getPendingAppointments().stream())
                .anyMatch(appointment -> appointment.getAppointmentId()
                        .equals(booking.getAppointmentId())));
    }

    @Test
    void doctorPendingQueueExcludesCancelledAppointments() {
        SessionInfo patient = session("U-DOCTOR-CANCELLED-PATIENT", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        service.cancelAppointment(
                patient,
                new CancelAppointmentRequest(booking.getAppointmentId()));

        DoctorWorkspaceView workspace = service.getDoctorWorkspace(
                session("U-DOCTOR-001", Role.USER));

        assertEquals(4, workspace.getSchedules().getFirst()
                .getPendingAppointments().size());
        assertTrue(workspace.getSchedules().getFirst().getPendingAppointments().stream()
                .noneMatch(appointment -> appointment.getAppointmentId()
                        .equals(booking.getAppointmentId())));
    }

    @Test
    void doctorCompletesConsultationAndPatientReadsSignedRecord() {
        SessionInfo patient = session("U-STUDENT-001", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        DoctorConsultationContextView context = service.getDoctorConsultationContext(
                doctor,
                new DoctorConsultationContextRequest(booking.getAppointmentId()));

        assertEquals("U-STUDENT-001", context.getAppointment().getPatientUserId());
        assertEquals("全科门诊", context.getDepartmentName());
        assertEquals("O型（演示）", context.getHealthProfile().getBloodType());
        assertEquals("无已知过敏（演示）", context.getHealthProfile().getAllergies());
        assertTrue(context.getPreviousConsultations().isEmpty());

        ConsultationRecordView record = service.submitConsultation(
                doctor,
                consultationRequest(booking.getAppointmentId()));

        assertEquals(booking.getAppointmentId(), record.getAppointmentId());
        assertEquals("doctor-chen", record.getDoctorId());
        assertEquals("U-STUDENT-001", record.getPatientUserId());
        assertEquals("上呼吸道感染（课程演示）", record.getDiagnosisOpinion());
        assertEquals("对症处置，注意休息与补水。", record.getTreatmentAdvice());

        ConsultationListResponse patientRecords = service.listMyConsultations(patient);
        assertEquals(1, patientRecords.getConsultations().size());
        assertEquals(record.getConsultationId(),
                patientRecords.getConsultations().getFirst().getConsultationId());
        PatientHealthRecordView healthRecord = service.getMyHealthRecord(patient);
        assertEquals("O型（演示）", healthRecord.getHealthProfile().getBloodType());
        assertEquals(1, healthRecord.getConsultations().size());
        assertEquals(record.getConsultationId(),
                healthRecord.getConsultations().getFirst().getConsultationId());
        assertTrue(service.getMyHealthRecord(
                session("U-OTHER-PATIENT", Role.USER))
                .getConsultations().isEmpty());
        assertTrue(service.listMyConsultations(
                session("U-OTHER-PATIENT", Role.USER))
                .getConsultations().isEmpty());
        assertEquals(AppointmentStatus.COMPLETED,
                service.listMyAppointments(patient).getAppointments().getFirst()
                        .getAppointmentStatus());
        assertTrue(service.getDoctorWorkspace(doctor).getSchedules().getFirst()
                .getPendingAppointments().stream()
                .noneMatch(appointment -> appointment.getAppointmentId()
                        .equals(booking.getAppointmentId())));
        DoctorWorkspaceView signedWorkspace = service.getDoctorWorkspace(doctor);
        assertEquals(1, signedWorkspace.getSignedRecords().size());
        assertEquals(record.getConsultationId(), signedWorkspace.getSignedRecords()
                .getFirst().getConsultation().getConsultationId());

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CONSULTABLE,
                () -> service.submitConsultation(
                        doctor,
                        consultationRequest(booking.getAppointmentId())));
    }

    @Test
    void patientBooksPaidFollowUpFromCompletedConsultationInSameDepartment() {
        InMemoryHospitalRepository repository = new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService followUpService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo patient = session("U-FOLLOW-UP-PATIENT", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);

        AppointmentBookingView firstVisit = followUpService.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        ConsultationRecordView consultation = followUpService.submitConsultation(
                doctor,
                consultationRequest(firstVisit.getAppointmentId()));

        SlotListResponse candidates = followUpService.searchSlots(
                patient,
                SearchSlotsRequest.followUp(consultation.getConsultationId()));
        assertFalse(candidates.getSlots().isEmpty());
        assertTrue(candidates.getSlots().stream()
                .allMatch(slot -> slot.getDepartmentId().equals("dept-general")));
        assertTrue(candidates.getSlots().stream()
                .noneMatch(slot -> slot.getScheduleId().equals("slot-general-1")));
        assertEquals("doctor-chen", candidates.getSlots().getFirst().getDoctorId());
        assertTrue(candidates.getSlots().getFirst().getAvailability()
                == SlotAvailability.AVAILABLE);
        assertTrue(candidates.getSlots().stream()
                .anyMatch(slot -> slot.getDoctorId().equals("doctor-liu")));

        AppointmentBookingView followUp = followUpService.bookAppointment(
                patient,
                BookAppointmentRequest.followUp(
                        "slot-general-3", firstVisit.getAppointmentId()));
        HospitalAppointment saved = repository
                .findAppointmentById(followUp.getAppointmentId())
                .orElseThrow();
        HospitalAppointment source = repository
                .findAppointmentById(firstVisit.getAppointmentId())
                .orElseThrow();

        assertEquals(VisitType.FOLLOW_UP, saved.visitType());
        assertEquals(firstVisit.getAppointmentId(), saved.sourceFirstVisitAppointmentId());
        assertFalse(saved.episodeId().equals(source.episodeId()));
        assertEquals(1_800, followUp.getAmountCents());
        assertEquals(PaymentStatus.PAID, followUp.getPaymentStatus());

        DoctorConsultationContextView followUpContext = followUpService
                .getDoctorConsultationContext(
                        session("U-DOCTOR-002", Role.USER),
                        new DoctorConsultationContextRequest(
                                followUp.getAppointmentId()));
        assertEquals(firstVisit.getAppointmentId(),
                followUpContext.getAppointment().getSourceAppointmentId());
        assertTrue(followUpContext.getPreviousConsultations().stream()
                .anyMatch(record -> record.getAppointmentId()
                        .equals(firstVisit.getAppointmentId())));
    }

    @Test
    void followUpRejectsForeignSourceAndScheduleFromAnotherDepartment() {
        SessionInfo owner = session("U-FOLLOW-UP-OWNER", Role.USER);
        SessionInfo otherPatient = session("U-FOLLOW-UP-OTHER", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView firstVisit = service.bookAppointment(
                owner,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        ConsultationRecordView consultation = service.submitConsultation(
                doctor,
                consultationRequest(firstVisit.getAppointmentId()));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID,
                () -> service.searchSlots(
                        otherPatient,
                        SearchSlotsRequest.followUp(consultation.getConsultationId())));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_FOLLOW_UP_SOURCE_INVALID,
                () -> service.bookAppointment(
                        owner,
                        BookAppointmentRequest.followUp(
                                "slot-respiratory-1", firstVisit.getAppointmentId())));
    }

    @Test
    void patientUpdatesOnlyTheHealthProfileOwnedByTheCurrentSession() {
        SessionInfo patient = session("U-STUDENT-001", Role.USER);
        SessionInfo otherPatient = session("U-OTHER-PATIENT", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        var updated = service.updateMyHealthProfile(
                patient,
                new UpdatePatientHealthProfileRequest(
                        "AB型",
                        "花粉过敏",
                        "曾有踝关节扭伤",
                        "无",
                        "家属 13800000000"));

        assertEquals("AB型", updated.getBloodType());
        assertEquals(1L, updated.getVersion());
        assertEquals("花粉过敏",
                service.getMyHealthRecord(patient).getHealthProfile().getAllergies());
        assertEquals("花粉过敏", service.getDoctorConsultationContext(
                        session("U-DOCTOR-001", Role.USER),
                        new DoctorConsultationContextRequest(booking.getAppointmentId()))
                .getHealthProfile().getAllergies());
        assertEquals("未填写",
                service.getMyHealthRecord(otherPatient).getHealthProfile().getAllergies());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateMyHealthProfile(
                        patient,
                        new UpdatePatientHealthProfileRequest(
                                "A".repeat(31), "", "", "", "")));
    }

    @Test
    void rejectsAStalePatientHealthProfileUpdateInsteadOfOverwritingIt() {
        SessionInfo patient = session("U-STUDENT-001", Role.USER);
        var first = service.updateMyHealthProfile(
                patient,
                new UpdatePatientHealthProfileRequest(
                        "A型", "无", "", "", "", 0L));

        assertEquals(1L, first.getVersion());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_HEALTH_PROFILE_CONFLICT,
                () -> service.updateMyHealthProfile(
                        patient,
                        new UpdatePatientHealthProfileRequest(
                                "B型", "花粉过敏", "", "", "", 0L)));
        assertEquals("A型",
                service.getMyHealthRecord(patient).getHealthProfile().getBloodType());
    }

    @Test
    void examinationResultReviewCompletesOneContinuousClinicalEpisode() {
        SessionInfo patient = session("U-STUDENT-001", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView firstVisit = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        ExaminationOrderView ordered = service.submitExaminationPlan(
                doctor,
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(),
                        "发热待查（课程演示）",
                        "血常规",
                        "无需空腹（课程演示）",
                        ""));

        assertEquals(ExaminationStatus.ORDERED, ordered.getStatus());
        assertEquals(edu.seu.vcampus.common.hospital.ConsultationOutcome.WAITING_FOR_RESULTS,
                service.getMyHealthRecord(patient).getConsultations().getFirst().getOutcome());
        assertEquals("无", service.getMyHealthRecord(patient).getConsultations()
                .getFirst().getTreatmentAdvice());
        DoctorWorkspaceView waitingWorkspace = service.getDoctorWorkspace(doctor);
        assertEquals(1, waitingWorkspace.getFollowUps().size());
        assertEquals("U-STUDENT-001",
                waitingWorkspace.getFollowUps().getFirst().getPatientUserId());
        assertEquals(ExaminationStatus.ORDERED,
                waitingWorkspace.getFollowUps().getFirst().getExaminationStatus());
        assertEquals(1, waitingWorkspace.getFollowUps().getFirst()
                .getEpisodeRecords().size());
        assertEquals(1, waitingWorkspace.getFollowUps().getFirst()
                .getEpisodeExaminations().size());
        PatientHealthRecordView waiting = service.getMyHealthRecord(patient);
        assertEquals(1, waiting.getExaminations().size());
        assertEquals(1, waiting.getConsultations().size());
        assertEquals(AppointmentStatus.COMPLETED,
                service.listMyAppointments(patient).getAppointments().getFirst()
                        .getAppointmentStatus());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND,
                () -> service.publishDemoExaminationReport(
                        session("U-OTHER-PATIENT", Role.USER),
                        new PublishDemoExaminationReportRequest(ordered.getOrderId())));

        ExaminationOrderView reported = service.publishDemoExaminationReport(
                patient,
                new PublishDemoExaminationReportRequest(ordered.getOrderId()));
        assertEquals(ExaminationStatus.RESULT_READY, reported.getStatus());
        assertTrue(reported.getResultSummary().contains("课程流程演示"));
        assertFalse(service.getDoctorWorkspace(doctor).getFollowUps().getFirst()
                .isResultReviewBooked());

        AppointmentBookingView review = service.bookResultReview(
                patient,
                new BookResultReviewRequest(ordered.getOrderId()));
        assertEquals(0, review.getAmountCents());
        assertTrue(service.getDoctorWorkspace(doctor).getFollowUps().getFirst()
                .isResultReviewBooked());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED,
                () -> service.bookResultReview(
                        patient,
                        new BookResultReviewRequest(ordered.getOrderId())));
        assertEquals(VisitType.RESULT_REVIEW,
                service.listMyAppointments(patient).getAppointments().stream()
                        .filter(item -> item.getAppointmentId()
                                .equals(review.getAppointmentId()))
                        .findFirst()
                        .orElseThrow()
                        .getVisitType());

        DoctorConsultationContextView reviewContext =
                service.getDoctorConsultationContext(
                        doctor,
                        new DoctorConsultationContextRequest(review.getAppointmentId()));
        assertEquals(1, reviewContext.getEpisodeExaminations().size());
        assertEquals(1, reviewContext.getPreviousClinicalRecords().size());
        assertEquals(1, reviewContext.getPreviousClinicalRecords().getFirst()
                .getEpisodeExaminations().size());
        assertEquals(ExaminationStatus.RESULT_READY,
                reviewContext.getEpisodeExaminations().getFirst().getStatus());

        service.submitConsultation(
                doctor,
                new SubmitConsultationRequest(
                        review.getAppointmentId(),
                        "检查结果支持上呼吸道感染（课程演示）",
                        "已回看血常规演示报告。",
                        "继续休息并观察症状。",
                        "无。",
                        "症状加重时重新挂号。"));
        PatientHealthRecordView completed = service.getMyHealthRecord(patient);
        assertEquals(2, completed.getConsultations().size());
        assertEquals(ExaminationStatus.REVIEWED,
                completed.getExaminations().getFirst().getStatus());
        assertTrue(service.getDoctorWorkspace(doctor).getFollowUps().isEmpty());

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                () -> service.publishDemoExaminationReport(
                        patient,
                        new PublishDemoExaminationReportRequest(ordered.getOrderId())));
    }

    @Test
    void resultReviewNoShowKeepsTheReportedEpisodeOpen() {
        InMemoryHospitalRepository repository =
                new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService initialService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo patient = session("U-RESULT-REVIEW-NO-SHOW", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView firstVisit = initialService.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        ExaminationOrderView order = initialService.submitExaminationPlan(
                doctor,
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(), "待查", "血常规", "", ""));
        initialService.publishDemoExaminationReport(
                patient,
                new PublishDemoExaminationReportRequest(order.getOrderId()));
        AppointmentBookingView review = initialService.bookResultReview(
                patient,
                new BookResultReviewRequest(order.getOrderId()));
        HospitalBooking reviewBooking = repository.findBookingById(
                review.getAppointmentId()).orElseThrow();
        HospitalSlot reviewSlot = repository.findSlotById(
                reviewBooking.appointment().scheduleId()).orElseThrow();
        Clock afterReview = Clock.fixed(
                reviewSlot.endTime().plusMinutes(1)
                        .atZone(FIXED_CLOCK.getZone()).toInstant(),
                FIXED_CLOCK.getZone());

        var noShow = new HospitalService(repository, afterReview)
                .markAppointmentNoShow(
                        doctor,
                        new MarkAppointmentNoShowRequest(review.getAppointmentId()));

        assertEquals(AppointmentStatus.NO_SHOW, noShow.getAppointmentStatus());
        assertEquals(EpisodeStatus.RESULT_READY,
                repository.findEpisodeById(order.getEpisodeId()).orElseThrow().status());
        assertEquals(ExaminationStatus.RESULT_READY,
                repository.findExaminationOrderById(order.getOrderId())
                        .orElseThrow().status());
        assertTrue(repository.findConsultationByAppointmentId(
                review.getAppointmentId()).isEmpty());
    }

    @Test
    void resultReviewCanOpenAnotherExaminationAndBookASecondReview() {
        InMemoryHospitalRepository repository = new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService repeatedService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo patient = session("U-REPEATED-EXAM-PATIENT", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);

        AppointmentBookingView firstVisit = repeatedService.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        ExaminationOrderView firstOrder = repeatedService.submitExaminationPlan(
                doctor,
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(), "发热待查", "血常规", "", ""));
        repeatedService.publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(firstOrder.getOrderId()));
        AppointmentBookingView firstReview = repeatedService.bookResultReview(
                patient, new BookResultReviewRequest(firstOrder.getOrderId()));

        ExaminationOrderView secondOrder = repeatedService.submitExaminationPlan(
                doctor,
                new SubmitExaminationPlanRequest(
                        firstReview.getAppointmentId(),
                        "血常规不足以完成判断",
                        "血常规结果已结合症状解读",
                        "胸部影像",
                        "按检查部门要求准备",
                        "如症状加重及时就医"));
        assertEquals("血常规结果已结合症状解读",
                repository.findConsultationByAppointmentId(
                                firstReview.getAppointmentId())
                        .orElseThrow().examinationAdvice());

        PatientHealthRecordView waitingAgain = repeatedService.getMyHealthRecord(patient);
        assertEquals(2, waitingAgain.getExaminations().size());
        assertEquals(ExaminationStatus.REVIEWED, waitingAgain.getExaminations().stream()
                .filter(order -> order.getOrderId().equals(firstOrder.getOrderId()))
                .findFirst().orElseThrow().getStatus());
        assertEquals(ExaminationStatus.ORDERED, waitingAgain.getExaminations().stream()
                .filter(order -> order.getOrderId().equals(secondOrder.getOrderId()))
                .findFirst().orElseThrow().getStatus());
        assertEquals(2, waitingAgain.getConsultations().size());

        ExaminationOrderView secondReport = repeatedService.publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(secondOrder.getOrderId()));
        assertFalse(secondReport.isResultReviewBooked());
        AppointmentBookingView secondReview = repeatedService.bookResultReview(
                patient, new BookResultReviewRequest(secondOrder.getOrderId()));
        assertFalse(firstReview.getAppointmentId().equals(secondReview.getAppointmentId()));
        assertTrue(secondReview.getStartTime().isAfter(firstReview.getStartTime()));

        DoctorConsultationContextView secondReviewContext = repeatedService
                .getDoctorConsultationContext(
                        doctor,
                        new DoctorConsultationContextRequest(
                                secondReview.getAppointmentId()));
        assertEquals(2, secondReviewContext.getEpisodeExaminations().size());
        repeatedService.submitConsultation(
                doctor,
                new SubmitConsultationRequest(
                        secondReview.getAppointmentId(),
                        "结合两轮检查完成诊断",
                        "已解读第二轮检查结果",
                        "完成本轮诊疗",
                        "无",
                        "必要时普通复诊"));
        assertTrue(repeatedService.getDoctorWorkspace(doctor).getFollowUps().isEmpty());
        assertEquals(ExaminationStatus.REVIEWED,
                repository.findExaminationOrderById(secondOrder.getOrderId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.COMPLETED,
                repository.findEpisodeById(secondOrder.getEpisodeId())
                        .orElseThrow().status());
    }

    @Test
    void serializesConcurrentResultReviewBookingsAcrossDifferentSchedules() throws Exception {
        InMemoryHospitalRepository repository =
                new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService setupService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo patient = session("U-CONCURRENT-RESULT-REVIEW", Role.USER);
        AppointmentBookingView firstVisit = setupService.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        ExaminationOrderView order = setupService.submitExaminationPlan(
                session("U-DOCTOR-001", Role.USER),
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(), "待查", "血常规", "", ""));
        setupService.publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(order.getOrderId()));

        CountDownLatch bothReadNoBooking = new CountDownLatch(2);
        AtomicInteger slotSearches = new AtomicInteger();
        HospitalRepository concurrentRepository = (HospitalRepository) Proxy.newProxyInstance(
                HospitalRepository.class.getClassLoader(),
                new Class<?>[]{HospitalRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("findSlots")) {
                        String scheduleId = slotSearches.getAndIncrement() == 0
                                ? "slot-general-1" : "slot-general-3";
                        return List.of(repository.findSlotById(scheduleId).orElseThrow());
                    }
                    if (method.getName().equals("findBookingsByPatientUserId")) {
                        List<HospitalBooking> snapshot = repository
                                .findBookingsByPatientUserId((String) arguments[0]);
                        bothReadNoBooking.countDown();
                        bothReadNoBooking.await(500, TimeUnit.MILLISECONDS);
                        return snapshot;
                    }
                    try {
                        return method.invoke(repository, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });
        HospitalService concurrentService =
                new HospitalService(concurrentRepository, FIXED_CLOCK);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = java.util.stream.IntStream.range(0, 2)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(2, TimeUnit.SECONDS)) {
                            return "START_TIMEOUT";
                        }
                        try {
                            concurrentService.bookResultReview(
                                    patient, new BookResultReviewRequest(order.getOrderId()));
                            return "BOOKED";
                        } catch (HospitalBusinessException exception) {
                            return exception.errorCode();
                        }
                    }))
                    .toList();

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
            List<String> results = futures.stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }).toList();

            assertEquals(1, results.stream().filter("BOOKED"::equals).count());
            assertEquals(1, results.stream()
                    .filter(ErrorCodes.HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED::equals)
                    .count());
            assertEquals(1, repository.findBookingsByPatientUserId(patient.getUserId()).stream()
                    .map(HospitalBooking::appointment)
                    .filter(HospitalAppointment::occupiesSlot)
                    .filter(appointment -> appointment.visitType() == VisitType.RESULT_REVIEW)
                    .filter(appointment -> appointment.episodeId().equals(order.getEpisodeId()))
                    .count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void rechecksSelectedResultReviewScheduleAfterAcquiringItsLock() {
        InMemoryHospitalRepository repository =
                new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService setupService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo patient = session("U-RESULT-REVIEW-CLOSED-RACE", Role.USER);
        AppointmentBookingView firstVisit = setupService.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        ExaminationOrderView order = setupService.submitExaminationPlan(
                session("U-DOCTOR-001", Role.USER),
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(), "待查", "血常规", "", ""));
        setupService.publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(order.getOrderId()));
        HospitalSlot candidate = repository.findSlotById("slot-general-3").orElseThrow();
        HospitalSlot closedCandidate = new HospitalSlot(
                candidate.scheduleId(), candidate.departmentId(), candidate.departmentName(),
                candidate.doctorId(), candidate.doctorName(), candidate.doctorTitle(),
                candidate.startTime(), candidate.endTime(), candidate.priceCents(),
                candidate.capacity(), candidate.bookedCount(), false);
        HospitalRepository changingRepository = (HospitalRepository) Proxy.newProxyInstance(
                HospitalRepository.class.getClassLoader(),
                new Class<?>[]{HospitalRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("findSlots")) {
                        return List.of(candidate);
                    }
                    if (method.getName().equals("findSlotById")
                            && candidate.scheduleId().equals(arguments[0])) {
                        return java.util.Optional.of(closedCandidate);
                    }
                    try {
                        return method.invoke(repository, arguments);
                    } catch (InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                });

        HospitalService changingService = new HospitalService(
                changingRepository, FIXED_CLOCK);

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SCHEDULE_CLOSED,
                () -> changingService.bookResultReview(
                        patient, new BookResultReviewRequest(order.getOrderId())));
    }

    @Test
    void doctorCannotOpenOrCompleteAnotherDoctorsAppointment() {
        AppointmentBookingView booking = service.bookAppointment(
                session("U-CROSS-DOCTOR-PATIENT", Role.USER),
                BookAppointmentRequest.firstVisit("slot-general-1"));
        SessionInfo otherDoctor = session("U-DOCTOR-002", Role.USER);

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> service.getDoctorConsultationContext(
                        otherDoctor,
                        new DoctorConsultationContextRequest(
                                booking.getAppointmentId())));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> service.submitConsultation(
                        otherDoctor,
                        consultationRequest(booking.getAppointmentId())));
    }

    @Test
    void patientCannotUseDoctorConsultationOperations() {
        SessionInfo patient = session("U-STUDENT-001", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        assertBusinessFailure(
                ErrorCodes.AUTH_FORBIDDEN,
                () -> service.getDoctorConsultationContext(
                        patient,
                        new DoctorConsultationContextRequest(
                                booking.getAppointmentId())));
        assertBusinessFailure(
                ErrorCodes.AUTH_FORBIDDEN,
                () -> service.submitConsultation(
                        patient,
                        consultationRequest(booking.getAppointmentId())));
    }

    @Test
    void consultationRequiresDiagnosisAndTreatment() {
        SessionInfo patient = session("U-INVALID-CONSULTATION", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        assertThrows(IllegalArgumentException.class, () -> service.submitConsultation(
                doctor,
                new SubmitConsultationRequest(
                        booking.getAppointmentId(), " ", "", "处置", "", "")));
        assertThrows(IllegalArgumentException.class, () -> service.submitConsultation(
                doctor,
                new SubmitConsultationRequest(
                        booking.getAppointmentId(), "诊断", "", " ", "", "")));
    }

    @Test
    void doctorMarksOnlyAnEndedAssignedAppointmentAsNoShow() {
        InMemoryHospitalRepository repository =
                new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService bookingService = new HospitalService(repository, FIXED_CLOCK);
        SessionInfo patient = session("U-PATIENT-NO-SHOW", Role.USER);
        SessionInfo doctor = session("U-DOCTOR-001", Role.USER);
        AppointmentBookingView booking = bookingService.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        MarkAppointmentNoShowRequest request =
                new MarkAppointmentNoShowRequest(booking.getAppointmentId());

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_NO_SHOW,
                () -> bookingService.markAppointmentNoShow(doctor, request));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> new HospitalService(
                        repository,
                        Clock.offset(FIXED_CLOCK, Duration.ofDays(2)))
                        .markAppointmentNoShow(
                                session("U-DOCTOR-002", Role.USER), request));

        HospitalService laterService = new HospitalService(
                repository,
                Clock.offset(FIXED_CLOCK, Duration.ofDays(2)));
        var noShow = laterService.markAppointmentNoShow(doctor, request);

        assertEquals(AppointmentStatus.NO_SHOW, noShow.getAppointmentStatus());
        HospitalBooking stored = repository.findBookingById(booking.getAppointmentId())
                .orElseThrow();
        assertEquals(AppointmentStatus.NO_SHOW, stored.appointment().status());
        assertEquals(PaymentStatus.PAID, stored.bill().paymentStatus());
        assertTrue(stored.appointment().occupiesSlot());
        assertTrue(repository.findConsultationByAppointmentId(
                booking.getAppointmentId()).isEmpty());
        assertEquals(EpisodeStatus.CANCELLED,
                repository.findEpisodeById(stored.appointment().episodeId())
                        .orElseThrow().status());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_NO_SHOW,
                () -> laterService.markAppointmentNoShow(doctor, request));
        assertEquals(AppointmentStatus.NO_SHOW,
                laterService.listMyAppointments(patient).getAppointments()
                        .getFirst().getAppointmentStatus());
    }

    @Test
    void cancelsOwnedAppointmentRefundsPaymentAndReleasesTheSlot() {
        SessionInfo patient = session("U-PATIENT-CANCEL-1", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        assertEquals(7, service.searchSlots(
                patient,
                SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"))
                .getSlots().getFirst().getRemaining());

        var cancelled = service.cancelAppointment(
                patient,
                new CancelAppointmentRequest(booking.getAppointmentId()));

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getAppointmentStatus());
        assertEquals(PaymentStatus.REFUNDED, cancelled.getPaymentStatus());
        var slotAfterCancellation = service.searchSlots(
                patient,
                SearchSlotsRequest.firstVisit("dept-general", "doctor-chen"))
                .getSlots().getFirst();
        assertEquals(8, slotAfterCancellation.getRemaining());
        assertFalse(slotAfterCancellation.isBookedByCurrentUser());
        assertEquals(
                AppointmentStatus.CANCELLED,
                service.listMyAppointments(patient).getAppointments()
                        .getFirst().getAppointmentStatus());
    }

    @Test
    void rejectsUnknownForeignRepeatedAndStartedCancellations() {
        SessionInfo owner = session("U-PATIENT-CANCEL-2", Role.USER);
        SessionInfo otherPatient = session("U-PATIENT-CANCEL-3", Role.USER);
        AppointmentBookingView booking = service.bookAppointment(
                owner,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> service.cancelAppointment(
                        otherPatient,
                        new CancelAppointmentRequest(booking.getAppointmentId())));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> service.cancelAppointment(
                        owner,
                        new CancelAppointmentRequest("appointment-missing")));

        service.cancelAppointment(
                owner,
                new CancelAppointmentRequest(booking.getAppointmentId()));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CANCELLABLE,
                () -> service.cancelAppointment(
                        owner,
                        new CancelAppointmentRequest(booking.getAppointmentId())));

        InMemoryHospitalRepository repository =
                new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService bookingService = new HospitalService(repository, FIXED_CLOCK);
        AppointmentBookingView startedBooking = bookingService.bookAppointment(
                owner,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        HospitalService laterService = new HospitalService(
                repository,
                Clock.offset(FIXED_CLOCK, Duration.ofDays(2)));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_STARTED,
                () -> laterService.cancelAppointment(
                        owner,
                        new CancelAppointmentRequest(startedBooking.getAppointmentId())));
    }

    @Test
    void rejectsDuplicateFullClosedAndUnknownScheduleBookings() {
        SessionInfo patient = session("U-PATIENT-002", Role.USER);
        service.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_DUPLICATE_APPOINTMENT,
                () -> service.bookAppointment(
                        patient,
                        BookAppointmentRequest.firstVisit("slot-general-1")));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SLOT_FULL,
                () -> service.bookAppointment(
                        patient,
                        BookAppointmentRequest.firstVisit("slot-psychology-2")));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SCHEDULE_CLOSED,
                () -> service.bookAppointment(
                        patient,
                        BookAppointmentRequest.firstVisit("slot-eye-closed")));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SCHEDULE_NOT_FOUND,
                () -> service.bookAppointment(
                        patient,
                        BookAppointmentRequest.firstVisit("slot-missing")));
    }

    @Test
    void rejectsBookingAfterTheScheduleHasStarted() {
        InMemoryHospitalRepository repository =
                new InMemoryHospitalRepository(FIXED_CLOCK);
        HospitalService laterService = new HospitalService(
                repository,
                Clock.offset(FIXED_CLOCK, Duration.ofDays(2)));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SCHEDULE_STARTED,
                () -> laterService.bookAppointment(
                        session("U-PATIENT-LATE", Role.USER),
                        BookAppointmentRequest.firstVisit("slot-general-1")));
    }

    @Test
    void serializesConcurrentBookingsForTheSameSchedule() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> attempts = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(index -> (Callable<String>) () -> {
                        try {
                            AppointmentBookingView booking = service.bookAppointment(
                                    session("U-CONCURRENT-" + index, Role.USER),
                                    BookAppointmentRequest.firstVisit("slot-general-3"));
                            return "QUEUE-" + booking.getQueueNumber();
                        } catch (HospitalBusinessException exception) {
                            return exception.errorCode();
                        }
                    })
                    .toList();

            List<String> results = executor.invokeAll(attempts).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            Set<String> successfulQueues = new HashSet<>(results.stream()
                    .filter(result -> result.startsWith("QUEUE-"))
                    .toList());
            assertEquals(
                    Set.of("QUEUE-3", "QUEUE-4", "QUEUE-5", "QUEUE-6", "QUEUE-7", "QUEUE-8"),
                    successfulQueues);
            assertEquals(2, results.stream()
                    .filter(ErrorCodes.HOSPITAL_SLOT_FULL::equals)
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void allowsOnlyOnePatientToTakeTheFinalRemainingSlot() throws Exception {
        for (int index = 0; index < 5; index++) {
            service.bookAppointment(
                    session("U-PREFILL-" + index, Role.USER),
                    BookAppointmentRequest.firstVisit("slot-general-3"));
        }
        assertEquals(1, service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-general", "doctor-liu"))
                .getSlots().stream()
                .filter(slot -> slot.getScheduleId().equals("slot-general-3"))
                .findFirst()
                .orElseThrow()
                .getRemaining());

        int contenders = 4;
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<String>> futures = java.util.stream.IntStream
                    .range(0, contenders)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(2, TimeUnit.SECONDS)) {
                            return "START_TIMEOUT";
                        }
                        try {
                            AppointmentBookingView booking = service.bookAppointment(
                                    session("U-FINAL-SLOT-" + index, Role.USER),
                                    BookAppointmentRequest.firstVisit("slot-general-3"));
                            return "QUEUE-" + booking.getQueueNumber();
                        } catch (HospitalBusinessException exception) {
                            return exception.errorCode();
                        }
                    }))
                    .toList();

            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
            List<String> results = futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();

            assertEquals(1, results.stream().filter("QUEUE-8"::equals).count());
            assertEquals(3, results.stream()
                    .filter(ErrorCodes.HOSPITAL_SLOT_FULL::equals)
                    .count());
            assertEquals(0, service.searchSlots(
                    SearchSlotsRequest.firstVisit("dept-general", "doctor-liu"))
                    .getSlots().stream()
                    .filter(slot -> slot.getScheduleId().equals("slot-general-3"))
                    .findFirst()
                    .orElseThrow()
                    .getRemaining());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private static void assertBusinessFailure(String expectedCode, Runnable action) {
        HospitalBusinessException exception = assertThrows(
                HospitalBusinessException.class, action::run);
        assertEquals(expectedCode, exception.errorCode());
    }

    private static SubmitConsultationRequest consultationRequest(String appointmentId) {
        return new SubmitConsultationRequest(
                appointmentId,
                "上呼吸道感染（课程演示）",
                "如症状持续，建议完善血常规检查。",
                "对症处置，注意休息与补水。",
                "课程演示用药建议，请以真实医嘱为准。",
                "3天后未缓解时复诊。");
    }

    private static SessionInfo session(String userId, Role role) {
        return new SessionInfo("token-" + userId, userId, "demo", "演示用户", role);
    }
    private static SessionInfo session(String userId, Role role, Set<AdminScope> scopes) {
        return new SessionInfo(
                "token-" + userId, userId, "demo", "演示用户", role, scopes);
    }
}
