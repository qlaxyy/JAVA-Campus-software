package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学生退课请求。
 */
public final class DropCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final long enrollmentId;

    public DropCourseRequest(
        long batchId,
        long enrollmentId) {

        if (batchId <= 0) {
            throw new IllegalArgumentException(
                "batchId must be positive");
        }

        if (enrollmentId <= 0) {
            throw new IllegalArgumentException(
                "enrollmentId must be positive");
        }

        this.batchId = batchId;
        this.enrollmentId = enrollmentId;
    }

    public long getBatchId() {
        return batchId;
    }

    public long getEnrollmentId() {
        return enrollmentId;
    }
}
