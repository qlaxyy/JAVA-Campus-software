package edu.seu.vcampus.server.module.hospital;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Deterministic fake hospital data used before the Access repository is available. */
final class InMemoryHospitalRepository implements HospitalRepository {

    private final Map<String, DoctorProfile> doctorProfiles = new ConcurrentHashMap<>();
    private final Map<String, DoctorApplication> doctorApplications =
            new ConcurrentHashMap<>();
    private final List<HospitalDepartment> departments;
    private final List<HospitalSlot> slots;

    InMemoryHospitalRepository(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        LocalDate today = LocalDate.now(clock);
        departments = List.of(
                new HospitalDepartment("dept-general", "全科门诊", true),
                new HospitalDepartment("dept-psychology", "心理咨询", true),
                new HospitalDepartment("dept-dental", "口腔科", true),
                new HospitalDepartment("dept-eye", "眼科", true));
        doctorProfiles.put("U-TEACHER-001",
                new DoctorProfile(
                        "U-TEACHER-001", "dept-general", "演示医生", true));
        slots = List.of(
                slot("slot-general-1", "dept-general", "全科门诊",
                        "doctor-chen", "陈医生", "主治医师",
                        today.plusDays(1), 8, 30, 1_200, 12, 4, true),
                slot("slot-general-2", "dept-general", "全科门诊",
                        "doctor-chen", "陈医生", "主治医师",
                        today.plusDays(3), 14, 0, 1_200, 10, 10, true),
                slot("slot-general-3", "dept-general", "全科门诊",
                        "doctor-liu", "刘医生", "副主任医师",
                        today.plusDays(2), 9, 30, 1_800, 8, 2, true),
                slot("slot-psychology-1", "dept-psychology", "心理咨询",
                        "doctor-zhang", "张医生", "主治医师",
                        today.plusDays(1), 9, 0, 2_000, 6, 2, true),
                slot("slot-psychology-2", "dept-psychology", "心理咨询",
                        "doctor-wang", "王医生", "副主任医师",
                        today.plusDays(4), 15, 0, 2_600, 5, 5, true),
                slot("slot-dental-1", "dept-dental", "口腔科",
                        "doctor-zhao", "赵医生", "主治医师",
                        today.plusDays(2), 10, 0, 1_500, 8, 3, true),
                slot("slot-dental-2", "dept-dental", "口腔科",
                        "doctor-zhao", "赵医生", "主治医师",
                        today.plusDays(6), 13, 30, 1_500, 8, 1, true),
                slot("slot-eye-1", "dept-eye", "眼科",
                        "doctor-sun", "孙医生", "主任医师",
                        today.plusDays(5), 8, 30, 2_200, 10, 6, true),
                slot("slot-eye-closed", "dept-eye", "眼科",
                        "doctor-sun", "孙医生", "主任医师",
                        today.plusDays(3), 8, 30, 2_200, 10, 1, false));
    }

    @Override
    public boolean isActiveDoctorUser(String userId) {
        DoctorProfile profile = userId == null ? null : doctorProfiles.get(userId);
        return profile != null && profile.active();
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
    public void saveDoctorProfile(DoctorProfile profile) {
        doctorProfiles.put(profile.userId(), profile);
    }

    @Override
    public List<HospitalDepartment> findActiveDepartments() {
        return departments.stream().filter(HospitalDepartment::active).toList();
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
}
