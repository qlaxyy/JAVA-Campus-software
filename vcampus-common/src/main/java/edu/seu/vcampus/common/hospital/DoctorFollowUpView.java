package edu.seu.vcampus.common.hospital;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** One examination-led clinical episode that still needs follow-up. */
public final class DoctorFollowUpView implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String examinationOrderId;
    private final String episodeId;
    private final String patientUserId;
    private final String departmentName;
    private final String examinationItem;
    private final ExaminationStatus examinationStatus;
    private final LocalDateTime orderedAt;
    private final boolean resultReviewBooked;
    private final String reviewDoctorName;
    private final LocalDateTime reviewStartTime;
    private final List<DoctorClinicalRecordView> episodeRecords;
    private final List<ExaminationOrderView> episodeExaminations;

    public DoctorFollowUpView(
            String examinationOrderId,
            String episodeId,
            String patientUserId,
            String departmentName,
            String examinationItem,
            ExaminationStatus examinationStatus,
            LocalDateTime orderedAt,
            boolean resultReviewBooked,
            String reviewDoctorName,
            LocalDateTime reviewStartTime) {
        this(examinationOrderId, episodeId, patientUserId, departmentName,
                examinationItem, examinationStatus, orderedAt, resultReviewBooked,
                reviewDoctorName, reviewStartTime, List.of(), List.of());
    }

    public DoctorFollowUpView(
            String examinationOrderId,
            String episodeId,
            String patientUserId,
            String departmentName,
            String examinationItem,
            ExaminationStatus examinationStatus,
            LocalDateTime orderedAt,
            boolean resultReviewBooked,
            String reviewDoctorName,
            LocalDateTime reviewStartTime,
            List<DoctorClinicalRecordView> episodeRecords,
            List<ExaminationOrderView> episodeExaminations) {
        this.examinationOrderId = requireText(examinationOrderId, "examinationOrderId");
        this.episodeId = requireText(episodeId, "episodeId");
        this.patientUserId = requireText(patientUserId, "patientUserId");
        this.departmentName = requireText(departmentName, "departmentName");
        this.examinationItem = requireText(examinationItem, "examinationItem");
        this.examinationStatus = Objects.requireNonNull(
                examinationStatus, "examinationStatus must not be null");
        this.orderedAt = Objects.requireNonNull(orderedAt, "orderedAt must not be null");
        this.resultReviewBooked = resultReviewBooked;
        this.reviewDoctorName = normalizeNullable(reviewDoctorName);
        this.reviewStartTime = reviewStartTime;
        if (resultReviewBooked && (this.reviewDoctorName == null || reviewStartTime == null)) {
            throw new IllegalArgumentException(
                    "a booked result review requires doctor and start time");
        }
        this.episodeRecords = List.copyOf(Objects.requireNonNull(
                episodeRecords, "episodeRecords must not be null"));
        this.episodeExaminations = List.copyOf(Objects.requireNonNull(
                episodeExaminations, "episodeExaminations must not be null"));
    }

    public String getExaminationOrderId() { return examinationOrderId; }

    public String getEpisodeId() { return episodeId; }

    public String getPatientUserId() { return patientUserId; }

    public String getDepartmentName() { return departmentName; }

    public String getExaminationItem() { return examinationItem; }

    public ExaminationStatus getExaminationStatus() { return examinationStatus; }

    public LocalDateTime getOrderedAt() { return orderedAt; }

    public boolean isResultReviewBooked() { return resultReviewBooked; }

    public String getReviewDoctorName() { return reviewDoctorName; }

    public LocalDateTime getReviewStartTime() { return reviewStartTime; }

    public List<DoctorClinicalRecordView> getEpisodeRecords() {
        return episodeRecords == null ? List.of() : episodeRecords;
    }

    public List<ExaminationOrderView> getEpisodeExaminations() {
        return episodeExaminations == null ? List.of() : episodeExaminations;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
