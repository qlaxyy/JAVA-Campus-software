package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 课程及其教学班信息。
 */
public final class CourseInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final double credits;
    private final String courseType;
    private final String departmentName;
    private final boolean selected;
    private final List<OfferingInfo> offerings;

    /**
     * 兼容现有代码的旧构造器。
     */
    public CourseInfo(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        boolean selected,
        List<OfferingInfo> offerings) {

        this(
            courseId,
            courseCode,
            courseName,
            credits,
            courseType,
            "未设置",
            selected,
            offerings);
    }

    /**
     * 包含真实开课院系的构造器。
     */
    public CourseInfo(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        String departmentName,
        boolean selected,
        List<OfferingInfo> offerings) {

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

        this.departmentName =
            requireText(
                departmentName,
                "departmentName");

        this.selected =
            selected;

        this.offerings =
            List.copyOf(
                Objects.requireNonNull(
                    offerings));
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

    public String getDepartmentName() {

        return departmentName;
    }

    public boolean isSelected() {

        return selected;
    }

    public List<OfferingInfo> getOfferings() {

        return offerings;
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
