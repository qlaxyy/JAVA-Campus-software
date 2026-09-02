package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 全校课程查询中的一条教学班记录。
 *
 * 注意：
 *
 * 全校课程查询是以“教学班”为一行，
 * 而不是以 CourseInfo 为一行。
 */
public final class CourseSearchItem
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long courseId;
    private final String courseCode;
    private final String courseName;
    private final double credits;
    private final String courseType;

    /**
     * 开课院系。
     */
    private final String departmentName;

    private final long offeringId;
    private final String classNo;
    private final List<String> teacherNames;
    private final List<ScheduleInfo> schedules;
    private final String locationName;
    private final String campusName;
    private final String teachingLanguage;

    private final int selectedCount;
    private final int capacity;
    private final int remainingCount;

    public CourseSearchItem(
        long courseId,
        String courseCode,
        String courseName,
        double credits,
        String courseType,
        String departmentName,
        long offeringId,
        String classNo,
        List<String> teacherNames,
        List<ScheduleInfo> schedules,
        String locationName,
        String campusName,
        String teachingLanguage,
        int selectedCount,
        int capacity,
        int remainingCount) {

        this.courseId = courseId;
        this.courseCode =
            Objects.requireNonNull(courseCode);

        this.courseName =
            Objects.requireNonNull(courseName);

        this.credits = credits;

        this.courseType =
            Objects.requireNonNull(courseType);

        this.departmentName =
            Objects.requireNonNull(departmentName);

        this.offeringId = offeringId;

        this.classNo =
            Objects.requireNonNull(classNo);

        this.teacherNames =
            List.copyOf(
                Objects.requireNonNull(
                    teacherNames));

        this.schedules =
            List.copyOf(
                Objects.requireNonNull(
                    schedules));

        this.locationName =
            locationName;

        this.campusName =
            campusName;

        this.teachingLanguage =
            teachingLanguage;

        this.selectedCount =
            selectedCount;

        this.capacity =
            capacity;

        this.remainingCount =
            remainingCount;
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

    public long getOfferingId() {
        return offeringId;
    }

    public String getClassNo() {
        return classNo;
    }

    public List<String> getTeacherNames() {
        return teacherNames;
    }

    public List<ScheduleInfo> getSchedules() {
        return schedules;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getCampusName() {
        return campusName;
    }

    public String getTeachingLanguage() {
        return teachingLanguage;
    }

    public int getSelectedCount() {
        return selectedCount;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRemainingCount() {
        return remainingCount;
    }
}
