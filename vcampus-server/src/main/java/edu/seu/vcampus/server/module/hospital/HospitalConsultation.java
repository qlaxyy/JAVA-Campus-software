package edu.seu.vcampus.server.module.hospital;

import edu.seu.vcampus.common.hospital.ConsultationOutcome;

import java.time.LocalDateTime;
import java.util.Objects;

/** Signed final consultation or preliminary examination-stage note. */
record HospitalConsultation(
        String consultationId,
        String appointmentId,
        String doctorId,
        String patientUserId,
        ConsultationOutcome outcome,
        String diagnosisOpinion,
        String examinationAdvice,
        String treatmentAdvice,
        String medicationAdvice,
        String followUpAdvice,
        LocalDateTime createdAt) {

    HospitalConsultation {
        consultationId = requireText(consultationId, "consultationId");
        appointmentId = requireText(appointmentId, "appointmentId");
        doctorId = requireText(doctorId, "doctorId");
        patientUserId = requireText(patientUserId, "patientUserId");
        Objects.requireNonNull(outcome, "outcome must not be null");
        diagnosisOpinion = requireText(diagnosisOpinion, "diagnosisOpinion");
        examinationAdvice = normalize(examinationAdvice);
        treatmentAdvice = outcome == ConsultationOutcome.COMPLETED
                ? requireText(treatmentAdvice, "treatmentAdvice")
                : normalize(treatmentAdvice);
        medicationAdvice = normalize(medicationAdvice);
        followUpAdvice = normalize(followUpAdvice);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
