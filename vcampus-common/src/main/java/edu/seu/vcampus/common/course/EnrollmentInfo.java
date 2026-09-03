package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前学生已选课程信息。
 */
public final class EnrollmentInfo
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long enrollmentId;

    private final long offeringId;

    private final String courseCode;

    private final String courseName;

    private final String classNo;

    private final List<String> teacherNames;

    private final List<ScheduleInfo> schedules;

    private final String locationName;

    private final double credits;

    private final String courseType;

    private final boolean canDrop;

    private final String dropUnavailableReason;

    public EnrollmentInfo(
        long enrollmentId,
        long offeringId,
        String courseCode,
        String courseName,
        String classNo,
        List<String> teacherNames,
        List<ScheduleInfo> schedules,
        String locationName,
        double credits,
        String courseType,
        boolean canDrop,
        String dropUnavailableReason) {

        this.enrollmentId = enrollmentId;
        this.offeringId = offeringId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.classNo = classNo;
        this.teacherNames =
            List.copyOf(teacherNames);
        this.schedules =
            List.copyOf(schedules);
        this.locationName = locationName;
        this.credits = credits;
        this.courseType = courseType;
        this.canDrop = canDrop;
        this.dropUnavailableReason =
            dropUnavailableReason;
    }

    public long getEnrollmentId() {
        return enrollmentId;
    }

    public long getOfferingId() {
        return offeringId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
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

    public double getCredits() {
        return credits;
    }

    public String getCourseType() {
        return courseType;
    }

    public boolean isCanDrop() {
        return canDrop;
    }

    public String getDropUnavailableReason() {
        return dropUnavailableReason;
    }
}
