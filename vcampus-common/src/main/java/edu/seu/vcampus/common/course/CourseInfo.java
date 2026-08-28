package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 学生选课页面中的课程信息。
 */
public final class CourseInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final double credits;
    private final String courseType;

    private final boolean selected;

    private final List<OfferingInfo> offerings;

    public CourseInfo(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        boolean selected,
        List<OfferingInfo> offerings) {

        this.courseId = courseId;
        this.courseCode = Objects.requireNonNull(courseCode);
        this.courseName = Objects.requireNonNull(courseName);
        this.credits = credits;
        this.courseType = Objects.requireNonNull(courseType);
        this.selected = selected;
        this.offerings = List.copyOf(offerings);
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

    public boolean isSelected() {
        return selected;
    }

    public List<OfferingInfo> getOfferings() {
        return offerings;
    }
}
