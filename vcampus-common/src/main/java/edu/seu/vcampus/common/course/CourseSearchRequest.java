package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全校课程查询请求。
 */
public final class CourseSearchRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String courseCode;
    private final String courseName;
    private final String teacherName;
    private final String departmentName;

    /**
     * ALL、AVAILABLE、FULL
     */
    private final String availability;

    private final int page;
    private final int pageSize;

    public CourseSearchRequest(
        String courseCode,
        String courseName,
        String teacherName,
        String departmentName,
        String availability,
        int page,
        int pageSize) {

        this.courseCode = normalize(courseCode);
        this.courseName = normalize(courseName);
        this.teacherName = normalize(teacherName);
        this.departmentName = normalize(departmentName);

        this.availability =
            availability == null || availability.isBlank()
                ? "ALL"
                : availability.trim();

        if (page <= 0) {
            throw new IllegalArgumentException(
                "page must be positive");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException(
                "pageSize must be positive");
        }

        this.page = page;
        this.pageSize = pageSize;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getAvailability() {
        return availability;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
