package edu.seu.vcampus.common.course;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 教学班展示信息。
 */
public final class OfferingInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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

    private final boolean selected;
    private final String availabilityStatus;

    public OfferingInfo(
        long offeringId,
        String classNo,
        List<String> teacherNames,
        List<ScheduleInfo> schedules,
        String locationName,
        String campusName,
        String teachingLanguage,
        int selectedCount,
        int capacity,
        int remainingCount,
        boolean selected,
        String availabilityStatus) {

        this.offeringId = offeringId;
        this.classNo = Objects.requireNonNull(classNo);
        this.teacherNames = List.copyOf(teacherNames);
        this.schedules = List.copyOf(schedules);
        this.locationName = locationName;
        this.campusName = campusName;
        this.teachingLanguage =
            Objects.requireNonNull(teachingLanguage);
        this.selectedCount = selectedCount;
        this.capacity = capacity;
        this.remainingCount = remainingCount;
        this.selected = selected;
        this.availabilityStatus =
            Objects.requireNonNull(availabilityStatus);
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

    public boolean isSelected() {
        return selected;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }
}
