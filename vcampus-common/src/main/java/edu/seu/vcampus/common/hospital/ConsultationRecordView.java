package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable doctor-authored consultation record. */
public final class ConsultationRecordView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String consultationId;
    private final String appointmentId;
    private final String doctorId;
    private final String doctorName;
    private final String doctorTitle;
    private final String patientUserId;
    private final String departmentName;
    private final VisitType visitType;
    private final ConsultationOutcome outcome;
    private final String diagnosisOpinion;
    private final String examinationAdvice;
    private final String treatmentAdvice;
    private final String medicationAdvice;
    private final String followUpAdvice;
    private final LocalDateTime createdAt;

    public ConsultationRecordView(
            String consultationId,
            String appointmentId,
            String doctorId,
            String doctorName,
            String doctorTitle,
            String patientUserId,
            String departmentName,
            VisitType visitType,
            ConsultationOutcome outcome,
            String diagnosisOpinion,
            String examinationAdvice,
            String treatmentAdvice,
            String medicationAdvice,
            String followUpAdvice,
            LocalDateTime createdAt) {
        this.consultationId = requireText(consultationId, "consultationId");
        this.appointmentId = requireText(appointmentId, "appointmentId");
        this.doctorId = requireText(doctorId, "doctorId");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.patientUserId = requireText(patientUserId, "patientUserId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.visitType = Objects.requireNonNull(visitType, "visitType must not be null");
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.diagnosisOpinion = requireText(diagnosisOpinion, "diagnosisOpinion");
        this.examinationAdvice = displayText(examinationAdvice);
        this.treatmentAdvice = displayText(treatmentAdvice);
        this.medicationAdvice = displayText(medicationAdvice);
        this.followUpAdvice = displayText(followUpAdvice);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getConsultationId() { return consultationId; }

    public String getAppointmentId() { return appointmentId; }

    public String getDoctorId() { return doctorId; }

    public String getDoctorName() { return doctorName; }

    public String getDoctorTitle() { return doctorTitle; }

    public String getPatientUserId() { return patientUserId; }

    public String getDepartmentName() { return departmentName; }

    public VisitType getVisitType() { return visitType; }

    public ConsultationOutcome getOutcome() { return outcome; }

    public String getDiagnosisOpinion() { return diagnosisOpinion; }

    public String getExaminationAdvice() { return examinationAdvice; }

    public String getTreatmentAdvice() { return treatmentAdvice; }

    public String getMedicationAdvice() { return medicationAdvice; }

    public String getFollowUpAdvice() { return followUpAdvice; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    private static String displayText(String value) {
        return value == null || value.isBlank() ? "无" : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
