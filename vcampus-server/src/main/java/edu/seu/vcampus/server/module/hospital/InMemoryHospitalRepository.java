package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.EpisodeStatus;
import edu.seu.vcampus.common.hospital.ExaminationStatus;
import edu.seu.vcampus.common.hospital.HospitalBillType;
import edu.seu.vcampus.common.hospital.PaymentStatus;
import edu.seu.vcampus.common.hospital.VisitType;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Deterministic fake hospital data used before the Access repository is available. */
final class InMemoryHospitalRepository implements HospitalRepository {

    private final Map<String, DoctorProfile> doctorProfiles = new ConcurrentHashMap<>();
    private final Map<String, DoctorApplication> doctorApplications =
            new ConcurrentHashMap<>();
    private final List<HospitalDoctor> doctors;
    private final List<HospitalDepartment> departments;
    private final List<HospitalSlot> slots;
    private final ConcurrentMap<String, HospitalPatientProfile> patientProfiles =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HospitalBooking> bookings =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HospitalConsultation> consultations =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HospitalEpisode> episodes =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HospitalExaminationOrder> examinationOrders =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HospitalExaminationReport> examinationReports =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HospitalPatientBill> clinicalBills =
            new ConcurrentHashMap<>();

    InMemoryHospitalRepository(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        LocalDate today = LocalDate.now(clock);
        departments = new CopyOnWriteArrayList<>(List.of(
                new HospitalDepartment(
                        "dept-general-category", "综合门诊", null, false, true),
                new HospitalDepartment(
                        "dept-general", "全科门诊", "dept-general-category", true, true),
                new HospitalDepartment("dept-internal", "内科", null, false, true),
                new HospitalDepartment(
                        "dept-respiratory", "呼吸内科", "dept-internal", true, true),
                new HospitalDepartment(
                        "dept-gastroenterology", "消化内科", "dept-internal", true, true),
                new HospitalDepartment("dept-surgery", "外科", null, false, true),
                new HospitalDepartment(
                        "dept-orthopedics", "骨科", "dept-surgery", false, true),
                new HospitalDepartment(
                        "dept-joint-surgery", "骨关节外科", "dept-orthopedics", true, true),
                new HospitalDepartment(
                        "dept-sports-medicine", "运动医学科", "dept-orthopedics", true, true),
                new HospitalDepartment(
                        "dept-mental-health", "精神心理科", null, false, true),
                new HospitalDepartment(
                        "dept-psychology", "心理咨询", "dept-mental-health", true, true),
                new HospitalDepartment(
                        "dept-dental-category", "口腔科", null, false, true),
                new HospitalDepartment(
                        "dept-dental", "口腔综合门诊", "dept-dental-category", true, true),
                new HospitalDepartment(
                        "dept-eye-category", "眼科", null, false, true),
                new HospitalDepartment(
                        "dept-eye", "眼科门诊", "dept-eye-category", true, true)));
        doctors = new ArrayList<>(List.of(
                new HospitalDoctor(
                        "doctor-chen",
                        "U-TEACHER-001",
                        "dept-general",
                        "陈医生",
                        "主治医师",
                        true),
                new HospitalDoctor(
                        "doctor-liu", "U-TEACHER-002", "dept-general",
                        "刘医生", "副主任医师", true),
                new HospitalDoctor(
                        "doctor-zhou", null, "dept-respiratory", "周医生", "主治医师", true),
                new HospitalDoctor(
                        "doctor-qian", null, "dept-gastroenterology", "钱医生", "副主任医师", true),
                new HospitalDoctor(
                        "doctor-lin", null, "dept-joint-surgery", "林医生", "副主任医师", true),
                new HospitalDoctor(
                        "doctor-he", null, "dept-joint-surgery", "何医生", "主治医师", true),
                new HospitalDoctor(
                        "doctor-wu", null, "dept-sports-medicine", "吴医生", "主治医师", true),
                new HospitalDoctor(
                        "doctor-zhang", null, "dept-psychology", "张医生", "主治医师", true),
                new HospitalDoctor(
                        "doctor-wang", null, "dept-psychology", "王医生", "副主任医师", true),
                new HospitalDoctor(
                        "doctor-zhao", null, "dept-dental", "赵医生", "主治医师", true),
                new HospitalDoctor(
                        "doctor-sun", null, "dept-eye", "孙医生", "主任医师", true)));
        doctorProfiles.put("U-TEACHER-001",
                new DoctorProfile(
                        "U-TEACHER-001", "dept-general", "陈医生",
                        "主治医师", true));
        doctorProfiles.put("U-TEACHER-002",
                new DoctorProfile(
                        "U-TEACHER-002", "dept-general", "刘医生",
                        "副主任医师", true));
        LocalDateTime profileUpdatedAt = LocalDateTime.now(clock).minusDays(2);
        patientProfiles.put("U-STUDENT-001",
                new HospitalPatientProfile(
                        "U-STUDENT-001",
                        "O型（演示）",
                        "无已知过敏（演示）",
                        "无重要既往史（演示）",
                        "无长期用药（演示）",
                        "校园联系人 000-0000（演示）",
                        profileUpdatedAt));
        patientProfiles.put("U-DEMO-PATIENT-001",
                new HospitalPatientProfile(
                        "U-DEMO-PATIENT-001",
                        "A型（演示）",
                        "青霉素过敏（虚构）",
                        "季节性鼻炎（虚构）",
                        "无长期用药（演示）",
                        "校园联系人 000-0001（演示）",
                        profileUpdatedAt));
        slots = new CopyOnWriteArrayList<>(List.of(
                slot("slot-general-1", "dept-general", "全科门诊",
                        "doctor-chen", "陈医生", "主治医师",
                        today.plusDays(1), 8, 30, 1_200, 12, 0, true),
                slot("slot-general-2", "dept-general", "全科门诊",
                        "doctor-chen", "陈医生", "主治医师",
                        today.plusDays(3), 14, 0, 1_200, 10, 0, true),
                slot("slot-general-3", "dept-general", "全科门诊",
                        "doctor-liu", "刘医生", "副主任医师",
                        today.plusDays(2), 9, 30, 1_800, 8, 2, true),
                slot("slot-general-4", "dept-general", "全科门诊",
                        "doctor-chen", "陈医生", "主治医师",
                        today.plusDays(4), 10, 0, 1_200, 10, 0, true),
                slot("slot-respiratory-1", "dept-respiratory", "呼吸内科",
                        "doctor-zhou", "周医生", "主治医师",
                        today.plusDays(1), 10, 0, 1_600, 8, 3, true),
                slot("slot-gastroenterology-1", "dept-gastroenterology", "消化内科",
                        "doctor-qian", "钱医生", "副主任医师",
                        today.plusDays(2), 14, 30, 1_800, 8, 1, true),
                slot("slot-joint-lin-am", "dept-joint-surgery", "骨关节外科",
                        "doctor-lin", "林医生", "副主任医师",
                        today.plusDays(1), 8, 30, 2_200, 8, 3, true),
                slot("slot-joint-lin-pm", "dept-joint-surgery", "骨关节外科",
                        "doctor-lin", "林医生", "副主任医师",
                        today.plusDays(1), 14, 0, 2_200, 6, 1, true),
                slot("slot-joint-he-1", "dept-joint-surgery", "骨关节外科",
                        "doctor-he", "何医生", "主治医师",
                        today.plusDays(2), 9, 0, 1_800, 10, 4, true),
                slot("slot-sports-1", "dept-sports-medicine", "运动医学科",
                        "doctor-wu", "吴医生", "主治医师",
                        today.plusDays(3), 15, 30, 1_800, 8, 2, true),
                slot("slot-psychology-1", "dept-psychology", "心理咨询",
                        "doctor-zhang", "张医生", "主治医师",
                        today.plusDays(1), 9, 0, 2_000, 6, 2, true),
                slot("slot-psychology-2", "dept-psychology", "心理咨询",
                        "doctor-wang", "王医生", "副主任医师",
                        today.plusDays(4), 15, 0, 2_600, 5, 5, true),
                slot("slot-dental-1", "dept-dental", "口腔综合门诊",
                        "doctor-zhao", "赵医生", "主治医师",
                        today.plusDays(2), 10, 0, 1_500, 8, 3, true),
                slot("slot-dental-2", "dept-dental", "口腔综合门诊",
                        "doctor-zhao", "赵医生", "主治医师",
                        today.plusDays(6), 13, 30, 1_500, 8, 1, true),
                slot("slot-eye-1", "dept-eye", "眼科门诊",
                        "doctor-sun", "孙医生", "主任医师",
                        today.plusDays(5), 8, 30, 2_200, 10, 6, true),
                slot("slot-eye-closed", "dept-eye", "眼科门诊",
                        "doctor-sun", "孙医生", "主任医师",
                        today.plusDays(3), 8, 30, 2_200, 10, 1, false)));
        LocalDateTime seededAt = LocalDateTime.now(clock).minusDays(1);
        seedBookings("slot-general-1", 4, 1_200, 1, seededAt);
        seedBookings("slot-general-2", 10, 1_200, 101, seededAt);
    }

