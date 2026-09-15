package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.ApplyStatusChangeRequest;
import edu.seu.vcampus.common.student.StatusChangeDto;
import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory student repository implementation for testing and fallback use. */
public final class StudentMemoryRepository implements StudentRepository {

    private final Map<String, StudentProfileDto> profiles = new ConcurrentHashMap<>();
    private final List<StatusChangeDto> statusChanges = new ArrayList<>();
    private final AtomicLong changeIdSequence = new AtomicLong(1L);

    public StudentMemoryRepository() {
        StudentProfileDto defaultStudent = new StudentProfileDto();
        defaultStudent.setId(1001L);
        defaultStudent.setStudentId("20260006");
        defaultStudent.setName("测试学生");
        defaultStudent.setGender("男");
        defaultStudent.setDepartment("计算机科学与工程学院");
        defaultStudent.setMajor("计算机科学与技术");
        defaultStudent.setClassName("计科2601班");
        defaultStudent.setEnrollmentYear(2026);
        defaultStudent.setAcademicStatus("在读");
        defaultStudent.setPoliticalStatus("共青团员");
        defaultStudent.setPhone("13800000000");
        defaultStudent.setEmail("20260006@seu.edu.cn");
        defaultStudent.setHomeAddress("江苏省南京市江宁区");
        defaultStudent.setEmergencyContact("家长");
        defaultStudent.setEmergencyPhone("13900000000");
        profiles.put("20260006", defaultStudent);
    }

    @Override
    public Optional<StudentProfileDto> findByStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(profiles.get(studentId.trim()));
    }

    @Override
    public boolean updateProfile(StudentUpdateProfileRequest request) {
        if (request == null || request.getStudentId() == null) {
            return false;
        }
        StudentProfileDto profile = profiles.get(request.getStudentId().trim());
        if (profile == null) {
            return false;
        }
        if (request.getPoliticalStatus() != null) {
            profile.setPoliticalStatus(request.getPoliticalStatus());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getHomeAddress() != null) {
            profile.setHomeAddress(request.getHomeAddress());
        }
        if (request.getEmergencyContact() != null) {
            profile.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getEmergencyPhone() != null) {
            profile.setEmergencyPhone(request.getEmergencyPhone());
        }
        return true;
    }

    @Override
    public boolean addStudent(StudentProfileDto profile) {
        if (profile == null || profile.getStudentId() == null || profile.getStudentId().isBlank()) {
            return false;
        }
        String key = profile.getStudentId().trim();
        if (profiles.containsKey(key)) {
            return false;
        }
        profiles.put(key, profile);
        return true;
    }

    @Override
    public boolean deleteStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return false;
        }
        return profiles.remove(studentId.trim()) != null;
    }

    @Override
    public StatusChangeDto createStatusChange(ApplyStatusChangeRequest request) {
        if (request == null || request.getStudentId() == null) {
            return null;
        }
        StudentProfileDto profile = profiles.get(request.getStudentId().trim());
        if (profile == null) {
            return null;
        }
        StatusChangeDto dto = new StatusChangeDto();
        dto.setChangeId(changeIdSequence.getAndIncrement());
        dto.setStudentId(profile.getStudentId());
        dto.setStudentName(profile.getName());
        dto.setChangeType(request.getChangeType());
        dto.setReason(request.getReason());
        dto.setChangeDate("2026-09-14 12:00");
        dto.setAuditStatus("待审核");
        dto.setOperator("-");
        statusChanges.add(dto);
        return dto;
    }

    @Override
    public List<StatusChangeDto> listStatusChanges(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return List.copyOf(statusChanges);
        }
        List<StatusChangeDto> filtered = new ArrayList<>();
        for (StatusChangeDto dto : statusChanges) {
            if (dto.getStudentId().equalsIgnoreCase(studentId.trim())) {
                filtered.add(dto);
            }
        }
        return List.copyOf(filtered);
    }

    @Override
    public boolean auditStatusChange(Long changeId, boolean approved, String operator) {
        if (changeId == null) {
            return false;
        }
        for (StatusChangeDto dto : statusChanges) {
            if (dto.getChangeId().equals(changeId) && "待审核".equals(dto.getAuditStatus())) {
                dto.setAuditStatus(approved ? "已通过" : "已驳回");
                dto.setOperator(operator != null ? operator : "管理员");

                if (approved) {
                    StudentProfileDto profile = profiles.get(dto.getStudentId());
                    if (profile != null) {
                        String type = dto.getChangeType();
                        if ("休学".equals(type)) {
                            profile.setAcademicStatus("休学");
                        } else if ("复学".equals(type)) {
                            profile.setAcademicStatus("在读");
                        } else if ("退学".equals(type)) {
                            profile.setAcademicStatus("退学");
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}
