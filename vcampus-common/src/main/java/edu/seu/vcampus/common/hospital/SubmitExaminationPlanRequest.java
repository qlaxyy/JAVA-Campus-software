package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;

/** Doctor request that signs a preliminary stage note and opens an examination. */
public final class SubmitExaminationPlanRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String appointmentId;
    private final String preliminaryDiagnosis;
    private final String reportInterpretation;
    private final String examinationItem;
    private final String examinationInstructions;
    private final String interimCareAdvice;

    public SubmitExaminationPlanRequest(
            String appointmentId,
            String preliminaryDiagnosis,
            String examinationItem,
            String examinationInstructions,
            String interimCareAdvice) {
        this(appointmentId, preliminaryDiagnosis, "", examinationItem,
                examinationInstructions, interimCareAdvice);
    }

    public SubmitExaminationPlanRequest(
            String appointmentId,
            String preliminaryDiagnosis,
            String reportInterpretation,
            String examinationItem,
            String examinationInstructions,
            String interimCareAdvice) {
        this.appointmentId = requireText(appointmentId, "appointmentId");
        this.preliminaryDiagnosis = requireText(
                preliminaryDiagnosis, "preliminaryDiagnosis");
        this.reportInterpretation = normalize(reportInterpretation);
        this.examinationItem = requireText(examinationItem, "examinationItem");
        this.examinationInstructions = normalize(examinationInstructions);
        this.interimCareAdvice = normalize(interimCareAdvice);
    }

    public String getAppointmentId() { return appointmentId; }

    public String getPreliminaryDiagnosis() { return preliminaryDiagnosis; }

    public String getReportInterpretation() {
        return reportInterpretation == null ? "" : reportInterpretation;
    }

    public String getExaminationItem() { return examinationItem; }

    public String getExaminationInstructions() { return examinationInstructions; }

    public String getInterimCareAdvice() { return interimCareAdvice; }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
