package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.*;

import java.util.List;
import java.util.Optional;

public final class StudentService {

    private final StudentMemoryRepository repository;

    public StudentService() {
        this.repository = new StudentMemoryRepository();
    }

    public StudentService(StudentMemoryRepository repository) {
        this.repository = repository;
    }

    public Optional<StudentProfileDto> findById(String studentId) {
        return repository.findByStudentId(studentId);
    }

    public StudentProfileResponse getProfile(StudentProfileRequest req) {
        StudentProfileResponse resp = new StudentProfileResponse();
        if (req == null || req.getStudentId() == null) {
            resp.setFound(false);
            resp.setMessage("学号不能为空");
            return resp;
        }
        Optional<StudentProfileDto> opt = repository.findByStudentId(req.getStudentId());
        if (opt.isPresent()) {
            resp.setFound(true);
            resp.setProfile(opt.get());
        } else {
            resp.setFound(false);
            resp.setMessage("未查询到对应学籍档案");
        }
        return resp;
    }

    public boolean updateProfile(StudentUpdateProfileRequest req) {
        return repository.updateProfile(req);
    }

    public StatusChangeDto applyStatusChange(ApplyStatusChangeRequest req) {
        return repository.createStatusChange(req);
    }

    public List<StatusChangeDto> listStatusChanges(String studentId) {
        return repository.listStatusChanges(studentId);
    }

    public boolean auditStatusChange(Long changeId, boolean approved, String operator) {
        return repository.auditStatusChange(changeId, approved, operator);
    }
}
