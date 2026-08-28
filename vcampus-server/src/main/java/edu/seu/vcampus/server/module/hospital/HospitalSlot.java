package edu.seu.vcampus.server.module.hospital;

import java.time.LocalDateTime;
import java.util.Objects;

/** Internal schedule row used by the first in-memory implementation. */
record HospitalSlot(
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
        int bookedCount,
        boolean published) {

    HospitalSlot {
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        Objects.requireNonNull(departmentId, "departmentId must not be null");
        Objects.requireNonNull(departmentName, "departmentName must not be null");
        Objects.requireNonNull(doctorId, "doctorId must not be null");
        Objects.requireNonNull(doctorName, "doctorName must not be null");
        Objects.requireNonNull(doctorTitle, "doctorTitle must not be null");
        Objects.requireNonNull(startTime, "startTime must not be null");
        Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        if (priceCents < 0 || capacity < 0 || bookedCount < 0 || bookedCount > capacity) {
            throw new IllegalArgumentException("slot number fields are invalid");
        }
    }
}
