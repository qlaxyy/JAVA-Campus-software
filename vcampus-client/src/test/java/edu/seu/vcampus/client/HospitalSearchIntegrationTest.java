package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.DoctorWorkspaceView;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.AppointmentListResponse;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.CreateScheduleRequest;
import edu.seu.vcampus.common.hospital.AdminScheduleWorkspaceView;
import edu.seu.vcampus.common.hospital.ConsultationListResponse;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SetSchedulePublicationRequest;
import edu.seu.vcampus.common.hospital.PatientBillListResponse;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalSearchIntegrationTest {

    @Test
    void patientReadsAndPaysDoctorGeneratedFeeThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class,
                    context.send(
                            HospitalActions.BOOK_APPOINTMENT,
                            BookAppointmentRequest.firstVisit("slot-general-1"))
                            .getData());

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260002", "123456".toCharArray()).isSuccess());
            assertTrue(context.send(
                    HospitalActions.SUBMIT_CONSULTATION,
                    new SubmitConsultationRequest(
                            booking.getAppointmentId(),
                            "课程演示诊断", "", "课程演示处置", "无", "必要时复诊"))
                    .isSuccess());

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            PatientBillListResponse bills = assertInstanceOf(
                    PatientBillListResponse.class,
                    context.send(HospitalActions.LIST_MY_BILLS, null).getData());
            var unpaid = bills.getBills().stream()
                    .filter(bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID)
                    .findFirst()
                    .orElseThrow();
            assertEquals(1_800, unpaid.getAmountCents());
            Response payment = context.send(
                    HospitalActions.PAY_BILL,
                    new PayHospitalBillRequest(unpaid.getBillId()));
            assertTrue(payment.isSuccess());
            assertEquals(PaymentStatus.PAID,
                    assertInstanceOf(
                            edu.seu.vcampus.common.hospital.PatientBillView.class,
                            payment.getData()).getPaymentStatus());
        }
    }

    @Test
    void administratorPublishesScheduleThroughSocketForPatientAndDoctor() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));
            LocalDateTime start = LocalDateTime.now()
                    .plusDays(5).withHour(16).withMinute(0).withSecond(0).withNano(0);

            assertTrue(context.login("20260007", "123456".toCharArray()).isSuccess());
            Response workspaceResponse = context.send(
                    HospitalActions.GET_ADMIN_SCHEDULE_WORKSPACE, null);
            AdminScheduleWorkspaceView workspace = assertInstanceOf(
                    AdminScheduleWorkspaceView.class, workspaceResponse.getData());
            assertTrue(workspace.getDoctors().stream()
                    .anyMatch(doctor -> doctor.getDoctorId().equals("doctor-chen")));

            Response createResponse = context.send(
                    HospitalActions.CREATE_SCHEDULE,
                    new CreateScheduleRequest(
                            "doctor-chen", "dept-general",
                            start, start.plusMinutes(30), 1_500, 6));
            var draft = assertInstanceOf(
                    edu.seu.vcampus.common.hospital.SlotView.class,
                    createResponse.getData());
            assertEquals(
                    edu.seu.vcampus.common.hospital.SlotAvailability.CLOSED,
                    draft.getAvailability());
            assertTrue(context.send(
                    HospitalActions.SET_SCHEDULE_PUBLICATION,
                    new SetSchedulePublicationRequest(
                            draft.getScheduleId(), true)).isSuccess());

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());
            SlotListResponse patientSlots = assertInstanceOf(
                    SlotListResponse.class,
                    context.send(
                            HospitalActions.SEARCH_SLOTS,
                            SearchSlotsRequest.firstVisit(
                                    "dept-general", "doctor-chen"))
                            .getData());
            assertTrue(patientSlots.getSlots().stream()
                    .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260002", "123456".toCharArray()).isSuccess());
            DoctorWorkspaceView doctorWorkspace = assertInstanceOf(
                    DoctorWorkspaceView.class,
                    context.send(HospitalActions.GET_DOCTOR_WORKSPACE, null).getData());
            assertTrue(doctorWorkspace.getSchedules().stream()
                    .anyMatch(slot -> slot.getScheduleId().equals(draft.getScheduleId())));
        }
    }

    @Test
    void serverCalculatesPatientDoctorAndAdminModeAccess() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            assertTrue(context.login(
                    "20260002", "123456".toCharArray()).isSuccess());
            Response doctorResponse = context.send(HospitalActions.GET_MODE_ACCESS, null);
            HospitalModeAccessView doctorAccess = assertInstanceOf(
                    HospitalModeAccessView.class, doctorResponse.getData());
            assertTrue(doctorAccess.canAccess(HospitalMode.PATIENT));
            assertTrue(doctorAccess.canAccess(HospitalMode.DOCTOR));
            assertFalse(doctorAccess.canAccess(HospitalMode.ADMIN));

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260007", "123456".toCharArray()).isSuccess());
            Response adminResponse = context.send(HospitalActions.GET_MODE_ACCESS, null);
            HospitalModeAccessView adminAccess = assertInstanceOf(
                    HospitalModeAccessView.class, adminResponse.getData());
            assertTrue(adminAccess.canAccess(HospitalMode.PATIENT));
            assertFalse(adminAccess.canAccess(HospitalMode.DOCTOR));
            assertTrue(adminAccess.canAccess(HospitalMode.ADMIN));
        }
    }

    @Test
    void loggedInStudentCanSearchHospitalSlotsThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response beforeLogin = context.send(HospitalActions.LIST_DEPARTMENTS, null);
            assertFalse(beforeLogin.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED, beforeLogin.getCode());
            Response appointmentsBeforeLogin = context.send(
                    HospitalActions.SEARCH_APPOINTMENTS, null);
            assertFalse(appointmentsBeforeLogin.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED, appointmentsBeforeLogin.getCode());

            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());

            Response departmentsResponse = context.send(
                    HospitalActions.LIST_DEPARTMENTS, null);
            assertTrue(departmentsResponse.isSuccess());
            DepartmentListResponse departments = assertInstanceOf(
                    DepartmentListResponse.class, departmentsResponse.getData());
            assertEquals(15, departments.getDepartments().size());
            assertTrue(departments.getDepartments().stream()
                    .anyMatch(department -> department.getDepartmentId()
                            .equals("dept-joint-surgery")
                            && department.isBookable()
                            && "dept-orthopedics".equals(
                            department.getParentDepartmentId())));

            Response slotsResponse = context.send(
                    HospitalActions.SEARCH_SLOTS,
                    SearchSlotsRequest.firstVisit("dept-psychology", null));
            assertTrue(slotsResponse.isSuccess());
            SlotListResponse slots = assertInstanceOf(
                    SlotListResponse.class, slotsResponse.getData());
            assertFalse(slots.getSlots().isEmpty());

            String scheduleId = slots.getSlots().getFirst().getScheduleId();
            Response bookingResponse = context.send(
                    HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit(scheduleId));
            assertTrue(bookingResponse.isSuccess());
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class, bookingResponse.getData());
            assertEquals(3, booking.getQueueNumber());
            assertEquals(2_000, booking.getAmountCents());

            Response appointmentsResponse = context.send(
                    HospitalActions.SEARCH_APPOINTMENTS, null);
            assertTrue(appointmentsResponse.isSuccess());
            AppointmentListResponse appointments = assertInstanceOf(
                    AppointmentListResponse.class, appointmentsResponse.getData());
            assertEquals(1, appointments.getAppointments().size());
            assertEquals(
                    booking.getAppointmentId(),
                    appointments.getAppointments().getFirst().getAppointmentId());

            Response duplicateBooking = context.send(
                    HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit(scheduleId));
            assertFalse(duplicateBooking.isSuccess());
            assertEquals(
                    ErrorCodes.HOSPITAL_DUPLICATE_APPOINTMENT,
                    duplicateBooking.getCode());

            Response invalidDepartment = context.send(
                    HospitalActions.SEARCH_SLOTS,
                    SearchSlotsRequest.firstVisit("dept-missing", null));
            assertFalse(invalidDepartment.isSuccess());
            assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, invalidDepartment.getCode());

            Response cancellationResponse = context.send(
                    HospitalActions.CANCEL_APPOINTMENT,
                    new CancelAppointmentRequest(booking.getAppointmentId()));
            assertTrue(cancellationResponse.isSuccess());
            var cancellation = assertInstanceOf(
                    edu.seu.vcampus.common.hospital.AppointmentView.class,
                    cancellationResponse.getData());
            assertEquals(
                    edu.seu.vcampus.common.hospital.AppointmentStatus.CANCELLED,
                    cancellation.getAppointmentStatus());
            assertEquals(
                    edu.seu.vcampus.common.hospital.PaymentStatus.REFUNDED,
                    cancellation.getPaymentStatus());

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260002", "123456".toCharArray()).isSuccess());
            Response otherPatientAppointments = context.send(
                    HospitalActions.SEARCH_APPOINTMENTS, null);
            AppointmentListResponse otherList = assertInstanceOf(
                    AppointmentListResponse.class, otherPatientAppointments.getData());
            assertTrue(otherList.getAppointments().isEmpty());
            Response foreignCancellation = context.send(
                    HospitalActions.CANCEL_APPOINTMENT,
                    new CancelAppointmentRequest(booking.getAppointmentId()));
            assertFalse(foreignCancellation.isSuccess());
            assertEquals(
                    ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                    foreignCancellation.getCode());
        }
    }

    @Test
    void doctorCompletesAppointmentAndPatientReadsRecordThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response beforeLogin = context.send(
                    HospitalActions.GET_DOCTOR_WORKSPACE, null);
            assertFalse(beforeLogin.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED, beforeLogin.getCode());

            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());
            Response studentAttempt = context.send(
                    HospitalActions.GET_DOCTOR_WORKSPACE, null);
            assertFalse(studentAttempt.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, studentAttempt.getCode());

            Response bookingResponse = context.send(
                    HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit("slot-general-1"));
            AppointmentBookingView booking = assertInstanceOf(
                    AppointmentBookingView.class, bookingResponse.getData());

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260002", "123456".toCharArray()).isSuccess());
            Response workspaceResponse = context.send(
                    HospitalActions.GET_DOCTOR_WORKSPACE, null);
            assertTrue(workspaceResponse.isSuccess());
            DoctorWorkspaceView workspace = assertInstanceOf(
                    DoctorWorkspaceView.class, workspaceResponse.getData());
            assertEquals("doctor-chen", workspace.getDoctorId());
            assertEquals(3, workspace.getSchedules().size());
            var pending = workspace.getSchedules().getFirst()
                    .getPendingAppointments().stream()
                    .filter(appointment -> appointment.getAppointmentId()
                            .equals(booking.getAppointmentId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(booking.getAppointmentId(), pending.getAppointmentId());
            assertEquals("U-STUDENT-001", pending.getPatientUserId());

            Response contextResponse = context.send(
                    HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                    new DoctorConsultationContextRequest(booking.getAppointmentId()));
            assertTrue(contextResponse.isSuccess());
            DoctorConsultationContextView consultationContext = assertInstanceOf(
                    DoctorConsultationContextView.class, contextResponse.getData());
            assertEquals("U-STUDENT-001",
                    consultationContext.getAppointment().getPatientUserId());
            assertEquals("O型（演示）",
                    consultationContext.getHealthProfile().getBloodType());

            Response completionResponse = context.send(
                    HospitalActions.SUBMIT_CONSULTATION,
                    new SubmitConsultationRequest(
                            booking.getAppointmentId(),
                            "上呼吸道感染（课程演示）",
                            "建议检查血常规。",
                            "对症处置并休息。",
                            "无。",
                            "3天后未缓解时复诊。"));
            assertTrue(completionResponse.isSuccess());
            ConsultationRecordView completed = assertInstanceOf(
                    ConsultationRecordView.class, completionResponse.getData());
            assertEquals(booking.getAppointmentId(), completed.getAppointmentId());

            Response workspaceAfterCompletion = context.send(
                    HospitalActions.GET_DOCTOR_WORKSPACE, null);
            DoctorWorkspaceView refreshedWorkspace = assertInstanceOf(
                    DoctorWorkspaceView.class, workspaceAfterCompletion.getData());
            assertTrue(refreshedWorkspace.getSchedules().getFirst()
                    .getPendingAppointments().stream()
                    .noneMatch(appointment -> appointment.getAppointmentId()
                            .equals(booking.getAppointmentId())));

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login(
                    "20260001", "123456".toCharArray()).isSuccess());
            Response recordsResponse = context.send(
                    HospitalActions.LIST_MY_CONSULTATIONS, null);
            assertTrue(recordsResponse.isSuccess());
            ConsultationListResponse records = assertInstanceOf(
                    ConsultationListResponse.class, recordsResponse.getData());
            assertEquals(1, records.getConsultations().size());
            assertEquals(completed.getConsultationId(),
                    records.getConsultations().getFirst().getConsultationId());
            assertEquals("U-STUDENT-001",
                    records.getConsultations().getFirst().getPatientUserId());
            Response completedAppointmentsResponse = context.send(
                    HospitalActions.SEARCH_APPOINTMENTS, null);
            AppointmentListResponse completedAppointments = assertInstanceOf(
                    AppointmentListResponse.class,
                    completedAppointmentsResponse.getData());
            assertEquals(AppointmentStatus.COMPLETED,
                    completedAppointments.getAppointments().getFirst()
                            .getAppointmentStatus());
        }
    }
}
