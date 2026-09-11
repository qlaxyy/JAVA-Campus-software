package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** One draft schedule entered by a hospital administrator. */
public final class CreateScheduleRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String doctorId;
    private final String departmentId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int registrationFeeCents;
    private final int capacity;

    public CreateScheduleRequest(
            String doctorId,
            String departmentId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int registrationFeeCents,
            int capacity) {
        this.doctorId = requireText(doctorId, "doctorId");
        this.departmentId = requireText(departmentId, "departmentId");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (registrationFeeCents < 0 || registrationFeeCents > 100_000) {
            throw new IllegalArgumentException("registrationFeeCents is out of range");
        }
        if (capacity <= 0 || capacity > 200) {
            throw new IllegalArgumentException("capacity is out of range");
        }
        this.registrationFeeCents = registrationFeeCents;
        this.capacity = capacity;
    }

    public String getDoctorId() { return doctorId; }

    public String getDepartmentId() { return departmentId; }

    public LocalDateTime getStartTime() { return startTime; }

    public LocalDateTime getEndTime() { return endTime; }

    public int getRegistrationFeeCents() { return registrationFeeCents; }

    public int getCapacity() { return capacity; }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
