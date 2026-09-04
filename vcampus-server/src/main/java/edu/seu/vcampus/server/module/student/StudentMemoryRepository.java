package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.ApplyStatusChangeRequest;
import edu.seu.vcampus.common.student.StatusChangeDto;
import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class StudentMemoryRepository {

    private final Map<String, StudentProfileDto> store = new ConcurrentHashMap<>();
    private final List<StatusChangeDto> statusChangeStore = new CopyOnWriteArrayList<>();
    private final AtomicLong changeIdGen = new AtomicLong(1);

    public StudentMemoryRepository() {
        initDefaultData();
    }

    private void initDefaultData() {
        // 学生 1：张三
        StudentProfileDto s1 = new StudentProfileDto();
        s1.setId(1L);
        s1.setStudentId("student001");
        s1.setName("张三");
        s1.setGender("男");
        s1.setEthnicity("汉族");
        s1.setNativePlace("江苏省南京市");
        s1.setIdCardNumber("320102200401011234");
        s1.setBirthDate("2004-01-01");
        s1.setEnrollmentDate("2024-09-01");
        s1.setEnrollmentYear(2024);
        s1.setDepartment("计算机科学与工程学院");
        s1.setMajor("软件工程");
        s1.setClassName("软件工程2401班");
        s1.setSchoolingLength(4);
        s1.setAcademicStatus("在读");
        s1.setPlanId(1L);
        s1.setCurrentTerm(1);
        s1.setCampusId(1L);
        s1.setPoliticalStatus("共青团员");
        s1.setPhone("13800138000");
        s1.setEmail("student001@seu.edu.cn");
        s1.setHomeAddress("江苏省南京市江宁区东南大学九龙湖校区");
        s1.setEmergencyContact("张父");
        s1.setEmergencyPhone("13900139000");
        store.put(normalize(s1.getStudentId()), s1);

        // 学生 2：李四
        StudentProfileDto s2 = new StudentProfileDto();
        s2.setId(2L);
        s2.setStudentId("student002");
        s2.setName("李四");
        s2.setGender("女");
        s2.setEthnicity("汉族");
        s2.setNativePlace("江苏省苏州市");
        s2.setIdCardNumber("320502200405055678");
        s2.setBirthDate("2004-05-05");
        s2.setEnrollmentDate("2024-09-01");
        s2.setEnrollmentYear(2024);
        s2.setDepartment("计算机科学与工程学院");
        s2.setMajor("软件工程");
        s2.setClassName("软件工程2401班");
        s2.setSchoolingLength(4);
        s2.setAcademicStatus("在读");
        s2.setPlanId(1L);
        s2.setCurrentTerm(1);
        s2.setCampusId(1L);
        s2.setPoliticalStatus("群众");
        s2.setPhone("13800138001");
        s2.setEmail("student002@seu.edu.cn");
        s2.setHomeAddress("江苏省南京市江宁区东南大学九龙湖校区");
        s2.setEmergencyContact("李母");
        s2.setEmergencyPhone("13900139001");
        store.put(normalize(s2.getStudentId()), s2);
    }

    public String normalize(String id) {
        if (id == null) return "";
        String clean = id.trim().toLowerCase();
        if (clean.startsWith("u-")) {
            clean = clean.substring(2);
        }
        return clean.replace("-", "");
    }

    public Optional<StudentProfileDto> findById(String studentId) {
        return findByStudentId(studentId);
    }

    public Optional<StudentProfileDto> findByStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(normalize(studentId)));
    }

    public boolean updateProfile(StudentUpdateProfileRequest req) {
        if (req == null || req.getStudentId() == null) return false;
        StudentProfileDto profile = store.get(normalize(req.getStudentId()));
        if (profile == null) return false;

        if (req.getPoliticalStatus() != null) profile.setPoliticalStatus(req.getPoliticalStatus());
        if (req.getPhone() != null) profile.setPhone(req.getPhone());
        if (req.getEmail() != null) profile.setEmail(req.getEmail());
        if (req.getHomeAddress() != null) profile.setHomeAddress(req.getHomeAddress());
        if (req.getEmergencyContact() != null) profile.setEmergencyContact(req.getEmergencyContact());
        if (req.getEmergencyPhone() != null) profile.setEmergencyPhone(req.getEmergencyPhone());

        return true;
    }

    // ================= 异动相关方法 =================

    public StatusChangeDto createStatusChange(ApplyStatusChangeRequest req) {
        String key = normalize(req.getStudentId());
        StudentProfileDto profile = store.get(key);
        if (profile == null) return null;

        StatusChangeDto change = new StatusChangeDto();
        change.setChangeId(changeIdGen.getAndIncrement());
        change.setStudentId(profile.getStudentId());
        change.setStudentName(profile.getName());
        change.setChangeType(req.getChangeType());
        change.setReason(req.getReason());
        change.setChangeDate(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        change.setAuditStatus("待审核");
        change.setOperator("-");

        statusChangeStore.add(0, change); // 倒序插入
        return change;
    }

    public List<StatusChangeDto> listStatusChanges(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return new ArrayList<>(statusChangeStore);
        }
        String target = normalize(studentId);
        List<StatusChangeDto> result = new ArrayList<>();
        for (StatusChangeDto item : statusChangeStore) {
            if (normalize(item.getStudentId()).equals(target)) {
                result.add(item);
            }
        }
        return result;
    }

    public boolean auditStatusChange(Long changeId, boolean approved, String operator) {
        for (StatusChangeDto item : statusChangeStore) {
            if (Objects.equals(item.getChangeId(), changeId)) {
                if (!"待审核".equals(item.getAuditStatus())) {
                    return false; // 已审核过
                }
                item.setAuditStatus(approved ? "已通过" : "已驳回");
                item.setOperator(operator);

                // 若审核通过，联动修改学生主档案的学籍状态
                if (approved) {
                    StudentProfileDto student = store.get(normalize(item.getStudentId()));
                    if (student != null) {
                        if ("休学".equals(item.getChangeType())) {
                            student.setAcademicStatus("休学");
                        } else if ("复学".equals(item.getChangeType())) {
                            student.setAcademicStatus("在读");
                        } else if ("退学".equals(item.getChangeType())) {
                            student.setAcademicStatus("退学");
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}
