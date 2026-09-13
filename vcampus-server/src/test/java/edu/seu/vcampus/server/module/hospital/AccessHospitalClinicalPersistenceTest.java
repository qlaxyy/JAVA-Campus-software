package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentBookingView;
import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.BookResultReviewRequest;
import edu.seu.vcampus.common.hospital.ConsultationOutcome;
import edu.seu.vcampus.common.hospital.ConsultationRecordView;
import edu.seu.vcampus.common.hospital.DoctorConsultationContextRequest;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.ExaminationOrderView;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.MarkAppointmentNoShowRequest;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.PublishDemoExaminationReportRequest;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.SubmitExaminationPlanRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import io.github.spannm.jackcess.DatabaseBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessHospitalClinicalPersistenceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 4, 0, 0);
    private static final SessionInfo DOCTOR = session("U-DOCTOR-001");
    private static final SessionInfo OTHER_DOCTOR = session("U-DOCTOR-002");

    @TempDir
    Path temporaryDirectory;

    @Test
    void noShowAppointmentAndEpisodeStateSurviveRepositoryReopen() {
        Path path = temporaryDirectory.resolve("no-show.accdb");
        SessionInfo patient = session("U-ACCESS-NO-SHOW-001");
        HospitalService initialService = service(path);
        AppointmentBookingView booking = initialService.bookAppointment(
                patient,
                BookAppointmentRequest.firstVisit("slot-general-1"));
        HospitalService afterSchedule = new HospitalService(
                repository(path),
                Clock.offset(CLOCK, Duration.ofDays(2)));

        afterSchedule.markAppointmentNoShow(
                DOCTOR,
                new MarkAppointmentNoShowRequest(booking.getAppointmentId()));

        AccessHospitalRepository reopened = repository(path);
        HospitalBooking stored = reopened.findBookingById(
                booking.getAppointmentId()).orElseThrow();
        assertEquals(AppointmentStatus.NO_SHOW, stored.appointment().status());
        assertNull(stored.appointment().cancelledAt());
        assertNull(stored.appointment().completedAt());
        assertEquals(PaymentStatus.PAID, stored.bill().paymentStatus());
        assertEquals(EpisodeStatus.CANCELLED,
                reopened.findEpisodeById(stored.appointment().episodeId())
                        .orElseThrow().status());
        assertTrue(reopened.findConsultationByAppointmentId(
                booking.getAppointmentId()).isEmpty());
        assertEquals(1, reopened.findBillsByPatientUserId(patient.getUserId()).size());
        assertEquals(HospitalBillType.REGISTRATION,
                reopened.findBillsByPatientUserId(patient.getUserId())
                        .getFirst().billType());
    }

    @Test
    void completedConsultationIsReadFromTheReopenedAccessFileWithoutTextLoss()
            throws Exception {
        Path path = temporaryDirectory.resolve("completed-consultation.accdb");
        SessionInfo patient = session("U-ACCESS-COMPLETE-001");
        HospitalService service = service(path);
        AppointmentBookingView booking = service.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        String diagnosis = "持续咳嗽伴咽痛，".repeat(35) + "考虑上呼吸道感染（课程演示）。";
        String treatment = "建议充分休息、补充温水并记录体温；".repeat(22)
                + "若出现呼吸困难应及时就医。";

        ConsultationRecordView written = service.submitConsultation(
                DOCTOR,
                new SubmitConsultationRequest(
                        booking.getAppointmentId(), diagnosis, "", treatment, "", ""));

        AccessHospitalRepository reopenedRepository = repository(path);
        HospitalConsultation stored = reopenedRepository
                .findConsultationByAppointmentId(booking.getAppointmentId())
                .orElseThrow();
        assertEquals(written.getConsultationId(), stored.consultationId());
        assertEquals(diagnosis, stored.diagnosisOpinion());
        assertEquals(treatment, stored.treatmentAdvice());
        assertEquals("", stored.examinationAdvice());
        assertEquals("", stored.medicationAdvice());
        assertEquals("", stored.followUpAdvice());
        assertEquals(ConsultationOutcome.COMPLETED, stored.outcome());
        assertEquals(NOW, stored.createdAt());
        assertEquals(AppointmentStatus.COMPLETED,
                reopenedRepository.findAppointmentById(booking.getAppointmentId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.COMPLETED,
                reopenedRepository.findEpisodeById(
                                reopenedRepository.findAppointmentById(
                                                booking.getAppointmentId())
                                        .orElseThrow().episodeId())
                        .orElseThrow().status());

        HospitalService reopenedService = service(path);
        ConsultationRecordView displayed = reopenedService.listMyConsultations(patient)
                .getConsultations().getFirst();
        assertEquals(diagnosis, displayed.getDiagnosisOpinion());
        assertEquals(treatment, displayed.getTreatmentAdvice());
        assertEquals("无", displayed.getExaminationAdvice());
        assertEquals("无", displayed.getMedicationAdvice());
        assertEquals("无", displayed.getFollowUpAdvice());
        assertEquals(1, rowCount(path, "tblHospitalConsultation"));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_CONSULTABLE,
                () -> reopenedService.submitConsultation(
                        DOCTOR,
                        new SubmitConsultationRequest(
                                booking.getAppointmentId(), "覆盖诊断", "", "覆盖处置", "", "")));
        HospitalConsultation afterDuplicate = repository(path)
                .findConsultationByAppointmentId(booking.getAppointmentId())
                .orElseThrow();
        assertEquals(written.getConsultationId(), afterDuplicate.consultationId());
        assertEquals(diagnosis, afterDuplicate.diagnosisOpinion());
    }

    @Test
    void examinationEpisodeSurvivesEveryRepositoryRecreationAndFinishesReviewed()
            throws Exception {
        Path path = temporaryDirectory.resolve("examination-lifecycle.accdb");
        SessionInfo patient = session("U-ACCESS-EXAM-001");
        HospitalService first = service(path);
        AppointmentBookingView firstVisit = first.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        String preliminaryDiagnosis = "反复低热伴乏力，".repeat(28) + "病因待查（课程演示）。";
        String examinationItem = "血常规与炎症指标联合检查（课程演示）";
        String instructions = "检查前可正常饮水，完成采样后在校园医院系统等待报告；".repeat(18);

        ExaminationOrderView ordered = first.submitExaminationPlan(
                DOCTOR,
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(), preliminaryDiagnosis,
                        examinationItem, instructions, ""));

        AccessHospitalRepository afterPlanRepository = repository(path);
        HospitalExaminationOrder orderedRow = afterPlanRepository
                .findExaminationOrderById(ordered.getOrderId()).orElseThrow();
        HospitalConsultation stageNote = afterPlanRepository
                .findConsultationByAppointmentId(firstVisit.getAppointmentId()).orElseThrow();
        assertEquals(ExaminationStatus.ORDERED, orderedRow.status());
        assertEquals(instructions, orderedRow.instructions());
        assertNull(orderedRow.reviewedAt());
        assertEquals(ConsultationOutcome.WAITING_FOR_RESULTS, stageNote.outcome());
        assertEquals(preliminaryDiagnosis, stageNote.diagnosisOpinion());
        assertEquals("", stageNote.treatmentAdvice());
        assertEquals("", stageNote.medicationAdvice());
        assertFalse(afterPlanRepository.findExaminationReportByOrderId(
                ordered.getOrderId()).isPresent());
        assertEquals(EpisodeStatus.WAITING_FOR_RESULTS,
                afterPlanRepository.findEpisodeById(ordered.getEpisodeId())
                        .orElseThrow().status());
        assertEquals(AppointmentStatus.COMPLETED,
                afterPlanRepository.findAppointmentById(firstVisit.getAppointmentId())
                        .orElseThrow().status());
        ExaminationOrderView waitingView = service(path).getMyHealthRecord(patient)
                .getExaminations().getFirst();
        assertEquals("尚未出具", waitingView.getResultSummary());
        assertNull(waitingView.getReportedAt());

        service(path).publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(ordered.getOrderId()));

        AccessHospitalRepository afterReportRepository = repository(path);
        HospitalExaminationReport report = afterReportRepository
                .findExaminationReportByOrderId(ordered.getOrderId()).orElseThrow();
        String expectedSummary = examinationItem + "演示结果：指标已完成采集，"
                + "请由接诊医生结合病情解读。本内容仅用于课程流程演示。";
        assertEquals(expectedSummary, report.resultSummary());
        assertEquals(NOW, report.reportedAt());
        assertEquals(ExaminationStatus.RESULT_READY,
                afterReportRepository.findExaminationOrderById(ordered.getOrderId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.RESULT_READY,
                afterReportRepository.findEpisodeById(ordered.getEpisodeId())
                        .orElseThrow().status());

        HospitalService afterReportService = service(path);
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_EXAMINATION_STATE_INVALID,
                () -> afterReportService.publishDemoExaminationReport(
                        patient,
                        new PublishDemoExaminationReportRequest(ordered.getOrderId())));
        HospitalExaminationReport afterDuplicateReport = repository(path)
                .findExaminationReportByOrderId(ordered.getOrderId()).orElseThrow();
        assertEquals(report.reportId(), afterDuplicateReport.reportId());
        assertEquals(expectedSummary, afterDuplicateReport.resultSummary());

        AppointmentBookingView review = service(path).bookResultReview(
                patient, new BookResultReviewRequest(ordered.getOrderId()));

        AccessHospitalRepository afterReviewBookingRepository = repository(path);
        HospitalBooking reviewBooking = afterReviewBookingRepository
                .findBookingById(review.getAppointmentId()).orElseThrow();
        assertEquals(0, review.getAmountCents());
        assertEquals(0, reviewBooking.billItem().amountCents());
        assertEquals(VisitType.RESULT_REVIEW, reviewBooking.appointment().visitType());
        assertEquals(firstVisit.getAppointmentId(),
                reviewBooking.appointment().sourceFirstVisitAppointmentId());
        assertEquals(ordered.getEpisodeId(), reviewBooking.appointment().episodeId());
        assertEquals(AppointmentStatus.BOOKED, reviewBooking.appointment().status());
        assertEquals(ExaminationStatus.RESULT_READY,
                afterReviewBookingRepository.findExaminationOrderById(ordered.getOrderId())
                        .orElseThrow().status());

        String finalDiagnosis = "结合检查结果，支持病毒性上呼吸道感染；".repeat(20)
                + "本结论仅用于课程流程演示。";
        String finalTreatment = "继续休息并观察症状，保持规律作息。";
        service(path).submitConsultation(
                DOCTOR,
                new SubmitConsultationRequest(
                        review.getAppointmentId(), finalDiagnosis, "",
                        finalTreatment, "", ""));

        AccessHospitalRepository completedRepository = repository(path);
        HospitalConsultation reviewConsultation = completedRepository
                .findConsultationByAppointmentId(review.getAppointmentId()).orElseThrow();
        HospitalExaminationOrder reviewed = completedRepository
                .findExaminationOrderById(ordered.getOrderId()).orElseThrow();
        HospitalEpisode completedEpisode = completedRepository
                .findEpisodeById(ordered.getEpisodeId()).orElseThrow();
        assertEquals(finalDiagnosis, reviewConsultation.diagnosisOpinion());
        assertEquals(finalTreatment, reviewConsultation.treatmentAdvice());
        assertEquals("", reviewConsultation.examinationAdvice());
        assertEquals("", reviewConsultation.medicationAdvice());
        assertEquals("", reviewConsultation.followUpAdvice());
        assertEquals(ExaminationStatus.REVIEWED, reviewed.status());
        assertEquals(NOW, reviewed.reviewedAt());
        assertEquals(EpisodeStatus.COMPLETED, completedEpisode.status());
        assertEquals(NOW, completedEpisode.completedAt());
        assertEquals(AppointmentStatus.COMPLETED,
                completedRepository.findAppointmentById(review.getAppointmentId())
                        .orElseThrow().status());
        assertEquals(2, service(path).getMyHealthRecord(patient).getConsultations().size());
        assertEquals(ExaminationStatus.REVIEWED,
                service(path).getMyHealthRecord(patient).getExaminations()
                        .getFirst().getStatus());
        assertTrue(service(path).getDoctorWorkspace(DOCTOR).getFollowUps().isEmpty());
        assertEquals(2, rowCount(path, "tblHospitalConsultation"));
        assertEquals(1, rowCount(path, "tblHospitalExaminationOrder"));
        assertEquals(1, rowCount(path, "tblHospitalExaminationReport"));
    }

    @Test
    void repeatedExaminationAndSecondResultReviewSurviveRepositoryRecreation()
            throws Exception {
        Path path = temporaryDirectory.resolve("repeated-examination.accdb");
        SessionInfo patient = session("U-ACCESS-REPEATED-EXAM");
        HospitalService firstService = service(path);
        AppointmentBookingView firstVisit = firstService.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        ExaminationOrderView firstOrder = firstService.submitExaminationPlan(
                DOCTOR,
                new SubmitExaminationPlanRequest(
                        firstVisit.getAppointmentId(), "发热待查", "血常规", "", ""));
        service(path).publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(firstOrder.getOrderId()));
        AppointmentBookingView firstReview = service(path).bookResultReview(
                patient, new BookResultReviewRequest(firstOrder.getOrderId()));

        ExaminationOrderView secondOrder = service(path).submitExaminationPlan(
                DOCTOR,
                new SubmitExaminationPlanRequest(
                        firstReview.getAppointmentId(),
                        "上一轮结果不足以完成判断",
                        "胸部影像",
                        "按检查部门要求准备",
                        "如症状加重及时就医"));

        AccessHospitalRepository afterSecondOrder = repository(path);
        assertEquals(ExaminationStatus.REVIEWED,
                afterSecondOrder.findExaminationOrderById(firstOrder.getOrderId())
                        .orElseThrow().status());
        assertEquals(ExaminationStatus.ORDERED,
                afterSecondOrder.findExaminationOrderById(secondOrder.getOrderId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.WAITING_FOR_RESULTS,
                afterSecondOrder.findEpisodeById(secondOrder.getEpisodeId())
                        .orElseThrow().status());

        service(path).publishDemoExaminationReport(
                patient, new PublishDemoExaminationReportRequest(secondOrder.getOrderId()));
        AppointmentBookingView secondReview = service(path).bookResultReview(
                patient, new BookResultReviewRequest(secondOrder.getOrderId()));
        assertFalse(firstReview.getAppointmentId().equals(secondReview.getAppointmentId()));
        service(path).submitConsultation(
                DOCTOR,
                new SubmitConsultationRequest(
                        secondReview.getAppointmentId(),
                        "结合两轮检查完成诊断",
                        "已解读第二轮检查结果",
                        "完成本轮诊疗",
                        "无",
                        "必要时普通复诊"));

        AccessHospitalRepository completed = repository(path);
        assertEquals(ExaminationStatus.REVIEWED,
                completed.findExaminationOrderById(secondOrder.getOrderId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.COMPLETED,
                completed.findEpisodeById(secondOrder.getEpisodeId())
                        .orElseThrow().status());
        assertEquals(3, rowCount(path, "tblHospitalConsultation"));
        assertEquals(2, rowCount(path, "tblHospitalExaminationOrder"));
        assertEquals(2, rowCount(path, "tblHospitalExaminationReport"));
    }

    @Test
    void serviceHidesForeignClinicalDataButAllowsDoctorAccountsToBookAsPatients() {
        Path path = temporaryDirectory.resolve("clinical-authorization.accdb");
        HospitalService service = service(path);
        SessionInfo owner = session("U-ACCESS-OWNER-001");
        SessionInfo otherPatient = session("U-ACCESS-OTHER-001");
        AppointmentBookingView booking = service.bookAppointment(
                owner, BookAppointmentRequest.firstVisit("slot-general-1"));

        assertTrue(service.getMyHealthRecord(otherPatient).getConsultations().isEmpty());
        assertTrue(service.getMyHealthRecord(otherPatient).getExaminations().isEmpty());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> service.getDoctorConsultationContext(
                        OTHER_DOCTOR,
                        new DoctorConsultationContextRequest(booking.getAppointmentId())));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_APPOINTMENT_NOT_FOUND,
                () -> service.submitExaminationPlan(
                        OTHER_DOCTOR,
                        new SubmitExaminationPlanRequest(
                                booking.getAppointmentId(), "越权诊断", "越权检查", "", "")));
        assertBusinessFailure(
                ErrorCodes.AUTH_FORBIDDEN,
                () -> service.getDoctorConsultationContext(
                        otherPatient,
                        new DoctorConsultationContextRequest(booking.getAppointmentId())));

        ExaminationOrderView order = service.submitExaminationPlan(
                DOCTOR,
                new SubmitExaminationPlanRequest(
                        booking.getAppointmentId(), "待查", "血常规", "", ""));
        HospitalService reopened = service(path);
        assertTrue(reopened.getMyHealthRecord(otherPatient).getConsultations().isEmpty());
        assertTrue(reopened.getMyHealthRecord(otherPatient).getExaminations().isEmpty());
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND,
                () -> reopened.publishDemoExaminationReport(
                        otherPatient,
                        new PublishDemoExaminationReportRequest(order.getOrderId())));
        assertBusinessFailure(
                ErrorCodes.HOSPITAL_EXAMINATION_NOT_FOUND,
                () -> reopened.bookResultReview(
                        otherPatient, new BookResultReviewRequest(order.getOrderId())));

        assertBusinessFailure(
                ErrorCodes.HOSPITAL_SELF_BOOKING_FORBIDDEN,
                () -> reopened.bookAppointment(
                        DOCTOR, BookAppointmentRequest.firstVisit("slot-general-1")));
        AppointmentBookingView doctorAsPatientBooking = reopened.bookAppointment(
                DOCTOR, BookAppointmentRequest.firstVisit("slot-general-3"));
        assertTrue(service(path).listMyAppointments(DOCTOR).getAppointments().stream()
                .anyMatch(item -> item.getAppointmentId()
                        .equals(doctorAsPatientBooking.getAppointmentId())));
    }

    @Test
    void stageCareIsStoredSeparatelyFromFormalTreatmentInTheAccessFile() throws Exception {
        Path path = temporaryDirectory.resolve("stage-care-on-disk.accdb");
        SessionInfo patient = session("U-ACCESS-STAGE-CARE");
        HospitalService service = service(path);
        String appointmentId = service.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1")).getAppointmentId();
        String care = "检查期间适量饮水，留意体温变化（虚构演示）。";
        ExaminationOrderView order = service.submitExaminationPlan(DOCTOR,
                new SubmitExaminationPlanRequest(appointmentId, "病因待查", "血常规", "", care));
        service.publishDemoExaminationReport(patient,
                new PublishDemoExaminationReportRequest(order.getOrderId()));

        // Read the physical file independently of UCanAccess's in-process mirror.
        try (var disk = new DatabaseBuilder(path).withReadOnly(true).open()) {
            var consultation = disk.getTable("tblHospitalConsultation").iterator().next();
            assertEquals(appointmentId, consultation.get("appointmentId"));
            assertEquals("WAITING_FOR_RESULTS", consultation.get("outcome"));
            assertEquals(care, consultation.get("interimCareAdvice"));
            assertNull(consultation.get("treatmentAdvice"));
            assertNull(consultation.get("medicationAdvice"));
            var persistedOrder = disk.getTable("tblHospitalExaminationOrder").iterator().next();
            assertEquals(order.getOrderId(), persistedOrder.get("orderId"));
            assertEquals("RESULT_READY", persistedOrder.get("status"));
            var report = disk.getTable("tblHospitalExaminationReport").iterator().next();
            assertEquals(order.getOrderId(), report.get("orderId"));
            assertEquals("DEMO", report.get("sourceType"));
            assertTrue(report.get("summary").toString().contains("课程流程演示"));
        }
        assertEquals(care, repository(path).findConsultationByAppointmentId(appointmentId)
                .orElseThrow().treatmentAdvice());
    }

    @Test
    void allClinicalWriteMethodsRollBackWritesMadeBeforeALateFailure() throws Exception {
        assertConsultationCompletionRollback();
        assertExaminationPlanRollback();
        assertExaminationReportRollback();
        assertResultReviewCompletionRollback();
    }

    private void assertConsultationCompletionRollback() throws Exception {
        Path path = temporaryDirectory.resolve("rollback-consultation.accdb");
        SessionInfo patient = session("U-ROLLBACK-CONSULT-001");
        AppointmentBookingView view = service(path).bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        AccessHospitalRepository repository = repository(path);
        HospitalBooking current = repository.findBookingById(view.getAppointmentId())
                .orElseThrow();
        HospitalEpisode currentEpisode = repository
                .findEpisodeById(current.appointment().episodeId()).orElseThrow();
        HospitalConsultation consultation = completedConsultation(
                "rollback-consultation", current, assignedDoctor(repository, current));
        HospitalEpisode completion = completedEpisode(currentEpisode);
        injectEpisodeTransitionConflict(
                path, currentEpisode, EpisodeStatus.COMPLETED, "consultation");

        assertLateSqlFailure(
                () -> repository.saveConsultationAndUpdateBooking(
                        consultation, completedBooking(current), completion,
                        clinicalBill(
                                "rollback-treatment-bill", current,
                                HospitalBillType.TREATMENT, 1_800)));

        AccessHospitalRepository reopened = repository(path);
        assertFalse(reopened.findConsultationByAppointmentId(
                view.getAppointmentId()).isPresent());
        assertEquals(AppointmentStatus.BOOKED,
                reopened.findAppointmentById(view.getAppointmentId()).orElseThrow().status());
        assertEquals(EpisodeStatus.IN_PROGRESS,
                reopened.findEpisodeById(currentEpisode.episodeId()).orElseThrow().status());
        assertEquals(current.billItem(),
                reopened.findBookingById(view.getAppointmentId()).orElseThrow().billItem());
        assertFalse(reopened.findBillById("rollback-treatment-bill").isPresent());
    }

    private void assertExaminationPlanRollback() throws Exception {
        Path path = temporaryDirectory.resolve("rollback-examination-plan.accdb");
        SessionInfo patient = session("U-ROLLBACK-PLAN-001");
        AppointmentBookingView newView = service(path).bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        AccessHospitalRepository repository = repository(path);
        HospitalBooking current = repository.findBookingById(newView.getAppointmentId())
                .orElseThrow();
        HospitalEpisode episode = repository.findEpisodeById(
                current.appointment().episodeId()).orElseThrow();
        String doctorId = assignedDoctor(repository, current);
        HospitalConsultation stageNote = waitingConsultation(
                "rollback-plan-consultation", current, doctorId);
        HospitalExaminationOrder newOrder = new HospitalExaminationOrder(
                "rollback-plan-order", episode.episodeId(),
                current.appointment().appointmentId(),
                doctorId, current.appointment().patientUserId(), "新的检查项目", "新的说明",
                ExaminationStatus.ORDERED, NOW, null);
        injectEpisodeTransitionConflict(
                path, episode, EpisodeStatus.WAITING_FOR_RESULTS, "plan");

        assertLateSqlFailure(
                () -> repository.saveExaminationPlan(
                        stageNote, completedBooking(current), waitingEpisode(episode),
                        newOrder,
                        clinicalBill(
                                "rollback-examination-bill", current,
                                HospitalBillType.EXAMINATION, 3_000)));

        AccessHospitalRepository reopened = repository(path);
        assertFalse(reopened.findConsultationByAppointmentId(
                newView.getAppointmentId()).isPresent());
        assertEquals(AppointmentStatus.BOOKED,
                reopened.findAppointmentById(newView.getAppointmentId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.IN_PROGRESS,
                reopened.findEpisodeById(episode.episodeId()).orElseThrow().status());
        assertFalse(reopened.findExaminationOrderById(newOrder.orderId()).isPresent());
        assertFalse(reopened.findBillById("rollback-examination-bill").isPresent());
    }

    private void assertExaminationReportRollback() throws Exception {
        Path path = temporaryDirectory.resolve("rollback-examination-report.accdb");
        ExaminationFixture waitingFixture = openExamination(
                path, "U-ROLLBACK-REPORT-001");
        AccessHospitalRepository repository = repository(path);
        HospitalExaminationOrder currentOrder = repository
                .findExaminationOrderById(waitingFixture.orderId()).orElseThrow();
        HospitalEpisode currentEpisode = repository
                .findEpisodeById(currentOrder.episodeId()).orElseThrow();
        HospitalExaminationOrder reportedOrder = withStatus(
                currentOrder, ExaminationStatus.RESULT_READY, null);
        HospitalExaminationReport newReport = new HospitalExaminationReport(
                "rollback-report", currentOrder.orderId(),
                "末端冲突前已经插入、随后必须回滚的报告", NOW);
        injectEpisodeTransitionConflict(
                path, currentEpisode, EpisodeStatus.RESULT_READY, "report");

        assertLateSqlFailure(
                () -> repository.saveExaminationReport(
                        reportedOrder, newReport, readyEpisode(currentEpisode)));

        AccessHospitalRepository reopened = repository(path);
        assertEquals(ExaminationStatus.ORDERED,
                reopened.findExaminationOrderById(waitingFixture.orderId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.WAITING_FOR_RESULTS,
                reopened.findEpisodeById(currentEpisode.episodeId()).orElseThrow().status());
        assertFalse(reopened.findExaminationReportByOrderId(
                waitingFixture.orderId()).isPresent());
    }

    private void assertResultReviewCompletionRollback() throws Exception {
        Path path = temporaryDirectory.resolve("rollback-result-review.accdb");
        ExaminationFixture fixture = openExamination(
                path, "U-ROLLBACK-REVIEW-001");
        HospitalService service = service(path);
        service.publishDemoExaminationReport(
                fixture.patient(), new PublishDemoExaminationReportRequest(fixture.orderId()));
        AppointmentBookingView reviewView = service(path).bookResultReview(
                fixture.patient(), new BookResultReviewRequest(fixture.orderId()));
        AccessHospitalRepository repository = repository(path);
        HospitalBooking review = repository.findBookingById(reviewView.getAppointmentId())
                .orElseThrow();
        HospitalEpisode episode = repository
                .findEpisodeById(review.appointment().episodeId()).orElseThrow();
        HospitalExaminationOrder order = repository
                .findExaminationOrderById(fixture.orderId()).orElseThrow();
        HospitalConsultation consultation = completedConsultation(
                "rollback-review-consultation", review, assignedDoctor(repository, review));
        injectEpisodeTransitionConflict(
                path, episode, EpisodeStatus.COMPLETED, "review");

        assertLateSqlFailure(
                () -> repository.saveResultReviewConsultation(
                        consultation,
                        completedBooking(review),
                        completedEpisode(episode),
                        withStatus(order, ExaminationStatus.REVIEWED, NOW)));

        AccessHospitalRepository reopened = repository(path);
        assertFalse(reopened.findConsultationByAppointmentId(
                reviewView.getAppointmentId()).isPresent());
        assertEquals(AppointmentStatus.BOOKED,
                reopened.findAppointmentById(reviewView.getAppointmentId())
                        .orElseThrow().status());
        assertEquals(ExaminationStatus.RESULT_READY,
                reopened.findExaminationOrderById(fixture.orderId())
                        .orElseThrow().status());
        assertEquals(EpisodeStatus.RESULT_READY,
                reopened.findEpisodeById(episode.episodeId()).orElseThrow().status());
        assertTrue(reopened.findExaminationReportByOrderId(
                fixture.orderId()).isPresent());
    }

    private static void assertLateSqlFailure(Runnable action) {
        IllegalStateException failure = assertThrows(IllegalStateException.class, action::run);
        assertInstanceOf(SQLException.class, failure.getCause());
    }

    private ExaminationFixture openExamination(Path path, String patientUserId) {
        SessionInfo patient = session(patientUserId);
        HospitalService service = service(path);
        AppointmentBookingView appointment = service.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        ExaminationOrderView order = service.submitExaminationPlan(
                DOCTOR,
                new SubmitExaminationPlanRequest(
                        appointment.getAppointmentId(), "原阶段诊断", "原检查项目", "", ""));
        return new ExaminationFixture(patient, appointment.getAppointmentId(), order.getOrderId());
    }

    private HospitalConsultation completedConsultation(
            String consultationId,
            HospitalBooking booking,
            String doctorId) {
        return new HospitalConsultation(
                consultationId,
                booking.appointment().appointmentId(),
                doctorId,
                booking.appointment().patientUserId(),
                ConsultationOutcome.COMPLETED,
                "事务回滚诊断",
                "",
                "事务回滚处置",
                "",
                "",
                NOW);
    }

    private HospitalConsultation waitingConsultation(
            String consultationId,
            HospitalBooking booking,
            String doctorId) {
        return new HospitalConsultation(
                consultationId,
                booking.appointment().appointmentId(),
                doctorId,
                booking.appointment().patientUserId(),
                ConsultationOutcome.WAITING_FOR_RESULTS,
                "事务回滚阶段诊断",
                "新的检查项目：新的说明",
                "",
                "",
                "检查结果出具后回诊。",
                NOW);
    }

    private HospitalBooking completedBooking(HospitalBooking current) {
        return new HospitalBooking(
                completedAppointment(current.appointment()),
                current.bill(),
                current.billItem());
    }

    private HospitalAppointment completedAppointment(HospitalAppointment current) {
        return new HospitalAppointment(
                current.appointmentId(), current.patientUserId(), current.scheduleId(),
                current.queueNumber(), current.createdAt(), null, NOW,
                AppointmentStatus.COMPLETED, current.visitType(), current.episodeId(),
                current.sourceFirstVisitAppointmentId());
    }

    private HospitalEpisode waitingEpisode(HospitalEpisode current) {
        return new HospitalEpisode(
                current.episodeId(), current.patientUserId(), current.departmentId(),
                EpisodeStatus.WAITING_FOR_RESULTS, current.openedAt(), null);
    }

    private HospitalEpisode readyEpisode(HospitalEpisode current) {
        return new HospitalEpisode(
                current.episodeId(), current.patientUserId(), current.departmentId(),
                EpisodeStatus.RESULT_READY, current.openedAt(), null);
    }

    private HospitalEpisode completedEpisode(HospitalEpisode current) {
        return new HospitalEpisode(
                current.episodeId(), current.patientUserId(), current.departmentId(),
                EpisodeStatus.COMPLETED, current.openedAt(), NOW);
    }

    private HospitalExaminationOrder withStatus(
            HospitalExaminationOrder current,
            ExaminationStatus status,
            LocalDateTime reviewedAt) {
        return new HospitalExaminationOrder(
                current.orderId(), current.episodeId(), current.orderedAppointmentId(),
                current.doctorId(), current.patientUserId(), current.itemName(),
                current.instructions(), status, current.orderedAt(), reviewedAt);
    }

    private HospitalPatientBill clinicalBill(
            String billId,
            HospitalBooking booking,
            HospitalBillType type,
            long amountCents) {
        return new HospitalPatientBill(
                billId,
                booking.appointment().appointmentId(),
                booking.appointment().patientUserId(),
                type,
                PaymentStatus.UNPAID,
                NOW,
                null,
                null,
                new HospitalBillItem(
                        billId + "-item", billId, "事务回滚费用", 1, amountCents));
    }

    private String assignedDoctor(
            AccessHospitalRepository repository,
            HospitalBooking booking) {
        return repository.findSlotById(booking.appointment().scheduleId())
                .orElseThrow().doctorId();
    }

    private void injectEpisodeTransitionConflict(
            Path path,
            HospitalEpisode currentEpisode,
            EpisodeStatus targetStatus,
            String suffix) throws Exception {
        AccessDatabase database = new AccessDatabase(path);
        try (Connection connection = database.openConnection()) {
            try (PreparedStatement deleteOtherEpisodes = connection.prepareStatement(
                    "DELETE FROM tblHospitalEpisode WHERE episodeId <> ?")) {
                deleteOtherEpisodes.setString(1, currentEpisode.episodeId());
                deleteOtherEpisodes.executeUpdate();
            }
            try (PreparedStatement insertSentinel = connection.prepareStatement(
                    "INSERT INTO tblHospitalEpisode "
                            + "(episodeId, patientUserId, departmentId, status, openedAt, "
                            + "completedAt) VALUES (?, ?, ?, ?, ?, ?)")) {
                insertSentinel.setString(1, "rollback-sentinel-" + suffix);
                insertSentinel.setString(2, currentEpisode.patientUserId());
                insertSentinel.setString(3, currentEpisode.departmentId());
                insertSentinel.setString(4, targetStatus.name());
                insertSentinel.setTimestamp(5, Timestamp.valueOf(currentEpisode.openedAt()));
                insertSentinel.setTimestamp(6, targetStatus == EpisodeStatus.COMPLETED
                        ? Timestamp.valueOf(NOW)
                        : null);
                insertSentinel.executeUpdate();
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE UNIQUE INDEX ux_test_episode_patient_status_" + suffix
                                + " ON tblHospitalEpisode (patientUserId, status)");
            }
        }
    }

    private int rowCount(Path path, String tableName) throws Exception {
        AccessDatabase database = new AccessDatabase(path);
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT COUNT(*) AS rowCount FROM " + tableName)) {
            assertTrue(result.next());
            return result.getInt("rowCount");
        }
    }

    private HospitalService service(Path path) {
        return new HospitalService(repository(path), CLOCK);
    }

    private AccessHospitalRepository repository(Path path) {
        return new AccessHospitalRepository(new AccessDatabase(path), CLOCK);
    }

    private static SessionInfo session(String userId) {
        return new SessionInfo(
                "token-" + userId, userId, "demo", "集成测试用户", Role.USER);
    }

    private static void assertBusinessFailure(String expectedCode, Runnable action) {
        HospitalBusinessException exception = assertThrows(
                HospitalBusinessException.class, action::run);
        assertEquals(expectedCode, exception.errorCode());
    }

    private record ExaminationFixture(
            SessionInfo patient,
            String appointmentId,
            String orderId) {
    }
}
