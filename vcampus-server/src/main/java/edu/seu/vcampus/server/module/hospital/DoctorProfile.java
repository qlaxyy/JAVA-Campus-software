package edu.seu.vcampus.server.module.hospital;

import java.util.Objects;

/** Hospital-owned professional data; its existence grants doctor capability. */
record DoctorProfile(
        String userId,
        String departmentId,
        String doctorName,
        String doctorTitle,
        boolean active) {
    DoctorProfile {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(departmentId);
        Objects.requireNonNull(doctorName);
        Objects.requireNonNull(doctorTitle);
    }
}
