package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.StudentProfileRequest;
import edu.seu.vcampus.common.student.StudentProfileResponse;

/**
 * 学籍业务逻辑处理服务
 */
public class StudentService {
    private final StudentMemoryRepository repository = new StudentMemoryRepository();

    public StudentProfileResponse getProfile(StudentProfileRequest req) {
        if (req == null || req.getStudentId() == null || req.getStudentId().isBlank()) {
            return StudentProfileResponse.notFound("学号不能为空");
        }
        return repository.findById(req.getStudentId())
            .map(StudentProfileResponse::success)
            .orElseGet(() -> StudentProfileResponse.notFound("未检索到学号为 [" + req.getStudentId() + "] 的学籍档案"));
    }
}