    @Override
    public Optional<HospitalDoctor> findActiveDoctorByUserId(String userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return doctors.stream()
                .filter(HospitalDoctor::active)
                .filter(doctor -> userId.equals(doctor.userId()))
                .findFirst();
    }

    @Override
    public List<HospitalDoctor> findActiveDoctors() {
        return doctors.stream().filter(HospitalDoctor::active).toList();
    }

    /** Returns the deterministic physician catalog used to seed a new Access database. */
    @Override
    public List<HospitalDoctor> findAllDoctors() {
        return List.copyOf(doctors);
    }

    /** Returns the deterministic schedule catalog used to seed a new Access database. */
    @Override
    public List<HospitalSlot> findAllSlots() {
        return List.copyOf(slots);
    }

    @Override
    public List<DoctorApplication> findDoctorApplications() {
        return List.copyOf(doctorApplications.values());
    }

    @Override
    public Optional<DoctorApplication> findDoctorApplication(String requestId) {
        return Optional.ofNullable(doctorApplications.get(requestId));
    }

    @Override
    public void saveDoctorApplication(DoctorApplication application) {
        doctorApplications.put(application.requestId(), application);
    }

    @Override
    public synchronized void saveDoctorProfile(DoctorProfile profile) {
        doctorProfiles.put(profile.userId(), profile);
        doctors.removeIf(doctor -> profile.userId().equals(doctor.userId()));
        doctors.add(new HospitalDoctor(
                profile.userId(),
                profile.userId(),
                profile.departmentId(),
                profile.doctorName(),
                profile.doctorTitle(),
                profile.active()));
    }

