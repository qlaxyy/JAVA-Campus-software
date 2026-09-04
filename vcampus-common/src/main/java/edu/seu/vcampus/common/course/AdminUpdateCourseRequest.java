package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教务修改课程基本信息请求。
 */
public final class AdminUpdateCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final double credits;
    private final String courseType;
    private final String reason;

    public AdminUpdateCourseRequest(
        long batchId,
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        String reason) {

        this.batchId =
            batchId;

        this.courseId =
            courseId;

        this.courseCode =
            Objects.requireNonNull(
                courseCode);

        this.courseName =
            Objects.requireNonNull(
                courseName);

        this.credits =
            credits;

        this.courseType =
            Objects.requireNonNull(
                courseType);

        this.reason =
            reason == null
                ? ""
                : reason;
    }

    public long getBatchId() {

        return batchId;
    }

    public long getCourseId() {

        return courseId;
    }

    public String getCourseCode() {

        return courseCode;
    }

    public String getCourseName() {

        return courseName;
    }

    public double getCredits() {

        return credits;
    }

    public String getCourseType() {

        return courseType;
    }

    public String getReason() {

        return reason;
    }
}
