package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotAvailability;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.AdminScope;
import edu.seu.vcampus.common.user.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

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
                session("U-TEACHER-001", Role.USER));
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
    void listsOnlyActiveDepartmentsInDisplayOrder() {
        DepartmentListResponse response = service.listDepartments();

        assertEquals(4, response.getDepartments().size());
        assertEquals(
                java.util.List.of("全科门诊", "口腔科", "心理咨询", "眼科"),
                response.getDepartments().stream()
                        .map(department -> department.getDepartmentName())
                        .toList());
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
    void hidesUnpublishedSlots() {
        SlotListResponse response = service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-eye", null));

        assertEquals(1, response.getSlots().size());
        assertEquals("slot-eye-1", response.getSlots().getFirst().getScheduleId());
    }

    @Test
    void rejectsUnknownDepartmentAndUnimplementedFollowUp() {
        assertThrows(IllegalArgumentException.class, () -> service.searchSlots(
                SearchSlotsRequest.firstVisit("dept-missing", null)));
        assertThrows(IllegalArgumentException.class, () -> service.searchSlots(
                SearchSlotsRequest.followUp("consultation-1")));
    }

    private static SessionInfo session(String userId, Role role) {
        return new SessionInfo("token-" + userId, userId, "demo", "演示用户", role);
    }
    private static SessionInfo session(String userId, Role role, Set<AdminScope> scopes) {
        return new SessionInfo(
                "token-" + userId, userId, "demo", "演示用户", role, scopes);
    }
}
