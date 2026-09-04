package edu.seu.vcampus.common.student;

import java.io.Serializable;

public final class StatusChangeDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long changeId;          // 自增主键
    private String studentId;       // 学号
    private String studentName;     // 学生姓名
    private String changeType;      // 异动类型: 休学、复学、转专业、退学
    private String reason;          // 申请理由
    private String changeDate;      // 申请/生效日期
    private String auditStatus;     // 审核状态: 待审核、已通过、已驳回
    private String operator;        // 审核人账号

    public StatusChangeDto() {}

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getChangeDate() { return changeDate; }
    public void setChangeDate(String changeDate) { this.changeDate = changeDate; }

    public String getAuditStatus() { return auditStatus; }
    public void setAuditStatus(String auditStatus) { this.auditStatus = auditStatus; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
}
