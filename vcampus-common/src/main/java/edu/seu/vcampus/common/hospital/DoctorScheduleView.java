package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** One future schedule and its pending appointments for the authenticated doctor. */
public final class DoctorScheduleView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String scheduleId;
    private final String departmentId;
    private final String departmentName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int capacity;
    private final int remaining;
    private final boolean published;
    private final List<DoctorAppointmentView> pendingAppointments;

    public DoctorScheduleView(
            String scheduleId,
            String departmentId,
            String departmentName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int capacity,
            int remaining,
            boolean published,
            List<DoctorAppointmentView> pendingAppointments) {
        this.scheduleId = requireText(scheduleId, "scheduleId");
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (capacity < 0 || remaining < 0 || remaining > capacity) {
            throw new IllegalArgumentException("schedule capacity fields are invalid");
        }
        this.capacity = capacity;
        this.remaining = remaining;
        this.published = published;
        this.pendingAppointments = List.copyOf(Objects.requireNonNull(
                pendingAppointments, "pendingAppointments must not be null"));
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRemaining() {
        return remaining;
    }

    public boolean isPublished() {
        return published;
    }

    public List<DoctorAppointmentView> getPendingAppointments() {
        return pendingAppointments;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