    @Override
    public List<HospitalDepartment> findActiveDepartments() {
        return departments.stream().filter(HospitalDepartment::active).toList();
    }

    @Override
    public List<HospitalDepartment> findAllDepartments() {
        return List.copyOf(departments);
    }

    @Override
    public synchronized void insertDepartment(HospitalDepartment department) {
        Objects.requireNonNull(department, "department must not be null");
        if (departments.stream().anyMatch(existing -> existing.departmentId()
                .equals(department.departmentId()))) {
            throw new IllegalStateException("department already exists");
        }
        departments.add(department);
    }

    @Override
    public synchronized void updateDepartment(HospitalDepartment department) {
        Objects.requireNonNull(department, "department must not be null");
        boolean removed = departments.removeIf(existing -> existing.departmentId()
                .equals(department.departmentId()));
        if (!removed) {
            throw new IllegalStateException("department does not exist");
        }
        departments.add(department);
        for (int index = 0; index < slots.size(); index++) {
            HospitalSlot slot = slots.get(index);
            if (slot.departmentId().equals(department.departmentId())) {
                slots.set(index, new HospitalSlot(
                        slot.scheduleId(), slot.departmentId(), department.departmentName(),
                        slot.doctorId(), slot.doctorName(), slot.doctorTitle(),
                        slot.startTime(), slot.endTime(), slot.priceCents(), slot.capacity(),
                        slot.bookedCount(), slot.published()));
            }
        }
    }

