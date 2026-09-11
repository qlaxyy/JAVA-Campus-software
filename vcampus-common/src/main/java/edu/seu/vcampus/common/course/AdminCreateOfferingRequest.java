package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 教务为已有课程新建教学班的请求。
 */
public final class AdminCreateOfferingRequest
    implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long batchId;
    private final long courseId;
    private final String classNo;
    private final String locationName;
    private final String campusName;
    private final String teachingLanguage;
    private final int capacity;
    private final ScheduleInfo schedule;
    private final String reason;

    public AdminCreateOfferingRequest(
        long batchId,
        long courseId,
        String classNo,
        String locationName,
        String campusName,
        String teachingLanguage,
        int capacity,
        ScheduleInfo schedule,
        String reason) {

        if (batchId <= 0 || courseId <= 0) {
            throw new IllegalArgumentException(
                "batchId and courseId must be positive");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                "capacity must be positive");
        }

        this.batchId = batchId;
        this.courseId = courseId;
        this.classNo = requireText(classNo, "classNo");
        this.locationName = requireText(
            locationName,
            "locationName");
        this.campusName = requireText(
            campusName,
            "campusName");
        this.teachingLanguage = requireText(
            teachingLanguage,
            "teachingLanguage");
        this.capacity = capacity;
        this.schedule = Objects.requireNonNull(
            schedule,
            "schedule must not be null");
        this.reason = requireText(reason, "reason");
    }

    public long getBatchId() {
        return batchId;
    }

    public long getCourseId() {
        return courseId;
    }

    public String getClassNo() {
        return classNo;
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

    public int getCapacity() {
        return capacity;
    }

    public ScheduleInfo getSchedule() {
        return schedule;
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

        String cleaned = value.trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException(
                fieldName + " must not be blank");
        }
        return cleaned;
    }
}
