package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Read-only schedule information displayed by the hospital client. */
public final class SlotView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String scheduleId;
    private final String departmentId;
    private final String departmentName;
    private final String doctorId;
    private final String doctorName;
    private final String doctorTitle;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int priceCents;
    private final int capacity;
    private final int remaining;
    private final SlotAvailability availability;

    public SlotView(
            String scheduleId,
            String departmentId,
            String departmentName,
            String doctorId,
            String doctorName,
            String doctorTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int priceCents,
            int capacity,
            int remaining,
            SlotAvailability availability) {
        this.scheduleId = requireText(scheduleId, "scheduleId");
        this.departmentId = requireText(departmentId, "departmentId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.doctorId = requireText(doctorId, "doctorId");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        this.availability = Objects.requireNonNull(
                availability, "availability must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (priceCents < 0 || capacity < 0 || remaining < 0 || remaining > capacity) {
            throw new IllegalArgumentException("slot number fields are invalid");
        }
        this.priceCents = priceCents;
        this.capacity = capacity;
        this.remaining = remaining;
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

    public String getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getDoctorTitle() {
        return doctorTitle;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getRemaining() {
        return remaining;
    }

    public SlotAvailability getAvailability() {
        return availability;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
