package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.StudentProfileDto;
import edu.seu.vcampus.common.student.StudentProfileRequest;
import edu.seu.vcampus.common.student.StudentProfileResponse;
import edu.seu.vcampus.common.student.StudentUpdateProfileRequest;

import java.util.Optional;

public class StudentService {
    private final StudentMemoryRepository repository = new StudentMemoryRepository();

    public StudentProfileResponse getProfile(StudentProfileRequest req) {
        if (req == null || req.getStudentId() == null || req.getStudentId().trim().isEmpty()) {
            return new StudentProfileResponse(false, "学号不能为空", null);
        }

        Optional<StudentProfileDto> profile = repository.findById(req.getStudentId());
        if (profile.isPresent()) {
            return new StudentProfileResponse(true, "查询成功", profile.get());
        } else {
            return new StudentProfileResponse(false, "未检索到学号为 [" + req.getStudentId() + "] 的学籍档案", null);
        }
    }

    public boolean updateProfile(StudentUpdateProfileRequest req) {
        return repository.updateProfile(req);
    }
}
