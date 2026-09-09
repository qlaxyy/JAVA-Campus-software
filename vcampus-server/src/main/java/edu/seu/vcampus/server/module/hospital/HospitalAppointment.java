package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.AppointmentStatus;
import edu.seu.vcampus.common.hospital.VisitType;

import java.time.LocalDateTime;
import java.util.Objects;

/** Internal appointment row. Patient identity always comes from the server session. */
record HospitalAppointment(
        String appointmentId,
        String patientUserId,
        String scheduleId,
        int queueNumber,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        LocalDateTime completedAt,
        AppointmentStatus status,
        VisitType visitType,
        String episodeId,
        String sourceFirstVisitAppointmentId) {

    HospitalAppointment {
        Objects.requireNonNull(appointmentId, "appointmentId must not be null");
        Objects.requireNonNull(patientUserId, "patientUserId must not be null");
        Objects.requireNonNull(scheduleId, "scheduleId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(visitType, "visitType must not be null");
        Objects.requireNonNull(episodeId, "episodeId must not be null");
        if (episodeId.isBlank()) {
            throw new IllegalArgumentException("episodeId must not be blank");
        }
        if (visitType == VisitType.FIRST_VISIT
                && sourceFirstVisitAppointmentId != null) {
            throw new IllegalArgumentException(
                    "a first visit cannot have a source appointment");
        }
        if (visitType != VisitType.FIRST_VISIT
                && (sourceFirstVisitAppointmentId == null
                || sourceFirstVisitAppointmentId.isBlank())) {
            throw new IllegalArgumentException(
                    "a follow-up or result review requires a source appointment");
        }
        if (queueNumber <= 0) {
            throw new IllegalArgumentException("queueNumber must be positive");
        }
        if (status == AppointmentStatus.CANCELLED && cancelledAt == null) {
            throw new IllegalArgumentException(
                    "cancelledAt is required for a cancelled appointment");
        }
        if (status != AppointmentStatus.CANCELLED && cancelledAt != null) {
            throw new IllegalArgumentException(
                    "cancelledAt is only allowed for a cancelled appointment");
        }
        if (cancelledAt != null && cancelledAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("cancelledAt must not be before createdAt");
        }
        if (status == AppointmentStatus.COMPLETED && completedAt == null) {
            throw new IllegalArgumentException(
                    "completedAt is required for a completed appointment");
        }
        if (status != AppointmentStatus.COMPLETED && completedAt != null) {
            throw new IllegalArgumentException(
                    "completedAt is only allowed for a completed appointment");
        }
        if (completedAt != null && completedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("completedAt must not be before createdAt");
        }
    }

    boolean occupiesSlot() {
        return status != AppointmentStatus.CANCELLED;
    }
}
