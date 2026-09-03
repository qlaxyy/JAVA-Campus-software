package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;

import java.time.LocalDateTime;
import java.util.Objects;

/** Internal doctor-onboarding request owned by the hospital module. */
record DoctorApplication(
        String requestId,
        DoctorApplicationType applicationType,
        String username,
        String displayName,
        String departmentId,
        String doctorTitle,
        String requestedByUserId,
        DoctorApplicationStatus status,
        String targetUserId,
        String reviewedByUserId,
        LocalDateTime createdAt) {

    DoctorApplication {
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(applicationType);
        Objects.requireNonNull(displayName);
        Objects.requireNonNull(departmentId);
        Objects.requireNonNull(doctorTitle);
        Objects.requireNonNull(requestedByUserId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
    }

    DoctorApplication reviewed(
            DoctorApplicationStatus newStatus,
            String resolvedUsername,
            String userId,
            String reviewerUserId) {
        return new DoctorApplication(
                requestId, applicationType, resolvedUsername,
                displayName, departmentId, doctorTitle,
                requestedByUserId, newStatus, userId, reviewerUserId, createdAt);
    }
}
