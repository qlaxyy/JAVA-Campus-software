package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.student.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StudentService {

    private final StudentRepository repository;

    public StudentService() {
        this(new StudentMemoryRepository());
    }

    public StudentService(StudentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public Optional<StudentProfileDto> findById(String studentId) {
        return repository.findByStudentId(studentId);
    }

    public StudentProfileResponse getProfile(StudentProfileRequest req) {
        StudentProfileResponse resp = new StudentProfileResponse();
        if (req == null || req.getStudentId() == null || req.getStudentId().isBlank()) {
            resp.setFound(false);
            resp.setMessage("学号不能为空");
            return resp;
        }

        Optional<StudentProfileDto> profileOpt = repository.findByStudentId(req.getStudentId());
        if (profileOpt.isPresent()) {
            resp.setFound(true);
            resp.setProfile(profileOpt.get());
            resp.setMessage("获取学籍档案成功");
        } else {
            resp.setFound(false);
            resp.setMessage("未找到对应学生档案");
        }
        return resp;
    }

    public boolean updateProfile(StudentUpdateProfileRequest req) {
        if (req == null || req.getStudentId() == null) {
            return false;
        }
        return repository.updateProfile(req);
    }

    /**
     * 发起学籍异动申请（含严格的状态机校验与防重复提交机制）
     */
    public StatusChangeDto applyStatusChange(ApplyStatusChangeRequest req) {
        if (req == null || req.getStudentId() == null || req.getChangeType() == null) {
            throw new IllegalArgumentException("异动申请参数不完整");
        }

        String studentId = req.getStudentId();
        String changeType = req.getChangeType().trim();

        // 1. 核查学生是否存在
        StudentProfileDto student = repository.findByStudentId(studentId)
            .orElseThrow(() -> new IllegalArgumentException("未找到学号为 " + studentId + " 的学生档案"));

        String currentStatus = student.getAcademicStatus() != null ? student.getAcademicStatus().trim() : "在读";

        // 2. 终态不可逆检查
        if ("退学".equals(currentStatus) || "毕业".equals(currentStatus)) {
            throw new IllegalStateException("学生当前学籍为【" + currentStatus + "】状态，已离校，无法申请任何学籍异动");
        }

        // 3. 检查是否有未办结的待审核申请（防并发/防重复提交）
        List<StatusChangeDto> existingList = repository.listStatusChanges(studentId);
        boolean hasPending = existingList.stream()
            .anyMatch(c -> "待审核".equals(c.getAuditStatus()));
        if (hasPending) {
            throw new IllegalStateException("当前尚有待审核的异动申请未处理，请等待审批结果后再提交新申请");
        }

        // 4. 状态机前置约束校验
        if ("复学".equals(changeType)) {
            if (!"休学".equals(currentStatus)) {
                throw new IllegalStateException("申请复学失败：当前学生学籍状态为【" + currentStatus + "】，只有处于【休学】状态的学生方可办理复学");
            }
        } else if ("休学".equals(changeType)) {
            if (!"在读".equals(currentStatus)) {
                throw new IllegalStateException("申请休学失败：当前学生学籍状态为【" + currentStatus + "】，仅【在读】学生可申请休学");
            }
        } else if ("转专业".equals(changeType)) {
            if (!"在读".equals(currentStatus)) {
                throw new IllegalStateException("申请转专业失败：学生当前处于【" + currentStatus + "】状态，休学或非在读期间不得申请转专业");
            }
        } else if ("退学".equals(changeType)) {
            if (!"在读".equals(currentStatus) && !"休学".equals(currentStatus)) {
                throw new IllegalStateException("申请退学失败：当前状态【" + currentStatus + "】不支持退学办理");
            }
        }

        return repository.createStatusChange(req);
    }

    public List<StatusChangeDto> listStatusChanges(String studentId) {
        return repository.listStatusChanges(studentId);
    }

    public boolean auditStatusChange(Long changeId, boolean approved, String operator) {
        return repository.auditStatusChange(changeId, approved, operator);
    }
}
