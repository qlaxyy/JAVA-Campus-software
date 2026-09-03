package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class StudentMemoryRepository {
    private final Map<String, StudentProfileDto> store = new ConcurrentHashMap<>();

    public StudentMemoryRepository() {
        initDefaultData();
    }

    private void initDefaultData() {
        StudentProfileDto s1 = new StudentProfileDto(
            "student001", "张三", "男", "320102200401011234",
            "2004-01-01", "汉族", "江苏南京", "共青团员",
            "计算机科学与工程学院", "软件工程", "软工01班",
            "2022", "本科生", "在读",
            "13800138000", "zhangsan@seu.edu.cn", "江苏省南京市江宁区东南大学九龙湖校区",
            "张父", "13900139000"
        );
        store.put(s1.getStudentId(), s1);
    }

    public Optional<StudentProfileDto> findById(String studentId) {
        if (studentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(studentId.trim()));
    }

    public boolean updateProfile(StudentUpdateProfileRequest req) {
        if (req == null || req.getStudentId() == null) {
            return false;
        }
        StudentProfileDto p = store.get(req.getStudentId().trim());
        if (p == null) {
            return false;
        }
        if (req.getPoliticalStatus() != null) p.setPoliticalStatus(req.getPoliticalStatus().trim());
        if (req.getPhone() != null) p.setPhone(req.getPhone().trim());
        if (req.getEmail() != null) p.setEmail(req.getEmail().trim());
        if (req.getHomeAddress() != null) p.setHomeAddress(req.getHomeAddress().trim());
        if (req.getEmergencyContact() != null) p.setEmergencyContact(req.getEmergencyContact().trim());
        if (req.getEmergencyPhone() != null) p.setEmergencyPhone(req.getEmergencyPhone().trim());
        return true;
    }
}
