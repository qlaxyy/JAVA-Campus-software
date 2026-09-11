package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Server-authorized background information used before a doctor submits a record. */
public final class DoctorConsultationContextView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final DoctorAppointmentView appointment;
    private final String departmentName;
    private final String doctorName;
    private final String doctorTitle;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final PatientHealthProfileView healthProfile;
    private final List<ConsultationRecordView> previousConsultations;
    private final List<ExaminationOrderView> episodeExaminations;
    private final List<DoctorClinicalRecordView> previousClinicalRecords;

    public DoctorConsultationContextView(
            DoctorAppointmentView appointment,
            String departmentName,
            String doctorName,
            String doctorTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            PatientHealthProfileView healthProfile,
            List<ConsultationRecordView> previousConsultations,
            List<ExaminationOrderView> episodeExaminations) {
        this(appointment, departmentName, doctorName, doctorTitle, startTime, endTime,
                healthProfile, previousConsultations, episodeExaminations, List.of());
    }

    public DoctorConsultationContextView(
            DoctorAppointmentView appointment,
            String departmentName,
            String doctorName,
            String doctorTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            PatientHealthProfileView healthProfile,
            List<ConsultationRecordView> previousConsultations,
            List<ExaminationOrderView> episodeExaminations,
            List<DoctorClinicalRecordView> previousClinicalRecords) {
        this.appointment = Objects.requireNonNull(
                appointment, "appointment must not be null");
        this.departmentName = requireText(departmentName, "departmentName");
        this.doctorName = requireText(doctorName, "doctorName");
        this.doctorTitle = requireText(doctorTitle, "doctorTitle");
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        this.healthProfile = Objects.requireNonNull(
                healthProfile, "healthProfile must not be null");
        this.previousConsultations = List.copyOf(Objects.requireNonNull(
                previousConsultations, "previousConsultations must not be null"));
        this.episodeExaminations = List.copyOf(Objects.requireNonNull(
                episodeExaminations, "episodeExaminations must not be null"));
        this.previousClinicalRecords = List.copyOf(Objects.requireNonNull(
                previousClinicalRecords, "previousClinicalRecords must not be null"));
    }

    public DoctorAppointmentView getAppointment() {
        return appointment;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getDoctorTitle() {
        return doctorTitle;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public PatientHealthProfileView getHealthProfile() {
        return healthProfile;
    }

    public List<ConsultationRecordView> getPreviousConsultations() {
        return previousConsultations;
    }

    public List<ExaminationOrderView> getEpisodeExaminations() {
        return episodeExaminations;
    }

    public List<DoctorClinicalRecordView> getPreviousClinicalRecords() {
        return previousClinicalRecords == null ? List.of() : previousClinicalRecords;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
