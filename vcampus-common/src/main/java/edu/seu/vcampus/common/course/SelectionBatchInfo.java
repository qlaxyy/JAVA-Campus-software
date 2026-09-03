package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 学生端显示的选课批次信息。
 */
public final class SelectionBatchInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final String semester;
    private final String batchName;
    private final SelectionBatchType batchType;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final SelectionBatchStatus status;
    private final boolean allowSelect;
    private final boolean allowDrop;

    public SelectionBatchInfo(
        long batchId,
        String semester,
        String batchName,
        SelectionBatchType batchType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SelectionBatchStatus status,
        boolean allowSelect,
        boolean allowDrop) {

        this.batchId = batchId;
        this.semester = Objects.requireNonNull(semester);
        this.batchName = Objects.requireNonNull(batchName);
        this.batchType = Objects.requireNonNull(batchType);
        this.startTime = Objects.requireNonNull(startTime);
        this.endTime = Objects.requireNonNull(endTime);
        this.status = Objects.requireNonNull(status);
        this.allowSelect = allowSelect;
        this.allowDrop = allowDrop;
    }

    public long getBatchId() {
        return batchId;
    }

    public String getSemester() {
        return semester;
    }

    public String getBatchName() {
        return batchName;
    }

    public SelectionBatchType getBatchType() {
        return batchType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public SelectionBatchStatus getStatus() {
        return status;
    }

    public boolean isAllowSelect() {
        return allowSelect;
    }

    public boolean isAllowDrop() {
        return allowDrop;
    }
}
