package edu.seu.vcampus.client.module.hospital;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.AppointmentListResponse;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.AppointmentView;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextView;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.PatientHealthRecordView;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Request;
import edu.seu.vcampus.common.protocol.Response;
import io.github.spannm.jackcess.DatabaseBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/** Exercises production ServerMain in separate JVMs, with a disposable Access copy. */
class HospitalServerRestartIntegrationTest {
    @TempDir
    Path temporaryDirectory;

    private final Set<Long> processIds = new HashSet<>();

    @Test
    void clinicalWorkflowSurvivesActualServerProcessRestarts() throws Exception {
        Path database = temporaryDirectory.resolve("hospital-restart.accdb");
        String sourceSetting = System.getProperty("hospital.restart.source");
        Path source = sourceSetting == null ? null : Path.of(sourceSetting).toAbsolutePath();
        byte[] sourceDigest = source == null ? null : digest(source);
        try {
            if (source != null) {
                Files.copy(source, database);
                assertArrayEquals(sourceDigest, digest(database), "Database copy must be exact");
            } else {
                try (HospitalServerProcess ignored = start(database, "bootstrap")) {
                    // Produce a portable baseline when no local database copy was supplied.
                }
            }
            addTestSchedule(database);
            exerciseRestarts(database);
        } finally {
            if (source != null) {
                assertArrayEquals(sourceDigest, digest(source), "Source database must remain unchanged");
            }
        }
    }

