package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.BookAppointmentRequest;
import edu.seu.vcampus.common.hospital.CancelAppointmentRequest;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.GenerateWeeklySchedulesRequest;
import edu.seu.vcampus.common.hospital.SetSchedulePublicationRequest;
import edu.seu.vcampus.common.hospital.PayHospitalBillRequest;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotView;
import edu.seu.vcampus.common.hospital.SubmitConsultationRequest;
import edu.seu.vcampus.common.hospital.VisitType;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.SessionInfo;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.card.AccessCampusCardStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessHospitalRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC);
    private static final Clock ACTIVE_CLOCK = Clock.fixed(
            Instant.parse("2026-09-05T08:31:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void weeklyGenerationIsIdempotentAcrossAccessRepositoryReopen() {
        Path path = temporaryDirectory.resolve("weekly-auto-generation.accdb");
        SessionInfo admin = new SessionInfo("admin-token", "U-HOSPITAL-ADMIN-001",
                "20260005", "医院管理员", Role.USER, Set.of(AdminScope.HOSPITAL));
        GenerateWeeklySchedulesRequest request = new GenerateWeeklySchedulesRequest(
                "doctor-chen", LocalDate.of(2026, 10, 5),
                List.of(DayOfWeek.MONDAY), 1_200, 6);

        HospitalService initial = new HospitalService(repository(path), CLOCK);
        assertEquals(18, initial.generateWeeklySchedules(admin, request).getCreated());
        HospitalService reopened = new HospitalService(repository(path), CLOCK);
        var retry = reopened.generateWeeklySchedules(admin, request);
        assertEquals(0, retry.getCreated());
        assertEquals(18, retry.getExisting());
        HospitalSlot first = repository(path).findSlotsByDoctorId("doctor-chen")
                .stream().filter(slot -> slot.startTime().toLocalDate()
                        .equals(LocalDate.of(2026, 10, 5)))
                .findFirst().orElseThrow();
        assertTrue(first.published());

        reopened.setSchedulePublication(admin,
                new SetSchedulePublicationRequest(first.scheduleId(), false));
        assertEquals(0, new HospitalService(repository(path), CLOCK)
                .generateWeeklySchedules(admin, request).getCreated());
        assertFalse(repository(path).findSlotById(first.scheduleId())
                .orElseThrow().published());
    }

    @Test
    void failedBulkInsertionDoesNotExposeAPartialPublishedWorkweek() {
        Path path = temporaryDirectory.resolve("weekly-auto-rollback.accdb");
        AccessHospitalRepository store = repository(path);
        LocalDateTime start = LocalDateTime.of(2026, 10, 5, 9, 0);
        HospitalSlot first = new HospitalSlot(
                "atomic-new", "dept-general", "全科门诊", "doctor-chen",
                "陈医生", "副主任医师", start, start.plusMinutes(30),
                1_200, 6, 0, true);
        HospitalSlot duplicateId = new HospitalSlot(
                "slot-general-1", "dept-general", "全科门诊", "doctor-chen",
                "陈医生", "副主任医师", start.plusMinutes(30),
                start.plusMinutes(60), 1_200, 6, 0, true);

        assertThrows(IllegalStateException.class,
                () -> store.insertSlots(List.of(first, duplicateId)));
        assertTrue(repository(path).findSlotById("atomic-new").isEmpty());
    }

    @Test
    void createsAndReloadsDepartmentDoctorAndScheduleCatalog() {
        Path path = temporaryDirectory.resolve("hospital-catalog.accdb");
        AccessHospitalRepository first = repository(path);

        assertEquals(15, first.findActiveDepartments().size());
        assertEquals(16, first.findSlots(
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
    void everyFinalBookableDepartmentHasAnActiveDoctorAndPublishedSchedule() {
        AccessHospitalRepository repository = repository(
                temporaryDirectory.resolve("expanded-specialties.accdb"));
        List<HospitalDepartment> departments = repository.findActiveDepartments();
        List<HospitalDoctor> doctors = repository.findActiveDoctors();
        List<HospitalSlot> schedules = repository.findAllSlots();

        for (String departmentId : departments.stream()
                .filter(HospitalDepartment::bookable)
                .map(HospitalDepartment::departmentId)
                .toList()) {
            assertTrue(doctors.stream().anyMatch(doctor ->
                    doctor.departmentId().equals(departmentId)), departmentId);
            assertTrue(schedules.stream().anyMatch(schedule ->
                    schedule.departmentId().equals(departmentId)
                            && schedule.published()
                            && doctors.stream().anyMatch(doctor ->
                                    doctor.doctorId().equals(schedule.doctorId())
                                            && doctor.departmentId().equals(departmentId))),
                    departmentId);
        }
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
    void repairsLegacyDemoBindingsWithoutReactivatingAnExplicitlyStoppedDoctor() throws Exception {
        Path path = temporaryDirectory.resolve("doctor-account-reconciliation.accdb");
        AccessDatabase database = new AccessDatabase(path);
        new AccessHospitalRepository(database, CLOCK);

        try (Connection connection = database.openConnection()) {
            try (PreparedStatement legacyChen = connection.prepareStatement(
                    "UPDATE tblHospitalDoctor SET userId = ? WHERE doctorId = ?")) {
                legacyChen.setString(1, "U-TEACHER-001");
                legacyChen.setString(2, "doctor-chen");
                assertEquals(1, legacyChen.executeUpdate());
            }
            try (PreparedStatement legacyLiu = connection.prepareStatement(
                    "UPDATE tblHospitalDoctor SET userId = ? WHERE doctorId = ?")) {
                legacyLiu.setString(1, "U-TEACHER-002");
                legacyLiu.setString(2, "doctor-liu");
                assertEquals(1, legacyLiu.executeUpdate());
            }
            try (PreparedStatement stopped = connection.prepareStatement(
                    "INSERT INTO tblHospitalDoctor "
                            + "(doctorId, userId, departmentId, doctorName, doctorTitle, "
                            + "[active], createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stopped.setString(1, "doctor-explicitly-stopped");
                stopped.setString(2, "U-DOCTOR-001");
                stopped.setString(3, "dept-ent");
                stopped.setString(4, "已停用医生");
                stopped.setString(5, "主治医师");
                stopped.setBoolean(6, false);
                Timestamp now = Timestamp.valueOf(LocalDateTime.of(2026, 9, 3, 9, 0));
                stopped.setTimestamp(7, now);
                stopped.setTimestamp(8, now);
                assertEquals(1, stopped.executeUpdate());
            }
        }

        AccessHospitalRepository reopened = new AccessHospitalRepository(database, CLOCK);

        HospitalDoctor migrated = reopened.findActiveDoctorByUserId("U-DOCTOR-002")
                .orElseThrow();
        assertEquals("doctor-liu", migrated.doctorId());
        assertEquals("刘宁", migrated.doctorName());
        assertFalse(reopened.isActiveDoctorUser("U-DOCTOR-001"));
        assertTrue(reopened.findAllDoctors().stream().anyMatch(doctor ->
                doctor.doctorId().equals("doctor-explicitly-stopped")
                        && !doctor.active()));
    }

    @Test
    void insertsUpdatesAndReloadsDepartmentCatalog() {
        Path path = temporaryDirectory.resolve("department-maintenance.accdb");
        AccessHospitalRepository first = repository(path);
        int initialDepartmentCount = first.findActiveDepartments().size();
        HospitalDepartment created = new HospitalDepartment(
                "dept-admin-test", "康复医学", null, false, true);

        first.insertDepartment(created);
        assertEquals(initialDepartmentCount + 1,
                repository(path).findActiveDepartments().size());

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
        assertEquals(17, afterPublish.findAllSlots().size());
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
        assertEquals(0L, reloaded.version());

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
    void conditionallySavesPatientProfileByVersion() {
        Path path = temporaryDirectory.resolve("patient-profile-version.accdb");
        AccessHospitalRepository repository = repository(path);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 4, 12, 0);
        HospitalPatientProfile first = new HospitalPatientProfile(
                "U-PATIENT-VERSION-001", "A型", "无", "", "", "",
                updatedAt, 1L);

        assertEquals(true, repository.savePatientProfileIfVersion(first, 0L));
        HospitalPatientProfile second = new HospitalPatientProfile(
                first.patientUserId(), "B型", "无", "", "", "",
                updatedAt.plusMinutes(1), 2L);
        assertEquals(true, repository.savePatientProfileIfVersion(second, 1L));
        assertEquals(false, repository.savePatientProfileIfVersion(first, 0L));
        assertEquals("B型", repository(path)
                .findPatientProfile(first.patientUserId()).orElseThrow().bloodType());
        assertEquals(2L, repository(path)
                .findPatientProfile(first.patientUserId()).orElseThrow().version());
    }

    @Test
    void migratesLegacyPatientProfileWithoutLosingItsContent() throws Exception {
        Path path = temporaryDirectory.resolve("patient-profile-legacy.accdb");
        AccessDatabase database = new AccessDatabase(path);
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE tblHospitalPatientProfile ("
                    + "patientUserId TEXT(36) PRIMARY KEY, bloodType TEXT(10), "
                    + "allergies MEMO, medicalHistory MEMO, longTermMedication MEMO, "
                    + "emergencyContact TEXT(100), updatedAt DATETIME NOT NULL)");
            statement.executeUpdate("INSERT INTO tblHospitalPatientProfile "
                    + "(patientUserId, bloodType, allergies, updatedAt) VALUES "
                    + "('U-LEGACY-PATIENT', 'O型', '青霉素过敏', #2026-09-04 09:15:00#)");
        }

        HospitalPatientProfile migrated = repository(path)
                .findPatientProfile("U-LEGACY-PATIENT")
                .orElseThrow();

        assertEquals("O型", migrated.bloodType());
        assertEquals("青霉素过敏", migrated.allergies());
        assertEquals(0L, migrated.version());
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
        new HospitalService(repository(path), ACTIVE_CLOCK).submitConsultation(
                doctor, new SubmitConsultationRequest(
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
    void paysAClinicalBillThroughTheAccessCampusCardLedger() {
        Path path = temporaryDirectory.resolve("patient-bill-card-payment.accdb");
        SessionInfo patient = new SessionInfo(
                "token-bill-card-patient", "U-CARD-BILL-PATIENT", "20990001",
                "校园卡缴费患者", Role.USER);
        SessionInfo doctor = new SessionInfo(
                "token-bill-card-doctor", "U-DOCTOR-001", "20260029",
                "陈安", Role.USER);
        AccessCampusCardStore cards = new AccessCampusCardStore(new AccessDatabase(path));
        cards.recharge(patient, 10_000);
        HospitalService service = new HospitalService(repository(path), CLOCK, cards);
        String appointmentId = service.bookAppointment(
                        patient, BookAppointmentRequest.firstVisit("slot-general-1"))
                .getAppointmentId();
        new HospitalService(repository(path), ACTIVE_CLOCK, cards).submitConsultation(
                doctor, new SubmitConsultationRequest(
                appointmentId, "课程演示诊断", "", "课程演示处置", "无", "必要时复诊"));
        var unpaid = service.listMyBills(patient).getBills().stream()
                .filter(bill -> bill.getPaymentStatus() == PaymentStatus.UNPAID)
                .findFirst().orElseThrow();

        var paid = service.payBill(patient, new PayHospitalBillRequest(unpaid.getBillId()));

        assertEquals(PaymentStatus.PAID, paid.getPaymentStatus());
        assertEquals(7_000, cards.view(patient).getBalanceFen());
        String debitReference = cards.listLedger(patient).stream()
                .filter(entry -> entry.getReference().startsWith("bill:"))
                .findFirst().orElseThrow().getReference();
        assertTrue(debitReference.length() <= 73,
                "the derived refund reference must fit Access TEXT(80)");
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

        new HospitalService(repository(path), ACTIVE_CLOCK).submitConsultation(
                doctor, new SubmitConsultationRequest(
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
