package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/** Patient-owned aggregate used by the navigable health-record pages. */
public final class PatientHealthRecordView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final PatientHealthProfileView healthProfile;
    private final List<ConsultationRecordView> consultations;
    private final List<ExaminationOrderView> examinations;

    public PatientHealthRecordView(
            PatientHealthProfileView healthProfile,
            List<ConsultationRecordView> consultations,
            List<ExaminationOrderView> examinations) {
        this.healthProfile = Objects.requireNonNull(
                healthProfile, "healthProfile must not be null");
        this.consultations = List.copyOf(Objects.requireNonNull(
                consultations, "consultations must not be null"));
        this.examinations = List.copyOf(Objects.requireNonNull(
                examinations, "examinations must not be null"));
    }

    public PatientHealthProfileView getHealthProfile() {
        return healthProfile;
    }

    public List<ConsultationRecordView> getConsultations() {
        return consultations;
    }

    public List<ExaminationOrderView> getExaminations() {
        return examinations;
    }
}