    private void exerciseRestarts(Path database) throws Exception {
        String appointmentId;
        String oldToken;
        ExaminationOrderView order;
        // A doctor account acts as a patient, but books another doctor's schedule.
        try (HospitalServerProcess server = start(database, "1-plan")) {
            ClientContext patient = login(server, "20260029");
            ClientContext treatingDoctor = login(server, "20260006");
            ClientContext outsider = login(server, "20260005");
            oldToken = patient.currentSession().orElseThrow().getToken();
            AppointmentBookingView booking = send(patient, HospitalActions.BOOK_APPOINTMENT,
                    BookAppointmentRequest.firstVisit("restart-test-schedule"), AppointmentBookingView.class);
            appointmentId = booking.getAppointmentId();
            assertEquals(1200, booking.getAmountCents());
            assertRejected(outsider.send(HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                    new DoctorConsultationContextRequest(appointmentId)),
                    ErrorCodes.AUTH_FORBIDDEN);
            order = send(treatingDoctor, HospitalActions.SUBMIT_EXAMINATION_PLAN,
                    new SubmitExaminationPlanRequest(appointmentId, "发热待查（重启验收）",
                            "血常规（重启验收）", "无需空腹（演示）", "检查期间留意体温。"),
                    ExaminationOrderView.class);
            assertEquals(ExaminationStatus.ORDERED, order.getStatus());
        }
        assertDiskStatus(database, "tblHospitalEpisode", "episodeId", order.getEpisodeId(), "WAITING_FOR_RESULTS");

        String summary;
        try (HospitalServerProcess server = start(database, "2-report")) {
            Response staleSession = new CampusClient("127.0.0.1", server.port()).send(
                    Request.create(HospitalActions.GET_MY_HEALTH_RECORD, oldToken, null));
            assertRejected(staleSession, ErrorCodes.AUTH_REQUIRED);
            ClientContext patient = login(server, "20260029");
            PatientHealthRecordView record = healthRecord(patient);
            ExaminationOrderView waiting = findOrder(record, order.getOrderId());
            assertEquals(ExaminationStatus.ORDERED, waiting.getStatus());
            assertNull(waiting.getReportedAt());
            ConsultationRecordView stage = findConsultation(record, appointmentId);
            assertEquals(ConsultationOutcome.WAITING_FOR_RESULTS, stage.getOutcome());
            assertEquals("检查期间留意体温。", stage.getTreatmentAdvice());
            assertEquals(AppointmentStatus.COMPLETED, appointment(patient, appointmentId).getAppointmentStatus());
            ExaminationOrderView reported = send(patient, HospitalActions.PUBLISH_DEMO_EXAMINATION_REPORT,
                    new PublishDemoExaminationReportRequest(order.getOrderId()), ExaminationOrderView.class);
            assertEquals(ExaminationStatus.RESULT_READY, reported.getStatus());
            summary = reported.getResultSummary();
            assertTrue(summary.contains("课程流程演示"));
        }
        assertDiskStatus(database, "tblHospitalEpisode", "episodeId", order.getEpisodeId(), "RESULT_READY");

        String reviewId;
        try (HospitalServerProcess server = start(database, "3-book-review")) {
            ClientContext patient = login(server, "20260029");
            ClientContext other = login(server, "20260006");
            ExaminationOrderView persisted = findOrder(healthRecord(patient), order.getOrderId());
            assertEquals(summary, persisted.getResultSummary());
            assertEquals(ExaminationStatus.RESULT_READY, persisted.getStatus());
            assertFalse(healthRecord(other).getExaminations().stream()
                    .anyMatch(item -> item.getOrderId().equals(order.getOrderId())));
            assertFalse(healthRecord(other).getConsultations().stream()
                    .anyMatch(item -> item.getAppointmentId().equals(appointmentId)));
            assertRejected(other.send(HospitalActions.PUBLISH_DEMO_EXAMINATION_REPORT,
                    new PublishDemoExaminationReportRequest(order.getOrderId())),
                    ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND);
            assertRejected(other.send(HospitalActions.BOOK_RESULT_REVIEW,
                    new BookResultReviewRequest(order.getOrderId())),
                    ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND);
            AppointmentBookingView review = send(patient, HospitalActions.BOOK_RESULT_REVIEW,
                    new BookResultReviewRequest(order.getOrderId()), AppointmentBookingView.class);
            reviewId = review.getAppointmentId();
            assertEquals(0, review.getAmountCents());
        }

        try (HospitalServerProcess server = start(database, "4-complete-review")) {
            ClientContext patient = login(server, "20260029");
            ClientContext treatingDoctor = login(server, "20260006");
            ClientContext outsider = login(server, "20260005");
            AppointmentView review = appointment(patient, reviewId);
            assertEquals(VisitType.RESULT_REVIEW, review.getVisitType());
            assertEquals(AppointmentStatus.BOOKED, review.getAppointmentStatus());
            assertEquals(0, review.getAmountCents());
            assertTrue(findOrder(healthRecord(patient), order.getOrderId()).isResultReviewBooked());
            assertRejected(patient.send(HospitalActions.BOOK_RESULT_REVIEW,
                    new BookResultReviewRequest(order.getOrderId())),
                    ErrorCodes.HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED);
            assertRejected(outsider.send(HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                    new DoctorConsultationContextRequest(reviewId)), ErrorCodes.AUTH_FORBIDDEN);
            DoctorConsultationContextView context = send(treatingDoctor,
                    HospitalActions.GET_DOCTOR_CONSULTATION_CONTEXT,
                    new DoctorConsultationContextRequest(reviewId), DoctorConsultationContextView.class);
            assertEquals(summary, context.getEpisodeExaminations().stream()
                    .filter(item -> item.getOrderId().equals(order.getOrderId())).findFirst()
                    .orElseThrow().getResultSummary());
            send(treatingDoctor, HospitalActions.SUBMIT_CONSULTATION,
                    new SubmitConsultationRequest(reviewId, "结合检查完成判断（重启验收）", "已查看演示报告。",
                            "休息并观察症状（重启验收）。", "", "症状加重时重新挂号。"),
                    ConsultationRecordView.class);
        }
        assertDiskStatus(database, "tblHospitalEpisode", "episodeId", order.getEpisodeId(), "COMPLETED");
        assertDiskStatus(database, "tblHospitalExaminationOrder", "orderId", order.getOrderId(), "REVIEWED");

        try (HospitalServerProcess server = start(database, "5-history")) {
            ClientContext patient = login(server, "20260029");
            PatientHealthRecordView record = healthRecord(patient);
            assertEquals(ConsultationOutcome.WAITING_FOR_RESULTS,
                    findConsultation(record, appointmentId).getOutcome());
            assertEquals(ConsultationOutcome.COMPLETED, findConsultation(record, reviewId).getOutcome());
            assertEquals("休息并观察症状（重启验收）。", findConsultation(record, reviewId).getTreatmentAdvice());
            assertEquals(ExaminationStatus.REVIEWED, findOrder(record, order.getOrderId()).getStatus());
            assertEquals(summary, findOrder(record, order.getOrderId()).getResultSummary());
            assertEquals(AppointmentStatus.COMPLETED, appointment(patient, reviewId).getAppointmentStatus());
            assertEquals(0, appointment(patient, reviewId).getAmountCents());
            assertEquals(PaymentStatus.PAID, appointment(patient, appointmentId).getPaymentStatus());
            assertEquals(1200, appointment(patient, appointmentId).getAmountCents());
        }
    }

