package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 超级管理员修改学生成绩请求。
 */
public final class AdminUpdateGradeRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String studentId;
    private final long enrollmentId;
    private final double score;
    private final String reason;

    public AdminUpdateGradeRequest(
        String studentId,
        long enrollmentId,
        double score,
        String reason) {

        this.studentId =
            Objects.requireNonNull(
                studentId);

        this.enrollmentId =
            enrollmentId;

        this.score =
            score;

        this.reason =
            reason == null
                ? ""
                : reason;
    }

    public String getStudentId() {

        return studentId;
    }

    public long getEnrollmentId() {

        return enrollmentId;
    }

    public double getScore() {

        return score;
    }

    public String getReason() {

        return reason;
    }
}
