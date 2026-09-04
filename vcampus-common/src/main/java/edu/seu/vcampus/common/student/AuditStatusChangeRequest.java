package edu.seu.vcampus.common.student;

import java.io.Serializable;

public final class AuditStatusChangeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long changeId;
    private boolean approved;   // true: 通过, false: 驳回

    public AuditStatusChangeRequest() {}

    public AuditStatusChangeRequest(Long changeId, boolean approved) {
        this.changeId = changeId;
        this.approved = approved;
    }

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}