    private HospitalServerProcess start(Path database, String phase) throws Exception {
        HospitalServerProcess process = HospitalServerProcess.start(database,
                temporaryDirectory.resolve(phase + ".log"));
        if (!processIds.add(process.pid())) {
            process.close();
            fail("Restart must create a distinct server process");
        }
        return process;
    }

    private static ClientContext login(HospitalServerProcess server, String username) throws Exception {
        ClientContext client = new ClientContext(new CampusClient("127.0.0.1", server.port()));
        Response response = client.login(username, "123456".toCharArray());
        assertTrue(response.isSuccess(), response.getCode());
        return client;
    }

    private static <T> T send(ClientContext client, String action, Serializable request, Class<T> type)
            throws Exception {
        Response response = client.send(action, request);
        assertTrue(response.isSuccess(), action + ": " + response.getCode());
        return assertInstanceOf(type, response.getData());
    }

    private static void assertRejected(Response response, String code) {
        assertFalse(response.isSuccess());
        assertEquals(code, response.getCode());
    }

    private static PatientHealthRecordView healthRecord(ClientContext client) throws Exception {
        return send(client, HospitalActions.GET_MY_HEALTH_RECORD, null, PatientHealthRecordView.class);
    }

    private static ExaminationOrderView findOrder(PatientHealthRecordView record, String orderId) {
        return record.getExaminations().stream().filter(item -> item.getOrderId().equals(orderId))
                .findFirst().orElseThrow();
    }

    private static ConsultationRecordView findConsultation(PatientHealthRecordView record, String appointmentId) {
        return record.getConsultations().stream().filter(item -> item.getAppointmentId().equals(appointmentId))
                .findFirst().orElseThrow();
    }

    private static AppointmentView appointment(ClientContext client, String appointmentId) throws Exception {
        return send(client, HospitalActions.SEARCH_APPOINTMENTS, null, AppointmentListResponse.class)
                .getAppointments().stream().filter(item -> item.getAppointmentId().equals(appointmentId))
                .findFirst().orElseThrow();
    }

    private static void assertDiskStatus(Path database, String table, String key, String id, String status)
            throws Exception {
        try (var disk = new DatabaseBuilder(database).withReadOnly(true).open()) {
            for (var row : disk.getTable(table)) {
                if (id.equals(row.get(key))) {
                    assertEquals(status, row.get("status"));
                    return;
                }
            }
            fail("Expected persisted clinical row in " + table);
        }
    }

    private static void addTestSchedule(Path database) throws Exception {
        // Modify only the disposable copy while its server is stopped.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.plusDays(1).withHour(11).withMinute(17).withSecond(0).withNano(0);
        try (var disk = new DatabaseBuilder(database).open()) {
            boolean otherDoctorBound = false;
            for (var doctor : disk.getTable("tblHospitalDoctor")) {
                if ("U-STUDENT-001".equals(doctor.get("userId"))) {
                    otherDoctorBound = true;
                }
            }
            if (!otherDoctorBound) {
                disk.getTable("tblHospitalDoctor").addRowFromMap(new HashMap<>(Map.of(
                        "doctorId", "restart-test-other-doctor", "userId", "U-STUDENT-001",
                        "departmentId", "dept-general", "doctorName", "非所属测试医生",
                        "doctorTitle", "主治医师", "active", true,
                        "createdAt", now, "updatedAt", now)));
            }
            disk.getTable("tblHospitalSchedule").addRowFromMap(new HashMap<>(Map.ofEntries(
                    Map.entry("scheduleId", "restart-test-schedule"),
                    Map.entry("departmentId", "dept-general"),
                    Map.entry("doctorId", "restart-test-other-doctor"),
                    Map.entry("startTime", start), Map.entry("endTime", start.plusMinutes(30)),
                    Map.entry("registrationFeeCents", 1200), Map.entry("capacity", 12),
                    Map.entry("status", "PUBLISHED"), Map.entry("createdAt", now), Map.entry("updatedAt", now))));
        }
    }

    private static byte[] digest(Path path) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
    }
}
