package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 管理员为指定学生强制选课的请求。
 */
public final class AdminForceSelectCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    /**
     * 目标学生学号。
     */
    private final String studentId;

    /**
     * 选课批次编号。
     */
    private final long batchId;

    /**
     * 教学班编号。
     */
    private final long offeringId;

    /**
     * 强制选课原因。
     */
    private final String reason;

    public AdminForceSelectCourseRequest(
        String studentId,
        long batchId,
        long offeringId,
        String reason) {

        this.studentId =
            Objects.requireNonNull(
                studentId);

        this.batchId =
            batchId;

        this.offeringId =
            offeringId;

        this.reason =
            Objects.requireNonNull(
                reason);
    }

    public String getStudentId() {

        return studentId;
    }

    public long getBatchId() {

        return batchId;
    }

    public long getOfferingId() {

        return offeringId;
    }

    public String getReason() {

        return reason;
    }
}
