package edu.seu.vcampus.common.student;

import java.io.Serializable;

public final class ApplyStatusChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String changeType;
    private String reason;

    public ApplyStatusChangeRequest() {}

    public ApplyStatusChangeRequest(String studentId, String changeType, String reason) {
        this.studentId = studentId;
        this.changeType = changeType;
        this.reason = reason;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
