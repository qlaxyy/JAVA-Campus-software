package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 教务修改选课批次请求。
 */
public final class AdminUpdateBatchRequest
    implements Serializable {

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
    private final String reason;

    public AdminUpdateBatchRequest(
        long batchId,
        String semester,
        String batchName,
        SelectionBatchType batchType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SelectionBatchStatus status,
        boolean allowSelect,
        boolean allowDrop,
        String reason) {

        this.batchId =
            batchId;

        this.semester =
            Objects.requireNonNull(
                semester);

        this.batchName =
            Objects.requireNonNull(
                batchName);

        this.batchType =
            Objects.requireNonNull(
                batchType);

        this.startTime =
            Objects.requireNonNull(
                startTime);

        this.endTime =
            Objects.requireNonNull(
                endTime);

        this.status =
            Objects.requireNonNull(
                status);

        this.allowSelect =
            allowSelect;

        this.allowDrop =
            allowDrop;

        this.reason =
            reason == null
                ? ""
                : reason;
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

    public String getReason() {

        return reason;
    }
}
