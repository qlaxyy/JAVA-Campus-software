package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 管理员为指定学生强制退课的请求。
 */
public final class AdminForceDropCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    /**
     * 目标学生学号。
     */
    private final String studentId;

    /**
     * 需要退掉的选课记录编号。
     */
    private final long enrollmentId;

    /**
     * 强制退课原因。
     */
    private final String reason;

    public AdminForceDropCourseRequest(
        String studentId,
        long enrollmentId,
        String reason) {

        this.studentId =
            Objects.requireNonNull(
                studentId);

        this.enrollmentId =
            enrollmentId;

        this.reason =
            Objects.requireNonNull(
                reason);
    }

    public String getStudentId() {

        return studentId;
    }

    public long getEnrollmentId() {

        return enrollmentId;
    }

    public String getReason() {

        return reason;
    }
}