    @Override
    public List<HospitalSlot> findSlots(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        return slots.stream()
                .filter(slot -> !slot.startTime().toLocalDate().isBefore(startDate))
                .filter(slot -> !slot.startTime().toLocalDate().isAfter(endDate))
                .toList();
    }

    @Override
    public List<HospitalSlot> findSlotsByDoctorId(String doctorId) {
        Objects.requireNonNull(doctorId, "doctorId must not be null");
        return slots.stream()
                .filter(slot -> slot.doctorId().equals(doctorId))
                .toList();
    }

    @Override
    public Optional<HospitalSlot> findSlotById(String scheduleId) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        return slots.stream()
                .filter(slot -> slot.scheduleId().equals(scheduleId))
                .findFirst();
    }

    @Override
    public void insertSlot(HospitalSlot slot) {
        Objects.requireNonNull(slot, "slot must not be null");
        if (findSlotById(slot.scheduleId()).isPresent()) {
            throw new IllegalStateException("schedule already exists: " + slot.scheduleId());
        }
        slots.add(slot);
    }

    @Override
    public void updateSlot(HospitalSlot slot) {
        Objects.requireNonNull(slot, "slot must not be null");
        boolean removed = slots.removeIf(existing -> existing.scheduleId()
                .equals(slot.scheduleId()));
        if (!removed) {
            throw new IllegalStateException("schedule does not exist: " + slot.scheduleId());
        }
        slots.add(slot);
    }

    @Override
    public List<HospitalAppointment> findAppointmentsByScheduleId(String scheduleId) {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        return bookings.values().stream()
                .map(HospitalBooking::appointment)
                .filter(appointment -> appointment.scheduleId().equals(scheduleId))
                .toList();
    }

    @Override
    public List<HospitalBooking> findBookingsByPatientUserId(String patientUserId) {
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        return bookings.values().stream()
                .filter(booking -> booking.appointment().patientUserId()
                        .equals(patientUserId))
                .toList();
    }

    @Override
    public Optional<HospitalBooking> findBookingById(String appointmentId) {
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        return Optional.ofNullable(bookings.get(appointmentId));
    }

    @Override
    public Optional<HospitalAppointment> findAppointmentById(String appointmentId) {
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        HospitalBooking booking = bookings.get(appointmentId);
        return booking == null ? Optional.empty() : Optional.of(booking.appointment());
    }

    @Override
    public List<HospitalPatientBill> findBillsByPatientUserId(String patientUserId) {
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        List<HospitalPatientBill> result = new ArrayList<>();
        bookings.values().stream()
                .filter(booking -> booking.appointment().patientUserId().equals(patientUserId))
                .map(InMemoryHospitalRepository::registrationBill)
                .forEach(result::add);
        clinicalBills.values().stream()
                .filter(bill -> bill.patientUserId().equals(patientUserId))
                .forEach(result::add);
        return List.copyOf(result);
    }

    @Override
    public Optional<HospitalPatientBill> findBillById(String billId) {
        Objects.requireNonNull(billId, "billId must not be null");
        HospitalPatientBill clinical = clinicalBills.get(billId);
        if (clinical != null) {
            return Optional.of(clinical);
        }
        return bookings.values().stream()
                .filter(booking -> booking.bill().billId().equals(billId))
                .map(InMemoryHospitalRepository::registrationBill)
                .findFirst();
    }

    @Override
    public synchronized void updatePatientBill(HospitalPatientBill bill) {
        Objects.requireNonNull(bill, "bill must not be null");
        if (bill.billType() != HospitalBillType.REGISTRATION) {
            if (clinicalBills.replace(bill.billId(), bill) == null) {
                throw new IllegalStateException("bill does not exist");
            }
            return;
        }
        HospitalBooking booking = bookings.values().stream()
                .filter(candidate -> candidate.bill().billId().equals(bill.billId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("bill does not exist"));
        HospitalBill updated = new HospitalBill(
                bill.billId(), bill.appointmentId(), bill.createdAt(),
                bill.paidAt(), bill.refundedAt(), bill.paymentStatus());
        bookings.put(booking.appointment().appointmentId(),
                new HospitalBooking(booking.appointment(), updated, booking.billItem()));
    }

    @Override
    public Optional<HospitalPatientProfile> findPatientProfile(String patientUserId) {
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        return Optional.ofNullable(patientProfiles.get(patientUserId));
    }

    @Override
    public void savePatientProfile(HospitalPatientProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        patientProfiles.put(profile.patientUserId(), profile);
    }

    List<HospitalPatientProfile> findAllPatientProfiles() {
        return List.copyOf(patientProfiles.values());
    }

    List<HospitalBooking> findAllBookings() {
        return List.copyOf(bookings.values());
    }

    List<HospitalEpisode> findAllEpisodes() {
        return List.copyOf(episodes.values());
    }

    @Override
    public List<HospitalConsultation> findConsultationsByPatientUserId(
            String patientUserId) {
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        return consultations.values().stream()
                .filter(consultation -> consultation.patientUserId().equals(patientUserId))
                .toList();
    }

    @Override
    public List<HospitalConsultation> findConsultationsByDoctorId(String doctorId) {
        return consultations.values().stream()
                .filter(consultation -> consultation.doctorId().equals(doctorId))
                .toList();
    }

    @Override
    public Optional<HospitalConsultation> findConsultationById(String consultationId) {
        return consultations.values().stream()
                .filter(consultation -> consultation.consultationId().equals(consultationId))
                .findFirst();
    }

    @Override
    public Optional<HospitalConsultation> findConsultationByAppointmentId(
            String appointmentId) {
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        return consultations.values().stream()
                .filter(consultation -> consultation.appointmentId().equals(appointmentId))
                .findFirst();
    }

    @Override
    public Optional<HospitalEpisode> findEpisodeById(String episodeId) {
        Objects.requireNonNull(episodeId, "episodeId must not be null");
        return Optional.ofNullable(episodes.get(episodeId));
    }

    @Override
    public List<HospitalExaminationOrder> findExaminationOrdersByPatientUserId(
            String patientUserId) {
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        return examinationOrders.values().stream()
                .filter(order -> order.patientUserId().equals(patientUserId))
                .toList();
    }

    @Override
    public List<HospitalExaminationOrder> findExaminationOrdersByDoctorId(
            String doctorId) {
        Objects.requireNonNull(doctorId, "doctorId must not be null");
        return examinationOrders.values().stream()
                .filter(order -> order.doctorId().equals(doctorId))
                .toList();
    }

    @Override
    public List<HospitalExaminationOrder> findExaminationOrdersByEpisodeId(
            String episodeId) {
        Objects.requireNonNull(episodeId, "episodeId must not be null");
        return examinationOrders.values().stream()
                .filter(order -> order.episodeId().equals(episodeId))
                .toList();
    }

    @Override
    public Optional<HospitalExaminationOrder> findExaminationOrderById(String orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        return Optional.ofNullable(examinationOrders.get(orderId));
    }

    @Override
    public Optional<HospitalExaminationReport> findExaminationReportByOrderId(
            String orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");
        return examinationReports.values().stream()
                .filter(report -> report.orderId().equals(orderId))
                .findFirst();
    }

    @Override
    public void saveBooking(HospitalBooking booking) {
        Objects.requireNonNull(booking, "booking must not be null");
        HospitalBooking previous = bookings.putIfAbsent(
                booking.appointment().appointmentId(), booking);
        if (previous != null) {
            throw new IllegalStateException("appointmentId already exists");
        }
    }

    @Override
    public synchronized void saveBookingAndEpisode(
            HospitalBooking booking,
            HospitalEpisode episode) {
        Objects.requireNonNull(booking, "booking must not be null");
        Objects.requireNonNull(episode, "episode must not be null");
        if (!booking.appointment().episodeId().equals(episode.episodeId())) {
            throw new IllegalArgumentException("booking and episode do not match");
        }
        if (bookings.containsKey(booking.appointment().appointmentId())
                || episodes.containsKey(episode.episodeId())) {
            throw new IllegalStateException("booking or episode already exists");
        }
        bookings.put(booking.appointment().appointmentId(), booking);
        episodes.put(episode.episodeId(), episode);
    }

    @Override
    public void updateBooking(HospitalBooking booking) {
        Objects.requireNonNull(booking, "booking must not be null");
        String appointmentId = booking.appointment().appointmentId();
        HospitalBooking previous = bookings.replace(appointmentId, booking);
        if (previous == null) {
            throw new IllegalStateException("appointment does not exist");
        }
    }

    @Override
    public synchronized void updateBookingAndEpisode(
            HospitalBooking booking,
            HospitalEpisode episode) {
        Objects.requireNonNull(booking, "booking must not be null");
        Objects.requireNonNull(episode, "episode must not be null");
        if (!booking.appointment().episodeId().equals(episode.episodeId())) {
            throw new IllegalArgumentException("booking and episode do not match");
        }
        if (!bookings.containsKey(booking.appointment().appointmentId())
                || !episodes.containsKey(episode.episodeId())) {
            throw new IllegalStateException("booking or episode does not exist");
        }
        bookings.put(booking.appointment().appointmentId(), booking);
        episodes.put(episode.episodeId(), episode);
    }

    @Override
    public synchronized void saveConsultationAndUpdateBooking(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode completedEpisode,
            HospitalPatientBill treatmentBill) {
        Objects.requireNonNull(consultation, "consultation must not be null");
        Objects.requireNonNull(completedBooking, "completedBooking must not be null");
        Objects.requireNonNull(completedEpisode, "completedEpisode must not be null");
        validateClinicalBill(treatmentBill, completedBooking);
        String appointmentId = consultation.appointmentId();
        if (!appointmentId.equals(completedBooking.appointment().appointmentId())) {
            throw new IllegalArgumentException(
                    "consultation and booking must refer to the same appointment");
        }
        if (!completedBooking.appointment().episodeId()
                .equals(completedEpisode.episodeId())) {
            throw new IllegalArgumentException("booking and episode do not match");
        }
        if (findConsultationByAppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("consultation already exists for appointment");
        }
        if (!bookings.containsKey(appointmentId)) {
            throw new IllegalStateException("appointment does not exist");
        }
        if (!episodes.containsKey(completedEpisode.episodeId())) {
            throw new IllegalStateException("episode does not exist");
        }
        if (clinicalBills.containsKey(treatmentBill.billId())) {
            throw new IllegalStateException("bill already exists");
        }
        consultations.put(consultation.consultationId(), consultation);
        bookings.put(appointmentId, completedBooking);
        episodes.put(completedEpisode.episodeId(), completedEpisode);
        clinicalBills.put(treatmentBill.billId(), treatmentBill);
    }

    @Override
    public synchronized void saveExaminationPlan(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode waitingEpisode,
            HospitalExaminationOrder examinationOrder,
            HospitalPatientBill examinationBill) {
        validateClinicalSave(consultation, completedBooking, waitingEpisode);
        validateClinicalBill(examinationBill, completedBooking);
        if (!examinationOrder.episodeId().equals(waitingEpisode.episodeId())
                || !examinationOrder.orderedAppointmentId()
                        .equals(consultation.appointmentId())) {
            throw new IllegalArgumentException("examination order does not match visit");
        }
        if (examinationOrders.containsKey(examinationOrder.orderId())) {
            throw new IllegalStateException("examination order already exists");
        }
        if (clinicalBills.containsKey(examinationBill.billId())) {
            throw new IllegalStateException("bill already exists");
        }
        consultations.put(consultation.consultationId(), consultation);
        bookings.put(consultation.appointmentId(), completedBooking);
        episodes.put(waitingEpisode.episodeId(), waitingEpisode);
        examinationOrders.put(examinationOrder.orderId(), examinationOrder);
        clinicalBills.put(examinationBill.billId(), examinationBill);
    }

    @Override
    public synchronized void saveResultReviewExaminationPlan(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode waitingEpisode,
            HospitalExaminationOrder reviewedOrder,
            HospitalExaminationOrder nextExaminationOrder,
            HospitalPatientBill examinationBill) {
        validateClinicalSave(consultation, completedBooking, waitingEpisode);
        validateClinicalBill(examinationBill, completedBooking);
        HospitalEpisode currentEpisode = episodes.get(waitingEpisode.episodeId());
        HospitalExaminationOrder currentOrder = examinationOrders.get(reviewedOrder.orderId());
        if (completedBooking.appointment().visitType() != VisitType.RESULT_REVIEW
                || currentEpisode == null
                || currentEpisode.status() != EpisodeStatus.RESULT_READY
                || currentOrder == null
                || currentOrder.status() != ExaminationStatus.RESULT_READY
                || reviewedOrder.status() != ExaminationStatus.REVIEWED
                || !reviewedOrder.episodeId().equals(waitingEpisode.episodeId())
                || !reviewedOrder.patientUserId().equals(waitingEpisode.patientUserId())
                || !reviewedOrder.orderedAppointmentId().equals(
                        completedBooking.appointment().sourceFirstVisitAppointmentId())
                || !nextExaminationOrder.episodeId().equals(waitingEpisode.episodeId())
                || !nextExaminationOrder.orderedAppointmentId()
                        .equals(consultation.appointmentId())
                || !nextExaminationOrder.doctorId().equals(consultation.doctorId())
                || nextExaminationOrder.status() != ExaminationStatus.ORDERED) {
            throw new IllegalArgumentException(
                    "repeated examination does not match result-review visit");
        }
        boolean appointmentAlreadyHasOrder = examinationOrders.values().stream()
                .anyMatch(order -> order.orderedAppointmentId()
                        .equals(nextExaminationOrder.orderedAppointmentId()));
        if (appointmentAlreadyHasOrder
                || examinationOrders.containsKey(nextExaminationOrder.orderId())) {
            throw new IllegalStateException("examination order already exists");
        }
        if (findExaminationReportByOrderId(reviewedOrder.orderId()).isEmpty()) {
            throw new IllegalStateException("reviewed examination report does not exist");
        }
        if (clinicalBills.containsKey(examinationBill.billId())) {
            throw new IllegalStateException("bill already exists");
        }
        consultations.put(consultation.consultationId(), consultation);
        bookings.put(consultation.appointmentId(), completedBooking);
        episodes.put(waitingEpisode.episodeId(), waitingEpisode);
        examinationOrders.put(reviewedOrder.orderId(), reviewedOrder);
        examinationOrders.put(nextExaminationOrder.orderId(), nextExaminationOrder);
        clinicalBills.put(examinationBill.billId(), examinationBill);
    }

    @Override
    public synchronized void saveExaminationReport(
            HospitalExaminationOrder reportedOrder,
            HospitalExaminationReport report,
            HospitalEpisode resultReadyEpisode) {
        HospitalExaminationOrder current = examinationOrders.get(reportedOrder.orderId());
        if (current == null || !report.orderId().equals(reportedOrder.orderId())) {
            throw new IllegalStateException("examination order does not exist or match");
        }
        if (!resultReadyEpisode.episodeId().equals(reportedOrder.episodeId())) {
            throw new IllegalArgumentException("report episode does not match");
        }
        if (findExaminationReportByOrderId(reportedOrder.orderId()).isPresent()) {
            throw new IllegalStateException("examination report already exists");
        }
        examinationOrders.put(reportedOrder.orderId(), reportedOrder);
        examinationReports.put(report.reportId(), report);
        episodes.put(resultReadyEpisode.episodeId(), resultReadyEpisode);
    }

    @Override
    public synchronized void saveResultReviewConsultation(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode completedEpisode,
            HospitalExaminationOrder reviewedOrder) {
        validateClinicalSave(consultation, completedBooking, completedEpisode);
        if (!reviewedOrder.episodeId().equals(completedEpisode.episodeId())) {
            throw new IllegalArgumentException("reviewed order episode does not match");
        }
        consultations.put(consultation.consultationId(), consultation);
        bookings.put(consultation.appointmentId(), completedBooking);
        episodes.put(completedEpisode.episodeId(), completedEpisode);
        examinationOrders.put(reviewedOrder.orderId(), reviewedOrder);
    }

    private void validateClinicalSave(
            HospitalConsultation consultation,
            HospitalBooking completedBooking,
            HospitalEpisode episode) {
        Objects.requireNonNull(consultation, "consultation must not be null");
        Objects.requireNonNull(completedBooking, "completedBooking must not be null");
        Objects.requireNonNull(episode, "episode must not be null");
        String appointmentId = consultation.appointmentId();
        if (!appointmentId.equals(completedBooking.appointment().appointmentId())
                || !completedBooking.appointment().episodeId().equals(episode.episodeId())) {
            throw new IllegalArgumentException("clinical aggregate does not match");
        }
        if (findConsultationByAppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("consultation already exists for appointment");
        }
        if (!bookings.containsKey(appointmentId)
                || !episodes.containsKey(episode.episodeId())) {
            throw new IllegalStateException("appointment or episode does not exist");
        }
    }

    private static void validateClinicalBill(
            HospitalPatientBill bill,
            HospitalBooking booking) {
        Objects.requireNonNull(bill, "bill must not be null");
        if (!bill.appointmentId().equals(booking.appointment().appointmentId())
                || !bill.patientUserId().equals(booking.appointment().patientUserId())
                || bill.billType() == HospitalBillType.REGISTRATION
                || bill.paymentStatus() != PaymentStatus.UNPAID) {
            throw new IllegalArgumentException("clinical bill does not match appointment");
        }
    }

    private static HospitalPatientBill registrationBill(HospitalBooking booking) {
        HospitalBill bill = booking.bill();
        return new HospitalPatientBill(
                bill.billId(),
                booking.appointment().appointmentId(),
                booking.appointment().patientUserId(),
                HospitalBillType.REGISTRATION,
                bill.paymentStatus(),
                bill.createdAt(),
                bill.paidAt(),
                bill.refundedAt(),
                booking.billItem());
    }

    private static HospitalSlot slot(
            String scheduleId,
            String departmentId,
            String departmentName,
            String doctorId,
            String doctorName,
            String doctorTitle,
            LocalDate date,
            int hour,
            int minute,
            int priceCents,
            int capacity,
            int bookedCount,
            boolean published) {
        LocalDateTime start = LocalDateTime.of(date, LocalTime.of(hour, minute));
        return new HospitalSlot(
                scheduleId,
                departmentId,
                departmentName,
                doctorId,
                doctorName,
                doctorTitle,
                start,
                start.plusMinutes(30),
                priceCents,
                capacity,
                bookedCount,
                published);
    }

    private void seedBookings(
            String scheduleId,
            int count,
            int priceCents,
            int patientNumberStart,
            LocalDateTime createdAt) {
        for (int queueNumber = 1; queueNumber <= count; queueNumber++) {
            int patientNumber = patientNumberStart + queueNumber - 1;
            String suffix = scheduleId + "-" + queueNumber;
            String appointmentId = "seed-appointment-" + suffix;
            String episodeId = "seed-episode-" + suffix;
            String billId = "seed-bill-" + suffix;
            HospitalAppointment appointment = new HospitalAppointment(
                    appointmentId,
                    "U-DEMO-PATIENT-" + String.format("%03d", patientNumber),
                    scheduleId,
                    queueNumber,
                    createdAt.plusMinutes(queueNumber),
                    null,
                    null,
                    AppointmentStatus.BOOKED,
                    VisitType.FIRST_VISIT,
                    episodeId,
                    null);
            HospitalSlot slot = findSlotById(scheduleId).orElseThrow();
            episodes.put(episodeId, new HospitalEpisode(
                    episodeId,
                    appointment.patientUserId(),
                    slot.departmentId(),
                    EpisodeStatus.IN_PROGRESS,
                    appointment.createdAt(),
                    null));
            HospitalBill bill = new HospitalBill(
                    billId,
                    appointmentId,
                    createdAt.plusMinutes(queueNumber),
                    createdAt.plusMinutes(queueNumber),
                    null,
                    PaymentStatus.PAID);
            HospitalBillItem billItem = new HospitalBillItem(
                    "seed-bill-item-" + suffix,
                    billId,
                    "挂号费",
                    1,
                    priceCents);
            bookings.put(appointmentId, new HospitalBooking(appointment, bill, billItem));
        }
    }
}
