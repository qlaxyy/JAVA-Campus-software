package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** Internal doctor row with an optional binding to a shared user account. */
record HospitalDoctor(
        String doctorId,
        String userId,
        String departmentId,
        String doctorName,
        String doctorTitle,
        boolean active) {

    HospitalDoctor {
        doctorId = requireText(doctorId, "doctorId");
        if (userId != null && userId.isBlank()) {
            throw new IllegalArgumentException("userId must be null or non-blank");
        }
        userId = userId == null ? null : userId.trim();
        departmentId = requireText(departmentId, "departmentId");
        doctorName = requireText(doctorName, "doctorName");
        doctorTitle = requireText(doctorTitle, "doctorTitle");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
