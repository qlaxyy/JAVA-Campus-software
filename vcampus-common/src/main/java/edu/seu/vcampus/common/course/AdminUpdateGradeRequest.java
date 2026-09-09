package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 成绩录入或修改请求。
 *
 * 可由超级管理员或具有对应教学班权限的教师使用。
 */
public final class AdminUpdateGradeRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID =
        1L;

    private final String studentId;

    private final long enrollmentId;

    private final double usualScore;

    private final double finalExamScore;

    private final String reason;

    /**
     * 新成绩结构使用的构造器。
     */
    public AdminUpdateGradeRequest(
        String studentId,
        long enrollmentId,
        double usualScore,
        double finalExamScore,
        String reason) {

        this.studentId =
            Objects.requireNonNull(
                studentId);

        this.enrollmentId =
            enrollmentId;

        this.usualScore =
            usualScore;

        this.finalExamScore =
            finalExamScore;

        this.reason =
            reason == null
                ? ""
                : reason;
    }

    /**
     * 兼容原有单项成绩代码。
     *
     * 旧 score 同时作为平时成绩和期末成绩。
     */
    public AdminUpdateGradeRequest(
        String studentId,
        long enrollmentId,
        double score,
        String reason) {

        this(
            studentId,
            enrollmentId,
            score,
            score,
            reason);
    }

    public String getStudentId() {

        return studentId;
    }

    public long getEnrollmentId() {

        return enrollmentId;
    }

    public double getUsualScore() {

        return usualScore;
    }

    public double getFinalExamScore() {

        return finalExamScore;
    }

    /**
     * 兼容原有服务器代码。
     *
     * 后续服务器改造完成后不再使用此方法。
     */
    public double getScore() {

        return usualScore * 0.4
            + finalExamScore * 0.6;
    }

    public String getReason() {

        return reason;
    }
}
