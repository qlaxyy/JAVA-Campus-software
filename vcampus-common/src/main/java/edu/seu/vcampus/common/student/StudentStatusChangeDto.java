package edu.seu.vcampus.common.student;

import java.io.Serializable;

public class StudentStatusChangeDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long changeId;
    private String studentId;
    private String changeType; // 转专业、休学、复学、基本信息变更
    private String reason;     // 申请理由或变更详情描述
    private String targetMajor; // 信息变更时存：修改字段键（如 name, idCard, nativePlace）
    private String targetClass; // 信息变更时存：申请的新值
    private String status;      // 待审核、已通过、已驳回
    private String applyDate;
    private String reviewComments;
    private String reviewerUserId;
    private String reviewDate;

    public StudentStatusChangeDto() {}

    public StudentStatusChangeDto(Long changeId, String studentId, String changeType, String reason,
                                  String targetMajor, String targetClass, String status,
                                  String applyDate, String reviewComments, String reviewerUserId, String reviewDate) {
        this.changeId = changeId;
        this.studentId = studentId;
        this.changeType = changeType;
        this.reason = reason;
        this.targetMajor = targetMajor;
        this.targetClass = targetClass;
        this.status = status;
        this.applyDate = applyDate;
        this.reviewComments = reviewComments;
        this.reviewerUserId = reviewerUserId;
        this.reviewDate = reviewDate;
    }

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTargetMajor() { return targetMajor; }
    public void setTargetMajor(String targetMajor) { this.targetMajor = targetMajor; }

    public String getTargetClass() { return targetClass; }
    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApplyDate() { return applyDate; }
    public void setApplyDate(String applyDate) { this.applyDate = applyDate; }

    public String getReviewComments() { return reviewComments; }
    public void setReviewComments(String reviewComments) { this.reviewComments = reviewComments; }

    public String getReviewerUserId() { return reviewerUserId; }
    public void setReviewerUserId(String reviewerUserId) { this.reviewerUserId = reviewerUserId; }

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
}
