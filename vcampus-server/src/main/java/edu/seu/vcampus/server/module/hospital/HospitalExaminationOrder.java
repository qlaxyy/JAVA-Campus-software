package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.ExaminationStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/** One simplified examination order opened during an appointment. */
record HospitalExaminationOrder(
        String orderId,
        String episodeId,
        String orderedAppointmentId,
        String doctorId,
        String patientUserId,
        String itemName,
        String instructions,
        ExaminationStatus status,
        LocalDateTime orderedAt,
        LocalDateTime reviewedAt) {

    HospitalExaminationOrder {
        orderId = requireText(orderId, "orderId");
        episodeId = requireText(episodeId, "episodeId");
        orderedAppointmentId = requireText(orderedAppointmentId, "orderedAppointmentId");
        doctorId = requireText(doctorId, "doctorId");
        patientUserId = requireText(patientUserId, "patientUserId");
        itemName = requireText(itemName, "itemName");
        instructions = instructions == null ? "" : instructions.trim();
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(orderedAt, "orderedAt must not be null");
        if (status == ExaminationStatus.REVIEWED && reviewedAt == null) {
            throw new IllegalArgumentException("reviewed order requires reviewedAt");
        }
        if (status != ExaminationStatus.REVIEWED && reviewedAt != null) {
            throw new IllegalArgumentException("reviewedAt is only allowed when reviewed");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
