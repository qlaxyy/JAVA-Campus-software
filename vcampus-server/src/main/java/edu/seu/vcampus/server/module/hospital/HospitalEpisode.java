package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.EpisodeStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** One continuous diagnostic and treatment process for a patient. */
record HospitalEpisode(
        String episodeId,
        String patientUserId,
        String departmentId,
        EpisodeStatus status,
        LocalDateTime openedAt,
        LocalDateTime completedAt) {

    HospitalEpisode {
        episodeId = requireText(episodeId, "episodeId");
        patientUserId = requireText(patientUserId, "patientUserId");
        departmentId = requireText(departmentId, "departmentId");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(openedAt, "openedAt must not be null");
        if (status == EpisodeStatus.COMPLETED && completedAt == null) {
            throw new IllegalArgumentException("completed episode requires completedAt");
        }
        if (status != EpisodeStatus.COMPLETED && completedAt != null) {
            throw new IllegalArgumentException("completedAt is only allowed when completed");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
