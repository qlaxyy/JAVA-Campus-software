package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/** Doctor-authored diagnosis and disposition for one assigned appointment. */
public final class SubmitConsultationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;
    private final String diagnosisOpinion;
    private final String examinationAdvice;
    private final String treatmentAdvice;
    private final String medicationAdvice;
    private final String followUpAdvice;

    public SubmitConsultationRequest(
            String appointmentId,
            String diagnosisOpinion,
            String examinationAdvice,
            String treatmentAdvice,
            String medicationAdvice,
            String followUpAdvice) {
        this.appointmentId = requireText(appointmentId, "appointmentId");
        this.diagnosisOpinion = normalize(diagnosisOpinion);
        this.examinationAdvice = normalize(examinationAdvice);
        this.treatmentAdvice = normalize(treatmentAdvice);
        this.medicationAdvice = normalize(medicationAdvice);
        this.followUpAdvice = normalize(followUpAdvice);
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public String getDiagnosisOpinion() {
        return diagnosisOpinion;
    }

    public String getExaminationAdvice() {
        return examinationAdvice;
    }

    public String getTreatmentAdvice() {
        return treatmentAdvice;
    }

    public String getMedicationAdvice() {
        return medicationAdvice;
    }

    public String getFollowUpAdvice() {
        return followUpAdvice;
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
