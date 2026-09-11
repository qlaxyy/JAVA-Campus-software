package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教务修改课程基本信息请求。
 *
 * 课程基本信息是全局数据，不属于选课批次。
 */
public final class AdminUpdateCourseRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final double credits;
    private final String courseType;
    private final String reason;

    public AdminUpdateCourseRequest(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        String reason) {

        if (courseId <= 0) {

            throw new IllegalArgumentException(
                "courseId must be positive");
        }

        if (credits <= 0) {

            throw new IllegalArgumentException(
                "credits must be positive");
        }

        this.courseId =
            courseId;

        this.courseCode =
            requireText(
                courseCode,
                "courseCode");

        this.courseName =
            requireText(
                courseName,
                "courseName");

        this.credits =
            credits;

        this.courseType =
            requireText(
                courseType,
                "courseType");

        this.reason =
            reason == null
                ? ""
                : reason.trim();
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

    private static String requireText(
        String value,
        String fieldName) {

        Objects.requireNonNull(
            value,
            fieldName + " must not be null");

        String normalized =
            value.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                fieldName + " must not be blank");
        }

        return normalized;
    }
}
