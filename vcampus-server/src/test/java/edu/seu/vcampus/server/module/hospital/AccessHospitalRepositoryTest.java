package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessHospitalRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndReloadsDepartmentDoctorAndScheduleCatalog() {
        Path path = temporaryDirectory.resolve("hospital-catalog.accdb");
        AccessHospitalRepository first = repository(path);

        assertEquals(15, first.findActiveDepartments().size());
        assertEquals(14, first.findSlots(
                LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 11)).size());
        assertEquals("doctor-chen",
                first.findActiveDoctorByUserId("U-DOCTOR-001")
                        .orElseThrow().doctorId());

        AccessHospitalRepository reopened = repository(path);
        assertEquals(15, reopened.findActiveDepartments().size());
        assertFalse(reopened.findSlotsByDoctorId("doctor-chen").isEmpty());
        assertEquals("slot-general-1",
                reopened.findSlotById("slot-general-1").orElseThrow().scheduleId());
    }

    @Test
    void savesAndReloadsNewDoctorAccountBinding() {
        Path path = temporaryDirectory.resolve("new-doctor.accdb");
        AccessHospitalRepository repository = repository(path);

        repository.saveDoctorProfile(new DoctorProfile(
                "U-NEW-DOCTOR-001", "dept-general", "新医生",
                "主治医师", true));

        HospitalDoctor doctor = repository(path)
                .findActiveDoctorByUserId("U-NEW-DOCTOR-001")
                .orElseThrow();
        assertEquals("新医生", doctor.doctorName());
        assertEquals("dept-general", doctor.departmentId());
    }

    @Test
    void insertsUpdatesAndReloadsDepartmentCatalog() {
        Path path = temporaryDirectory.resolve("department-maintenance.accdb");
        AccessHospitalRepository first = repository(path);
        HospitalDepartment created = new HospitalDepartment(
                "dept-admin-test", "康复医学", null, false, true);

        first.insertDepartment(created);
        assertEquals(16, repository(path).findActiveDepartments().size());

        first = repository(path);
        first.updateDepartment(new HospitalDepartment(
                created.departmentId(), "康复医学科", null, false, false));

        AccessHospitalRepository reopened = repository(path);
        HospitalDepartment persisted = reopened.findAllDepartments().stream()
                .filter(item -> item.departmentId().equals(created.departmentId()))
                .findFirst()
                .orElseThrow();
        assertEquals("康复医学科", persisted.departmentName());
        assertFalse(persisted.active());
        assertFalse(reopened.findActiveDepartments().stream()
                .anyMatch(item -> item.departmentId().equals(created.departmentId())));
    }

    @Test
    void insertsPublishesAndReloadsAdministratorSchedule() {
        Path path = temporaryDirectory.resolve("administrator-schedule.accdb");
        AccessHospitalRepository first = repository(path);
        LocalDateTime start = LocalDateTime.of(2026, 9, 9, 16, 0);
        HospitalSlot draft = new HospitalSlot(
                "slot-admin-persist-001",
                "dept-general",
                "全科门诊",
                "doctor-chen",
                "陈医生",
                "主治医师",
                start,
                start.plusMinutes(30),
                1_500,
                6,
                0,
                false);

        first.insertSlot(draft);
        HospitalSlot reloadedDraft = repository(path)
                .findSlotById(draft.scheduleId())
                .orElseThrow();
        assertFalse(reloadedDraft.published());
        assertEquals(6, reloadedDraft.capacity());

        first = repository(path);
        first.updateSlot(new HospitalSlot(
                draft.scheduleId(), draft.departmentId(), draft.departmentName(),
                draft.doctorId(), draft.doctorName(), draft.doctorTitle(),
                draft.startTime(), draft.endTime(), draft.priceCents(),
                draft.capacity(), draft.bookedCount(), true));

        AccessHospitalRepository afterPublish = repository(path);
        assertEquals(10, afterPublish.findActiveDoctors().size());
        assertEquals(15, afterPublish.findAllSlots().size());
        assertEquals(true, afterPublish.findSlotById(draft.scheduleId())
                .orElseThrow().published());
    }

    @Test
    void savesUpdatesAndReloadsPatientHealthProfile() {
        Path path = temporaryDirectory.resolve("patient-profile.accdb");
        AccessHospitalRepository first = repository(path);
        LocalDateTime firstUpdate = LocalDateTime.of(2026, 9, 4, 9, 15);

        first.savePatientProfile(new HospitalPatientProfile(
                "U-PATIENT-PERSIST-001",
                "B型",
                "海鲜过敏",
                "既往骨折",
                "",
                "校园联系人 000-1000（演示）",
                firstUpdate));

        HospitalPatientProfile reloaded = repository(path)
                .findPatientProfile("U-PATIENT-PERSIST-001")
                .orElseThrow();
        assertEquals("B型", reloaded.bloodType());
        assertEquals("海鲜过敏", reloaded.allergies());
        assertEquals("", reloaded.longTermMedication());
        assertEquals(firstUpdate, reloaded.updatedAt());

        LocalDateTime secondUpdate = firstUpdate.plusHours(2);
        reloaded = new HospitalPatientProfile(
                reloaded.patientUserId(),
                "AB型",
                "无已知过敏",
                reloaded.medicalHistory(),
                "维生素（演示）",
                reloaded.emergencyContact(),
                secondUpdate);
        repository(path).savePatientProfile(reloaded);

        HospitalPatientProfile updated = repository(path)
                .findPatientProfile("U-PATIENT-PERSIST-001")
                .orElseThrow();
        assertEquals("AB型", updated.bloodType());
        assertEquals("无已知过敏", updated.allergies());
        assertEquals("维生素（演示）", updated.longTermMedication());
        assertEquals(secondUpdate, updated.updatedAt());
    }

    @Test
    void persistsBookingCancellationAndRollsBackPartialAggregate() {
        Path path = temporaryDirectory.resolve("appointment-lifecycle.accdb");
        AccessHospitalRepository first = repository(path);
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 4, 9, 0);
        HospitalBooking booked = booking(
                "appointment-persist-001",
                "episode-persist-001",
                "bill-persist-001",
                "bill-item-persist-001",
                AppointmentStatus.BOOKED,
                PaymentStatus.PAID,
                createdAt,
                null);
        HospitalEpisode episode = new HospitalEpisode(
                "episode-persist-001",
                "U-PATIENT-PERSIST-001",
                "dept-general",
                EpisodeStatus.IN_PROGRESS,
                createdAt,
                null);

        first.saveBookingAndEpisode(booked, episode);

        AccessHospitalRepository reopened = repository(path);
        HospitalBooking persisted = reopened
                .findBookingById("appointment-persist-001")
                .orElseThrow();
        assertEquals(AppointmentStatus.BOOKED, persisted.appointment().status());
        assertEquals(PaymentStatus.PAID, persisted.bill().paymentStatus());
        assertEquals(1_200, persisted.billItem().amountCents());
        assertEquals(EpisodeStatus.IN_PROGRESS,
                reopened.findEpisodeById("episode-persist-001")
                        .orElseThrow().status());
        assertEquals(5, reopened.findAppointmentsByScheduleId("slot-general-1")
                .stream().filter(HospitalAppointment::occupiesSlot).count());

        HospitalEpisode rolledBackEpisode = new HospitalEpisode(
                "episode-should-rollback",
                "U-PATIENT-PERSIST-001",
                "dept-general",
                EpisodeStatus.IN_PROGRESS,
                createdAt,
                null);
        HospitalBooking duplicateAppointment = booking(
                "appointment-persist-001",
                "episode-should-rollback",
                "bill-should-rollback",
                "bill-item-should-rollback",
                AppointmentStatus.BOOKED,
                PaymentStatus.PAID,
                createdAt,
                null);
        assertThrows(IllegalStateException.class,
                () -> reopened.saveBookingAndEpisode(
                        duplicateAppointment, rolledBackEpisode));
        assertFalse(repository(path).findEpisodeById("episode-should-rollback").isPresent());

        LocalDateTime cancelledAt = createdAt.plusHours(1);
        HospitalBooking cancelled = booking(
                "appointment-persist-001",
                "episode-persist-001",
                "bill-persist-001",
                "bill-item-persist-001",
                AppointmentStatus.CANCELLED,
                PaymentStatus.REFUNDED,
                createdAt,
                cancelledAt);
        HospitalEpisode cancelledEpisode = new HospitalEpisode(
                episode.episodeId(),
                episode.patientUserId(),
                episode.departmentId(),
                EpisodeStatus.CANCELLED,
                episode.openedAt(),
                null);
        reopened.updateBookingAndEpisode(cancelled, cancelledEpisode);

        AccessHospitalRepository afterCancellation = repository(path);
        HospitalBooking reloadedCancellation = afterCancellation
                .findBookingById("appointment-persist-001")
                .orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED,
                reloadedCancellation.appointment().status());
        assertEquals(cancelledAt, reloadedCancellation.appointment().cancelledAt());
        assertEquals(PaymentStatus.REFUNDED,
                reloadedCancellation.bill().paymentStatus());
        assertEquals(cancelledAt, reloadedCancellation.bill().refundedAt());
        assertEquals(EpisodeStatus.CANCELLED,
                afterCancellation.findEpisodeById("episode-persist-001")
                        .orElseThrow().status());
        assertEquals(4, afterCancellation.findAppointmentsByScheduleId("slot-general-1")
                .stream().filter(HospitalAppointment::occupiesSlot).count());
    }

    @Test
    void serviceKeepsAppointmentAndRemainingCountAcrossRepositoryRecreation() {
        Path path = temporaryDirectory.resolve("appointment-service.accdb");
        SessionInfo patient = new SessionInfo(
                "token-access-patient",
                "U-ACCESS-PATIENT-001",
                "access-patient",
                "Access测试患者",
                Role.USER);
        HospitalService firstService = new HospitalService(repository(path), CLOCK);
        int initialRemaining = remaining(firstService, patient, "slot-general-1");

        var booking = firstService.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        String appointmentId = booking.getAppointmentId();

        HospitalService reopenedService = new HospitalService(repository(path), CLOCK);
        assertEquals(1, reopenedService.listMyAppointments(patient)
                .getAppointments().stream()
                .filter(appointment -> appointment.getAppointmentId().equals(appointmentId))
                .count());
        assertEquals(initialRemaining - 1,
                remaining(reopenedService, patient, "slot-general-1"));

        reopenedService.cancelAppointment(
                patient, new CancelAppointmentRequest(appointmentId));

        HospitalService afterCancellation = new HospitalService(repository(path), CLOCK);
        assertEquals(AppointmentStatus.CANCELLED,
                afterCancellation.listMyAppointments(patient)
                        .getAppointments().stream()
                        .filter(appointment -> appointment.getAppointmentId()
                                .equals(appointmentId))
                        .findFirst()
                        .orElseThrow()
                        .getAppointmentStatus());
        assertEquals(PaymentStatus.REFUNDED,
                afterCancellation.listMyAppointments(patient)
                        .getAppointments().stream()
                        .filter(appointment -> appointment.getAppointmentId()
                                .equals(appointmentId))
                        .findFirst()
                        .orElseThrow()
                        .getPaymentStatus());
        assertEquals(initialRemaining,
                remaining(afterCancellation, patient, "slot-general-1"));

        var rebooked = afterCancellation.bookAppointment(
                patient, BookAppointmentRequest.firstVisit("slot-general-1"));
        assertEquals(booking.getQueueNumber() + 1, rebooked.getQueueNumber());
        assertEquals(initialRemaining - 1,
                remaining(new HospitalService(repository(path), CLOCK),
                        patient, "slot-general-1"));
    }

    @Test
    void persistsTreatmentBillAndSimulatedPaymentAcrossRepositoryRecreation() {
        Path path = temporaryDirectory.resolve("patient-bill-payment.accdb");
        SessionInfo patient = new SessionInfo(
                "token-bill-patient", "U-ACCESS-BILL-PATIENT", "bill-patient",
                "费用测试患者", Role.USER);
        SessionInfo doctor = new SessionInfo(
                "token-bill-doctor", "U-DOCTOR-001", "teacher001",
                "陈医生", Role.USER);
        HospitalService first = new HospitalService(repository(path), CLOCK);
        String appointmentId = first.bookAppointment(
                        patient, BookAppointmentRequest.firstVisit("slot-general-1"))
                .getAppointmentId();
        first.submitConsultation(doctor, new SubmitConsultationRequest(
                appointmentId,
                "课程演示诊断",
                "",
                "课程演示处置",
                "无",
                "必要时复诊"));

        HospitalService reopened = new HospitalService(repository(path), CLOCK);
        var unpaid = reopened.listMyBills(patient).getBills().stream()
                .filter(bill -> bill.getBillType() == HospitalBillType.TREATMENT)
                .findFirst()
                .orElseThrow();
        assertEquals(PaymentStatus.UNPAID, unpaid.getPaymentStatus());
        assertEquals(1_800, unpaid.getAmountCents());

        reopened.payBill(patient, new PayHospitalBillRequest(unpaid.getBillId()));

        var paid = new HospitalService(repository(path), CLOCK)
                .listMyBills(patient).getBills().stream()
                .filter(bill -> bill.getBillId().equals(unpaid.getBillId()))
                .findFirst()
                .orElseThrow();
        assertEquals(PaymentStatus.PAID, paid.getPaymentStatus());
        assertEquals(unpaid.getBillId(), paid.getBillId());
    }

    @Test
    void persistedAppointmentsRemainUsableByStagedClinicalWorkflow() {
        Path path = temporaryDirectory.resolve("appointment-clinical-bridge.accdb");
        AccessHospitalRepository repository = repository(path);
        HospitalService service = new HospitalService(repository, CLOCK);
        SessionInfo patient = new SessionInfo(
                "token-clinical-patient",
                "U-CLINICAL-PATIENT-001",
                "clinical-patient",
                "临床测试患者",
                Role.USER);
        SessionInfo doctor = new SessionInfo(
                "token-clinical-doctor",
                "U-DOCTOR-001",
                "teacher001",
                "陈医生",
                Role.USER);
        String appointmentId = service.bookAppointment(
                        patient,
                        BookAppointmentRequest.firstVisit("slot-general-1"))
                .getAppointmentId();

        service.submitConsultation(doctor, new SubmitConsultationRequest(
                appointmentId,
                "上呼吸道感染（虚构）",
                "暂无（虚构）",
                "休息并观察（虚构）",
                "无（虚构）",
                "症状加重时复诊（虚构）"));

        assertEquals(1, service.listMyConsultations(patient).getConsultations().size());
        AccessHospitalRepository reopened = repository(path);
        HospitalAppointment completed = reopened.findAppointmentById(appointmentId)
                .orElseThrow();
        assertEquals(AppointmentStatus.COMPLETED, completed.status());
        assertEquals(EpisodeStatus.COMPLETED,
                reopened.findEpisodeById(completed.episodeId()).orElseThrow().status());
    }

    private int remaining(
            HospitalService service,
            SessionInfo patient,
            String scheduleId) {
        return service.searchSlots(
                        patient,
                        SearchSlotsRequest.firstVisit("dept-general", null))
                .getSlots().stream()
                .filter(slot -> slot.getScheduleId().equals(scheduleId))
                .mapToInt(SlotView::getRemaining)
                .findFirst()
                .orElseThrow();
    }

    private HospitalBooking booking(
            String appointmentId,
            String episodeId,
            String billId,
            String billItemId,
            AppointmentStatus appointmentStatus,
            PaymentStatus paymentStatus,
            LocalDateTime createdAt,
            LocalDateTime cancelledAt) {
        HospitalAppointment appointment = new HospitalAppointment(
                appointmentId,
                "U-PATIENT-PERSIST-001",
                "slot-general-1",
                7,
                createdAt,
                cancelledAt,
                null,
                appointmentStatus,
                VisitType.FIRST_VISIT,
                episodeId,
                null);
        HospitalBill bill = new HospitalBill(
                billId,
                appointmentId,
                createdAt,
                createdAt,
                paymentStatus == PaymentStatus.REFUNDED ? cancelledAt : null,
                paymentStatus);
        HospitalBillItem billItem = new HospitalBillItem(
                billItemId,
                billId,
                "挂号费",
                1,
                1_200);
        return new HospitalBooking(appointment, bill, billItem);
    }

    private AccessHospitalRepository repository(Path path) {
        return new AccessHospitalRepository(new AccessDatabase(path), CLOCK);
    }
}
